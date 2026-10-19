package za.co.giantautoupholstery.core

import za.co.giantautoupholstery.domain.BookingInput
import java.time.Clock
import java.time.LocalDate

/** Single shared source of truth for validation. Every function returns an error message or null. */
object Validation {
    private val EMAIL = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    private val PHONE = Regex("^\\+?[0-9 ]{10,15}$")

    /** 08:00 ... 16:00 inclusive. */
    val BOOKING_TIMES: List<String> = (8..16).map { it.toString().padStart(2, '0') + ":00" }

    fun fullName(v: String): String? =
        if (v.trim().length !in 2..80) "Enter your full name (2–80 characters)" else null

    fun email(v: String): String? =
        if (!EMAIL.matches(v.trim())) "Enter a valid email address" else null

    fun registrationPassword(v: String): String? = when {
        v.length < 8 -> "Use at least 8 characters"
        v.none { it.isUpperCase() } -> "Include at least one uppercase letter"
        v.none { it.isDigit() } -> "Include at least one number"
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): String? =
        if (password != confirm) "Passwords do not match" else null

    /** Login only requires non-empty: existing valid credentials must not be rejected by the registration rule. */
    fun loginPassword(v: String): String? = if (v.isEmpty()) "Enter your password" else null

    fun bookingService(id: String?): String? = if (id.isNullOrBlank()) "Choose a service" else null

    /** Tomorrow or later, using Africa/Johannesburg. */
    fun bookingDate(date: LocalDate?, clock: Clock): String? = when {
        date == null -> "Choose a preferred date"
        !date.isAfter(clock.today()) -> "Choose tomorrow or a later date"
        else -> null
    }

    fun bookingTime(v: String?): String? = if (v !in BOOKING_TIMES) "Choose a time between 08:00 and 16:00" else null

    fun bookingAddress(v: String): String? =
        if (v.trim().length !in 5..200) "Enter an address (5–200 characters)" else null

    fun bookingDescription(v: String): String? =
        if (v.length > 500) "Keep the description under 500 characters" else null

    fun profilePhone(v: String): String? =
        if (v.isNotBlank() && !PHONE.matches(v.trim())) "Enter 10–15 digits (a leading + is allowed)" else null

    fun profileAddress(v: String): String? =
        if (v.trim().length > 200) "Keep the address under 200 characters" else null

    fun contactName(v: String) = fullName(v)
    fun contactSubject(v: String): String? = if (v.trim().length !in 3..120) "Subject must be 3–120 characters" else null
    fun contactMessage(v: String): String? = if (v.trim().length !in 10..1000) "Message must be 10–1000 characters" else null

    /** Field-keyed errors for a whole booking; used by the ViewModel AND the repository (never UI-only). */
    fun booking(input: BookingInput, clock: Clock): Map<String, String> = buildMap {
        bookingService(input.serviceId)?.let { put("service", it) }
        bookingDate(input.date, clock)?.let { put("date", it) }
        bookingTime(input.time)?.let { put("time", it) }
        bookingAddress(input.address)?.let { put("address", it) }
        bookingDescription(input.description).let { if (it != null) put("description", it) }
        if (!input.termsAccepted) put("terms", "Please accept the booking terms")
    }
}
