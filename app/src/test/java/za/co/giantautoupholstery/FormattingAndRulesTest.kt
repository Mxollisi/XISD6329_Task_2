package za.co.giantautoupholstery

import org.junit.Assert.*
import org.junit.Test
import za.co.giantautoupholstery.core.*
import za.co.giantautoupholstery.domain.Booking
import za.co.giantautoupholstery.domain.BookingStatus
import za.co.giantautoupholstery.domain.Service
import za.co.giantautoupholstery.ui.services.filterServices
import java.time.LocalDate
import kotlin.random.Random

class FormattingAndRulesTest {
    @Test fun formatRand_examples() {
        assertEquals("R350", formatRand(35_000))
        assertEquals("R1 200", formatRand(120_000))
        assertEquals("R4 000", formatRand(400_000))
        assertEquals("R1 234 567", formatRand(123_456_700))
        assertEquals("R350,50", formatRand(35_050))
        assertEquals("R0", formatRand(0))
    }

    @Test fun initials_and_firstName() {
        assertEquals("MM", initials("Mxo Mngomeni"))
        assertEquals("M", initials("Mxo"))
        assertEquals("?", initials(""))
        assertEquals("Mxo", firstName("Mxo Mngomeni"))
        assertEquals("there", firstName(null))
    }

    @Test fun reference_format() {
        val ref = generateReference(Random(1))
        assertTrue(Regex("^GAU-[A-Z2-9]{6}$").matches(ref))
        assertFalse(ref.contains('O') || ref.contains('0') || ref.contains('I') || ref.contains('1'))
    }

    private fun booking(status: BookingStatus, date: LocalDate, time: String = "09:00") =
        Booking("id-$status-$date-$time", "u", "GAU-AAAAAA", "s", "Svc", date, time, "addr", null, status, 0)

    @Test fun upcoming_vs_past_classification() {
        val today = LocalDate.of(2026, 10, 18)
        assertTrue(BookingRules.isUpcoming(BookingStatus.Pending, today, today))               // today counts as upcoming
        assertTrue(BookingRules.isUpcoming(BookingStatus.Confirmed, today.plusDays(5), today))
        assertFalse(BookingRules.isUpcoming(BookingStatus.Pending, today.minusDays(1), today)) // past date
        assertFalse(BookingRules.isUpcoming(BookingStatus.Completed, today.plusDays(1), today))
        assertFalse(BookingRules.isUpcoming(BookingStatus.Cancelled, today.plusDays(1), today))
    }

    @Test fun partition_sortsUpcomingAscending_pastDescending() {
        val today = LocalDate.of(2026, 10, 18)
        val list = listOf(
            booking(BookingStatus.Pending, today.plusDays(9)), booking(BookingStatus.Pending, today.plusDays(2)),
            booking(BookingStatus.Completed, today.minusDays(3)), booking(BookingStatus.Cancelled, today.minusDays(10)),
        )
        val (up, past) = BookingRules.partition(list, today)
        assertEquals(listOf(today.plusDays(2), today.plusDays(9)), up.map { it.preferredDate })
        assertEquals(listOf(today.minusDays(3), today.minusDays(10)), past.map { it.preferredDate })
    }

    private val services = listOf(
        Service("1", "Dining Chair Recovering", "Chairs", 35_000, "1–2 days", "New fabric on seats", null),
        Service("2", "Leather Sofa Repair", "Leather", 120_000, "1–2 days", "Cracks and tears", null),
        Service("3", "Car Roof Lining", "Auto", 150_000, "1 day", "Sagging lining", null),
    )

    @Test fun filter_by_query_and_category() {
        assertEquals(3, filterServices(services, "", "All").size)
        assertEquals(listOf("2"), filterServices(services, "sofa", "All").map { it.id })
        assertEquals(listOf("2"), filterServices(services, "TEARS", "All").map { it.id }) // description match, case-insensitive
        assertEquals(listOf("3"), filterServices(services, "", "Auto").map { it.id })
        assertTrue(filterServices(services, "sofa", "Auto").isEmpty())
    }
}
