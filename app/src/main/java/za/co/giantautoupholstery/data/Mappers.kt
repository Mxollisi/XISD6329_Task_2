package za.co.giantautoupholstery.data

import za.co.giantautoupholstery.data.local.*
import za.co.giantautoupholstery.domain.*
import java.time.LocalDate

fun ProfileEntity.toDomain() = Profile(uid, fullName, phone, address, emailNotifications, pushNotifications, themeMode)
fun ServiceEntity.toDomain() = Service(id, name, category, priceCents, duration, description, imageRef)
fun NotificationEntity.toDomain() = AppNotification(id, title, body, isRead, createdAt)
fun FaqEntity.toDomain() = Faq(id, question, answer, displayOrder)
fun BookingWithService.toDomain() = Booking(
    id = booking.id, ownerUid = booking.ownerUid, reference = booking.reference,
    serviceId = booking.serviceId, serviceName = service.name,
    preferredDate = LocalDate.parse(booking.preferredDate), preferredTime = booking.preferredTime,
    address = booking.address, description = booking.description,
    status = BookingStatus.valueOf(booking.status), createdAt = booking.createdAt,
)
