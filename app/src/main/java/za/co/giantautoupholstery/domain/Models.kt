package za.co.giantautoupholstery.domain

import java.time.LocalDate

enum class BookingStatus { Pending, Confirmed, Completed, Cancelled }

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String?,
    val hasPassword: Boolean,
)

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    /** Signed in with Firebase but the email is not confirmed yet: NOT treated as an app session. */
    data class PendingVerification(val email: String) : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

sealed interface AuthResult {
    data object Success : AuthResult
    data object NeedsVerification : AuthResult
    data class Error(val message: String) : AuthResult
}

data class Profile(
    val uid: String,
    val fullName: String,
    val phone: String?,
    val address: String?,
    val emailNotifications: Boolean,
    val pushNotifications: Boolean,
    val themeMode: String, // "system" | "light" | "dark"
)

data class Service(
    val id: String,
    val name: String,
    val category: String,
    val priceCents: Long,
    val duration: String,
    val description: String,
    val imageRef: String?,
)

data class Booking(
    val id: String,
    val ownerUid: String,
    val reference: String,
    val serviceId: String,
    val serviceName: String,
    val preferredDate: LocalDate,
    val preferredTime: String,
    val address: String,
    val description: String?,
    val status: BookingStatus,
    val createdAt: Long,
)

data class BookingInput(
    val serviceId: String?,
    val date: LocalDate?,
    val time: String?,
    val address: String,
    val description: String,
    val termsAccepted: Boolean,
)

data class AppNotification(val id: String, val title: String, val body: String, val isRead: Boolean, val createdAt: Long)
data class Faq(val id: String, val question: String, val answer: String, val displayOrder: Int)
data class ContactInput(val name: String, val email: String, val subject: String, val message: String)

val SERVICE_CATEGORIES = listOf("Sofa", "Leather", "Chairs", "Auto")
