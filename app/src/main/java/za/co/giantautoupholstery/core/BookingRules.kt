package za.co.giantautoupholstery.core

import za.co.giantautoupholstery.domain.Booking
import za.co.giantautoupholstery.domain.BookingStatus
import java.time.LocalDate

object BookingRules {
    /** Upcoming = date today-or-later (Johannesburg) and status not Completed/Cancelled. Everything else is Past. */
    fun isUpcoming(status: BookingStatus, date: LocalDate, today: LocalDate): Boolean =
        !date.isBefore(today) && status != BookingStatus.Completed && status != BookingStatus.Cancelled

    fun partition(bookings: List<Booking>, today: LocalDate): Pair<List<Booking>, List<Booking>> {
        val (up, past) = bookings.partition { isUpcoming(it.status, it.preferredDate, today) }
        return up.sortedWith(compareBy({ it.preferredDate }, { it.preferredTime })) to
            past.sortedWith(compareByDescending<Booking> { it.preferredDate }.thenByDescending { it.preferredTime })
    }
}
