package za.co.giantautoupholstery.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class BookingWithService(
    @Embedded val booking: BookingEntity,
    @Relation(parentColumn = "serviceId", entityColumn = "id") val service: ServiceEntity,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE uid = :uid") fun observe(uid: String): Flow<ProfileEntity?>
    @Query("SELECT * FROM profiles WHERE uid = :uid") suspend fun get(uid: String): ProfileEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(profile: ProfileEntity): Long
    @Query("UPDATE profiles SET fullName = :name, phone = :phone, address = :address, updatedAt = :now WHERE uid = :uid")
    suspend fun updateDetails(uid: String, name: String, phone: String?, address: String?, now: Long): Int
    @Query("UPDATE profiles SET emailNotifications = :email, pushNotifications = :push, themeMode = :theme, updatedAt = :now WHERE uid = :uid")
    suspend fun updatePreferences(uid: String, email: Boolean, push: Boolean, theme: String, now: Long): Int
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY priceCents ASC") fun observeAll(): Flow<List<ServiceEntity>>
    @Query("SELECT * FROM services WHERE id = :id") fun observe(id: String): Flow<ServiceEntity?>
    @Query("SELECT * FROM services WHERE id = :id") suspend fun get(id: String): ServiceEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAllIgnore(list: List<ServiceEntity>)
}

@Dao
interface BookingDao {
    // Every private query is scoped to the verified owner UID.
    @Transaction @Query("SELECT * FROM bookings WHERE ownerUid = :uid ORDER BY preferredDate DESC, preferredTime DESC")
    fun observeForOwner(uid: String): Flow<List<BookingWithService>>

    @Transaction @Query("SELECT * FROM bookings WHERE ownerUid = :uid AND id = :id")
    fun observeOne(uid: String, id: String): Flow<BookingWithService?>

    @Transaction @Query("SELECT * FROM bookings WHERE ownerUid = :uid AND id = :id")
    suspend fun getOwned(uid: String, id: String): BookingWithService?

    @Transaction @Query("SELECT * FROM bookings WHERE ownerUid = :uid AND submissionId = :submissionId")
    suspend fun findBySubmission(uid: String, submissionId: String): BookingWithService?

    @Query("SELECT COUNT(*) FROM bookings WHERE reference = :reference") suspend fun referenceCount(reference: String): Int

    /** ABORT on conflict: a duplicate reference/submission id throws and rolls the transaction back. */
    @Insert suspend fun insert(booking: BookingEntity)

    /** Atomic owner + status check: only the owner's Pending booking can be cancelled. Returns rows changed (0 or 1). */
    @Query("UPDATE bookings SET status = 'Cancelled' WHERE id = :id AND ownerUid = :uid AND status = 'Pending'")
    suspend fun cancelPending(uid: String, id: String): Int
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE ownerUid = :uid ORDER BY createdAt DESC") fun observeForOwner(uid: String): Flow<List<NotificationEntity>>
    @Query("SELECT COUNT(*) FROM notifications WHERE ownerUid = :uid AND isRead = 0") fun observeUnreadCount(uid: String): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(n: NotificationEntity)
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id AND ownerUid = :uid") suspend fun markRead(uid: String, id: String): Int
    @Query("UPDATE notifications SET isRead = 1 WHERE ownerUid = :uid AND isRead = 0") suspend fun markAllRead(uid: String): Int
    @Query("DELETE FROM notifications WHERE id = :id AND ownerUid = :uid") suspend fun delete(uid: String, id: String): Int
}

@Dao
interface FaqDao {
    @Query("SELECT * FROM faqs ORDER BY displayOrder ASC") fun observeAll(): Flow<List<FaqEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAllIgnore(list: List<FaqEntity>)
}

@Dao
interface ContactDao {
    @Insert suspend fun insert(m: ContactMessageEntity)
}

/** Development/test-only operations: fixtures, scoped cleanup and counts. Used only behind BuildConfig.DEBUG or in tests. */
@Dao
interface FixtureDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun profiles(list: List<ProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun bookings(list: List<BookingEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun notifications(list: List<NotificationEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun contacts(list: List<ContactMessageEntity>)

    @Query("DELETE FROM bookings WHERE id LIKE 'fixture-%'") suspend fun deleteBookings(): Int
    @Query("DELETE FROM notifications WHERE id LIKE 'fixture-%'") suspend fun deleteNotifications(): Int
    @Query("DELETE FROM contact_messages WHERE id LIKE 'fixture-%'") suspend fun deleteContacts(): Int
    @Query("DELETE FROM profiles WHERE uid LIKE 'SIMULATED-%'") suspend fun deleteProfiles(): Int

    @Query("SELECT COUNT(*) FROM profiles") suspend fun countProfiles(): Int
    @Query("SELECT COUNT(*) FROM services") suspend fun countServices(): Int
    @Query("SELECT COUNT(*) FROM bookings") suspend fun countBookings(): Int
    @Query("SELECT COUNT(*) FROM notifications") suspend fun countNotifications(): Int
    @Query("SELECT COUNT(*) FROM faqs") suspend fun countFaqs(): Int
    @Query("SELECT COUNT(*) FROM contact_messages") suspend fun countContacts(): Int
}
