package za.co.giantautoupholstery.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Never stores passwords or hashes: authentication is entirely handled by Firebase Authentication. */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val uid: String,
    val fullName: String,
    val phone: String?,
    val address: String?,
    val emailNotifications: Boolean,
    val pushNotifications: Boolean,
    val themeMode: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val priceCents: Long,
    val duration: String,
    val description: String,
    val createdAt: Long,
    val imageRef: String?,
)

@Entity(
    tableName = "bookings",
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["uid"], childColumns = ["ownerUid"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ServiceEntity::class, parentColumns = ["id"], childColumns = ["serviceId"]),
    ],
    indices = [
        Index("ownerUid"), Index("serviceId"), Index("status"),
        Index(value = ["ownerUid", "preferredDate"]),
        Index(value = ["reference"], unique = true),
        Index(value = ["submissionId"], unique = true),
    ],
)
data class BookingEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val reference: String,
    val serviceId: String,
    val preferredDate: String, // ISO yyyy-MM-dd, interpreted in Africa/Johannesburg
    val preferredTime: String, // HH:mm
    val address: String,
    val description: String?,
    val status: String,        // Pending | Confirmed | Completed | Cancelled
    val submissionId: String,
    val createdAt: Long,       // epoch millis (UTC instant)
)

@Entity(
    tableName = "notifications",
    foreignKeys = [ForeignKey(entity = ProfileEntity::class, parentColumns = ["uid"], childColumns = ["ownerUid"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("ownerUid"), Index(value = ["ownerUid", "isRead"])],
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "faqs")
data class FaqEntity(
    @PrimaryKey val id: String,
    val question: String,
    val answer: String,
    val displayOrder: Int,
)

@Entity(
    tableName = "contact_messages",
    foreignKeys = [ForeignKey(entity = ProfileEntity::class, parentColumns = ["uid"], childColumns = ["ownerUid"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("ownerUid")],
)
data class ContactMessageEntity(
    @PrimaryKey val id: String,
    val ownerUid: String?,
    val name: String,
    val email: String,
    val subject: String,
    val message: String,
    val createdAt: Long,
)
