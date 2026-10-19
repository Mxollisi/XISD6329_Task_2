package za.co.giantautoupholstery.data

import androidx.room.withTransaction
import za.co.giantautoupholstery.BuildConfig
import za.co.giantautoupholstery.core.DomainException
import za.co.giantautoupholstery.core.safeCall
import za.co.giantautoupholstery.core.today
import za.co.giantautoupholstery.data.local.*
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEVELOPMENT/TEST ONLY demonstration data for the "at least 10 records per table" assignment requirement.
 *  - Runs only when BuildConfig.DEBUG, through an explicit button; never on startup, never in release builds.
 *  - Transactional and idempotent: deterministic ids with INSERT IGNORE (never overwrites real rows).
 *  - Simulated profiles use uid "SIMULATED-PROFILE-NN". They are NOT Firebase accounts and cannot sign in.
 *  - Some bookings/notifications/contacts are attached to the signed-in tester so private lists can be demonstrated.
 */
@Singleton
class DevFixtures @Inject constructor(
    private val db: GiantDatabase,
    private val seeder: CatalogSeeder,
    private val clock: Clock,
) {
    companion object { const val MIN_RECORDS = 10 }

    private fun simUid(n: Int) = "SIMULATED-PROFILE-" + n.toString().padStart(2, '0')

    suspend fun setup(currentUid: String?): Result<Map<String, Int>> = safeCall {
        if (!BuildConfig.DEBUG) throw DomainException("Fixtures are only available in debug builds.")
        seeder.seed()
        val now = clock.millis()
        val today = clock.today()
        val services = seeder.services
        db.withTransaction {
            val dao = db.fixtureDao()
            val realOk = currentUid != null && db.profileDao().get(currentUid) != null
            dao.profiles((1..10).map {
                ProfileEntity(simUid(it), "Simulated Customer $it (fictional)", "+27 82 000 00${it.toString().padStart(2, '0')}",
                    "$it Fictional Street, Johannesburg", true, it % 2 == 0, listOf("system", "light", "dark")[it % 3], now, now)
            })
            fun owner(i: Int) = if (realOk && i <= 6) currentUid!! else simUid(((i - 1) % 10) + 1)
            val offsets = listOf(3, 5, 8, 12, -2, -10, 15, 20, -20, -30)
            val statuses = listOf("Pending", "Confirmed", "Pending", "Pending", "Completed", "Cancelled", "Pending", "Confirmed", "Completed", "Cancelled")
            dao.bookings((1..10).map { i ->
                val n = i.toString().padStart(3, '0')
                BookingEntity("fixture-booking-$n", owner(i), "GAU-FIX$n", services[(i - 1) % services.size].id,
                    today.plusDays(offsets[i - 1].toLong()).toString(), "%02d:00".format(java.util.Locale.ROOT, 8 + (i % 9)),
                    "$i Fictional Street, Johannesburg", "Simulated fixture booking $i (fictional)", statuses[i - 1], "fixture-submission-$n", now - i * 3_600_000L)
            })
            dao.notifications((1..10).map { i ->
                NotificationEntity("fixture-notification-${i.toString().padStart(3, '0')}", if (realOk && i <= 5) currentUid!! else simUid(i),
                    "Simulated notification $i", "Fictional demonstration notification $i.", i % 3 == 0, now - i * 1_800_000L)
            })
            dao.contacts((1..10).map { i ->
                ContactMessageEntity("fixture-contact-${i.toString().padStart(3, '0')}", if (realOk && i <= 3) currentUid else if (i <= 7) simUid(i) else null,
                    "Fictional Sender $i", "sender$i@example.invalid", "Simulated enquiry $i", "This is fictional demonstration message number $i for the database evidence.", now - i * 900_000L)
            })
        }
        countReport()
    }

    suspend fun countReport(): Map<String, Int> = db.fixtureDao().let {
        linkedMapOf(
            "profiles" to it.countProfiles(), "services" to it.countServices(), "bookings" to it.countBookings(),
            "notifications" to it.countNotifications(), "faqs" to it.countFaqs(), "contact_messages" to it.countContacts(),
        )
    }

    /** Tables below the 10-record requirement (empty list = requirement met). */
    fun shortfalls(report: Map<String, Int>): List<String> = report.filterValues { it < MIN_RECORDS }.keys.toList()

    /** Removes only fixture rows (id prefix "fixture-" / uid prefix "SIMULATED-"); real rows are untouched. */
    suspend fun cleanup(): Result<Map<String, Int>> = safeCall {
        if (!BuildConfig.DEBUG) throw DomainException("Fixtures are only available in debug builds.")
        db.withTransaction {
            val dao = db.fixtureDao()
            dao.deleteBookings(); dao.deleteNotifications(); dao.deleteContacts(); dao.deleteProfiles()
        }
        countReport()
    }
}
