package za.co.giantautoupholstery

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import za.co.giantautoupholstery.core.ValidationException
import za.co.giantautoupholstery.data.CatalogSeeder
import za.co.giantautoupholstery.data.DevFixtures
import za.co.giantautoupholstery.data.local.*
import za.co.giantautoupholstery.data.repo.BookingRepositoryImpl
import za.co.giantautoupholstery.data.repo.ProfileRepositoryImpl
import za.co.giantautoupholstery.domain.BookingInput
import za.co.giantautoupholstery.domain.BookingStatus
import za.co.giantautoupholstery.domain.LocalNotifier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Runs against isolated in-memory SQLite databases: no production data and no Firebase credentials needed. */
@RunWith(AndroidJUnit4::class)
class DatabaseAndRepositoryTest {
    private lateinit var db: GiantDatabase
    private lateinit var seeder: CatalogSeeder
    private lateinit var profiles: ProfileRepositoryImpl
    private lateinit var bookings: BookingRepositoryImpl
    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-18T08:00:00Z"), ZoneOffset.UTC)
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val silentNotifier = object : LocalNotifier { override fun notify(title: String, body: String) {} }

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ctx, GiantDatabase::class.java).allowMainThreadQueries().build()
        seeder = CatalogSeeder(db)
        profiles = ProfileRepositoryImpl(db, clock)
        bookings = BookingRepositoryImpl(db, clock, silentNotifier)
        runBlocking {
            seeder.seed()
            profiles.ensureProfile("uidA", "Alice Anderson").getOrThrow()
            profiles.ensureProfile("uidB", "Bob Brown").getOrThrow()
        }
    }

    @After fun tearDown() = db.close()

    private fun input(serviceId: String, date: LocalDate = LocalDate.of(2026, 10, 19)) =
        BookingInput(serviceId, date, "09:00", "12 Main Road, Johannesburg", "Torn seat", true)

    private fun firstServiceId() = seeder.services.first().id

    @Test fun welcomeNotification_createdOnce() = runBlocking {
        profiles.ensureProfile("uidA", "Alice Anderson").getOrThrow() // second call is a no-op
        val n = db.notificationDao().observeForOwner("uidA").first()
        assertEquals(1, n.count { it.title.startsWith("Welcome") })
    }

    @Test fun createBooking_persistsPending_withReference_and_notification() = runBlocking {
        val b = bookings.create("uidA", input(firstServiceId()), "sub-1").getOrThrow()
        assertEquals(BookingStatus.Pending, b.status)
        assertTrue(Regex("^GAU-[A-Z2-9]{6}$").matches(b.reference))
        val list = bookings.observeAll("uidA").first()
        assertEquals(1, list.size)
        assertEquals(b.reference, list[0].reference)
        assertEquals(2, db.notificationDao().observeForOwner("uidA").first().size) // welcome + booking saved
    }

    @Test fun createBooking_isIdempotentForSameSubmissionId() = runBlocking {
        val a = bookings.create("uidA", input(firstServiceId()), "same").getOrThrow()
        val b = bookings.create("uidA", input(firstServiceId()), "same").getOrThrow()
        assertEquals(a.id, b.id)
        assertEquals(1, bookings.observeAll("uidA").first().size)
        assertEquals(2, db.notificationDao().observeForOwner("uidA").first().size) // no duplicate notification
    }

    @Test fun createBooking_rejectsInvalidDate_and_writesNothing() = runBlocking {
        val r = bookings.create("uidA", input(firstServiceId(), LocalDate.of(2026, 10, 18)), "late")
        assertTrue(r.exceptionOrNull() is ValidationException)
        assertEquals(0, bookings.observeAll("uidA").first().size)
    }

    @Test fun accountIsolation_B_cannotSee_or_cancel_A() = runBlocking {
        val a = bookings.create("uidA", input(firstServiceId()), "iso").getOrThrow()
        assertTrue(bookings.observeAll("uidB").first().isEmpty())
        assertNull(bookings.observe("uidB", a.id).first())
        assertTrue(bookings.cancel("uidB", a.id).isFailure)
        assertEquals(BookingStatus.Pending, bookings.observe("uidA", a.id).first()!!.status)
    }

    @Test fun cancel_pendingOnly_updatesStatus_and_addsNotification_once() = runBlocking {
        val a = bookings.create("uidA", input(firstServiceId()), "c1").getOrThrow()
        assertTrue(bookings.cancel("uidA", a.id).isSuccess)
        assertEquals(BookingStatus.Cancelled, bookings.observe("uidA", a.id).first()!!.status)
        assertTrue(bookings.cancel("uidA", a.id).isFailure) // cannot cancel twice
        assertEquals(3, db.notificationDao().observeForOwner("uidA").first().size) // welcome + saved + cancelled
    }

    @Test fun customerCannotCancel_Confirmed_booking() = runBlocking {
        val a = bookings.create("uidA", input(firstServiceId()), "c2").getOrThrow()
        db.openHelper.writableDatabase.execSQL("UPDATE bookings SET status='Confirmed' WHERE id='${a.id}'") // simulate staff action
        assertTrue(bookings.cancel("uidA", a.id).isFailure)
        assertEquals(BookingStatus.Confirmed, bookings.observe("uidA", a.id).first()!!.status)
    }

    @Test fun foreignKey_violation_isRejected() {
        val bad = BookingEntity("x", "uidA", "GAU-BAD111", "no-such-service", "2026-10-19", "09:00", "addr12345", null, "Pending", "s", 0)
        assertThrows(SQLiteConstraintException::class.java) { runBlocking { db.bookingDao().insert(bad) } }
    }

    @Test fun uniqueReference_isEnforced() = runBlocking {
        val sid = firstServiceId()
        fun e(id: String, sub: String) = BookingEntity(id, "uidA", "GAU-SAME11", sid, "2026-10-19", "09:00", "addr12345", null, "Pending", sub, 0)
        db.bookingDao().insert(e("1", "s1"))
        assertThrows(SQLiteConstraintException::class.java) { runBlocking { db.bookingDao().insert(e("2", "s2")) } }
    }

    @Test fun notifications_markRead_markAll_delete_areOwnerScoped() = runBlocking {
        val dao = db.notificationDao()
        val own = dao.observeForOwner("uidA").first().first()
        assertEquals(0, dao.markRead("uidB", own.id))   // wrong owner: nothing changes
        assertEquals(1, dao.observeUnreadCount("uidA").first())
        assertEquals(1, dao.markRead("uidA", own.id))
        assertEquals(0, dao.observeUnreadCount("uidA").first())
        assertEquals(0, dao.delete("uidB", own.id))
        assertEquals(1, dao.delete("uidA", own.id))
    }

    @Test fun fixtures_giveAtLeastTenRowsInEverySixTables_idempotently_and_cleanUp() = runBlocking {
        val fixtures = DevFixtures(db, seeder, clock)
        val first = fixtures.setup("uidA").getOrThrow()
        assertTrue("shortfalls: ${fixtures.shortfalls(first)}", fixtures.shortfalls(first).isEmpty())
        listOf("profiles", "services", "bookings", "notifications", "faqs", "contact_messages").forEach {
            assertTrue("$it=${first[it]}", first.getValue(it) >= 10)
        }
        val second = fixtures.setup("uidA").getOrThrow() // idempotent
        assertEquals(first, second)
        assertEquals(6, bookings.observeAll("uidA").first().size) // private list demonstrable for the real tester
        assertTrue(bookings.observeAll("uidB").first().isEmpty())

        val after = fixtures.cleanup().getOrThrow()
        assertEquals(0, after["bookings"])
        assertEquals(2, after["profiles"]) // real profiles uidA/uidB untouched
    }

    @Test fun data_survives_closeAndReopen() {
        val name = "persist-test.db"
        ctx.deleteDatabase(name)
        var file = Room.databaseBuilder(ctx, GiantDatabase::class.java, name).allowMainThreadQueries().build()
        runBlocking { ProfileRepositoryImpl(file, clock).ensureProfile("persisted", "Persisted User").getOrThrow() }
        file.close()
        file = Room.databaseBuilder(ctx, GiantDatabase::class.java, name).allowMainThreadQueries().build()
        val p = runBlocking { file.profileDao().get("persisted") }
        assertEquals("Persisted User", p?.fullName)
        file.close()
        ctx.deleteDatabase(name)
    }
}
