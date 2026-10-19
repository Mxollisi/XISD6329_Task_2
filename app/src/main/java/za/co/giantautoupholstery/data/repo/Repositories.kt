package za.co.giantautoupholstery.data.repo

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import za.co.giantautoupholstery.core.*
import za.co.giantautoupholstery.data.*
import za.co.giantautoupholstery.data.local.*
import za.co.giantautoupholstery.domain.*
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(private val db: GiantDatabase, private val clock: Clock) : ProfileRepository {
    override fun observe(uid: String): Flow<Profile?> = db.profileDao().observe(uid).map { it?.toDomain() }

    override suspend fun ensureProfile(uid: String, fullName: String): Result<Unit> = safeCall {
        db.withTransaction {
            val now = clock.millis()
            val name = fullName.trim().ifBlank { "Customer" }.take(80)
            val inserted = db.profileDao().insertIgnore(ProfileEntity(uid, name, null, null, true, true, "system", now, now))
            if (inserted != -1L) { // first successful sign-in only => exactly one welcome notification
                db.notificationDao().insert(
                    NotificationEntity(UUID.randomUUID().toString(), uid, "Welcome to Giant Auto Upholstery",
                        "Your account is ready. Browse services and request your first booking.", false, now)
                )
            }
        }
    }

    override suspend fun updateDetails(uid: String, fullName: String, phone: String, address: String): Result<Unit> = safeCall {
        val errors = buildMap {
            Validation.fullName(fullName)?.let { put("fullName", it) }
            Validation.profilePhone(phone)?.let { put("phone", it) }
            Validation.profileAddress(address)?.let { put("address", it) }
        }
        if (errors.isNotEmpty()) throw ValidationException(errors)
        val n = db.profileDao().updateDetails(uid, fullName.trim(), phone.trim().ifBlank { null }, address.trim().ifBlank { null }, clock.millis())
        if (n != 1) throw DomainException("Profile not found. Please sign in again.")
    }

    override suspend fun updatePreferences(uid: String, email: Boolean, push: Boolean, themeMode: String): Result<Unit> = safeCall {
        require(themeMode in listOf("system", "light", "dark"))
        if (db.profileDao().updatePreferences(uid, email, push, themeMode, clock.millis()) != 1)
            throw DomainException("Profile not found. Please sign in again.")
    }
}

@Singleton
class ServiceRepositoryImpl @Inject constructor(private val db: GiantDatabase) : ServiceRepository {
    override fun observeAll() = db.serviceDao().observeAll().map { l -> l.map { it.toDomain() } }
    override fun observe(id: String) = db.serviceDao().observe(id).map { it?.toDomain() }
}

@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val db: GiantDatabase,
    private val clock: Clock,
    private val notifier: LocalNotifier,
) : BookingRepository {

    override fun observeAll(uid: String) = db.bookingDao().observeForOwner(uid).map { l -> l.map { it.toDomain() } }
    override fun observe(uid: String, id: String) = db.bookingDao().observeOne(uid, id).map { it?.toDomain() }

    override suspend fun create(uid: String, input: BookingInput, submissionId: String): Result<Booking> = safeCall {
        // Validation is enforced here as well as in the ViewModel: never rely on UI-only validation.
        val errors = Validation.booking(input, clock)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        val booking = db.withTransaction {
            val dao = db.bookingDao()
            dao.findBySubmission(uid, submissionId)?.let { return@withTransaction it.toDomain() } // idempotent retry
            val service = db.serviceDao().get(input.serviceId!!) ?: throw DomainException("That service is no longer available.")
            if (db.profileDao().get(uid) == null) throw DomainException("Your profile is not ready. Please sign in again.")

            var reference = generateReference()
            var attempts = 0
            while (dao.referenceCount(reference) > 0) {
                if (++attempts > 10) throw DomainException("Could not generate a booking reference. Please retry.")
                reference = generateReference()
            }
            val now = clock.millis()
            val entity = BookingEntity(
                id = UUID.randomUUID().toString(), ownerUid = uid, reference = reference, serviceId = service.id,
                preferredDate = input.date!!.toString(), preferredTime = input.time!!, address = input.address.trim(),
                description = input.description.trim().ifBlank { null }, status = BookingStatus.Pending.name,
                submissionId = submissionId, createdAt = now,
            )
            dao.insert(entity)
            db.notificationDao().insert(
                NotificationEntity(UUID.randomUUID().toString(), uid, "Booking request saved",
                    "Request $reference for ${service.name} was saved on this device as Pending.", false, now)
            )
            BookingWithService(entity, service).toDomain()
        }
        // Only after the transaction has committed.
        if (db.profileDao().get(uid)?.pushNotifications == true) notifier.notify("Booking request saved", "Reference ${booking.reference}")
        booking
    }

    override suspend fun cancel(uid: String, bookingId: String): Result<Unit> = safeCall {
        val reference = db.withTransaction {
            val dao = db.bookingDao()
            val current = dao.getOwned(uid, bookingId) ?: throw DomainException("Booking not found.")
            if (current.booking.status != BookingStatus.Pending.name) throw DomainException("Only Pending bookings can be cancelled.")
            if (dao.cancelPending(uid, bookingId) != 1) throw DomainException("This booking could no longer be cancelled.")
            db.notificationDao().insert(
                NotificationEntity(UUID.randomUUID().toString(), uid, "Booking cancelled",
                    "Booking ${current.booking.reference} was cancelled.", false, clock.millis())
            )
            current.booking.reference
        }
        if (db.profileDao().get(uid)?.pushNotifications == true) notifier.notify("Booking cancelled", "Reference $reference")
    }
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(private val db: GiantDatabase) : NotificationRepository {
    override fun observeAll(uid: String) = db.notificationDao().observeForOwner(uid).map { l -> l.map { it.toDomain() } }
    override fun observeUnreadCount(uid: String) = db.notificationDao().observeUnreadCount(uid)
    override suspend fun markRead(uid: String, id: String) = safeCall { db.notificationDao().markRead(uid, id); Unit }
    override suspend fun markAllRead(uid: String) = safeCall { db.notificationDao().markAllRead(uid); Unit }
    override suspend fun delete(uid: String, id: String) = safeCall { db.notificationDao().delete(uid, id); Unit }
}

@Singleton
class SupportRepositoryImpl @Inject constructor(private val db: GiantDatabase, private val clock: Clock) : SupportRepository {
    override fun observeFaqs() = db.faqDao().observeAll().map { l -> l.map { it.toDomain() } }

    override suspend fun saveContact(ownerUid: String?, input: ContactInput): Result<Unit> = safeCall {
        val errors = buildMap {
            Validation.contactName(input.name)?.let { put("name", it) }
            Validation.email(input.email)?.let { put("email", it) }
            Validation.contactSubject(input.subject)?.let { put("subject", it) }
            Validation.contactMessage(input.message)?.let { put("message", it) }
        }
        if (errors.isNotEmpty()) throw ValidationException(errors)
        // Saved locally only: this is NOT delivered to the business.
        db.contactDao().insert(ContactMessageEntity(UUID.randomUUID().toString(), ownerUid, input.name.trim(), input.email.trim(),
            input.subject.trim(), input.message.trim(), clock.millis()))
    }
}
