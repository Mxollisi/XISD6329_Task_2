package za.co.giantautoupholstery

import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import za.co.giantautoupholstery.core.Validation
import za.co.giantautoupholstery.domain.BookingInput
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone

class ValidationTest {
    private val originalTz = TimeZone.getDefault()
    @After fun restore() = TimeZone.setDefault(originalTz)

    private fun clockAt(utc: String) = Clock.fixed(Instant.parse(utc), ZoneOffset.UTC)

    @Test fun fullName_boundaries() {
        assertNotNull(Validation.fullName("A"))
        assertNull(Validation.fullName("Al"))
        assertNull(Validation.fullName("A".repeat(80)))
        assertNotNull(Validation.fullName("A".repeat(81)))
        assertNotNull(Validation.fullName("   A   ")) // trimmed length 1
    }

    @Test fun email_format() {
        assertNull(Validation.email("mxo@example.co.za"))
        assertNotNull(Validation.email("not-an-email"))
        assertNotNull(Validation.email("a@b"))
    }

    @Test fun registrationPassword_rules() {
        assertNotNull(Validation.registrationPassword("Abc1"))          // too short
        assertNotNull(Validation.registrationPassword("abcdefg1"))     // no uppercase
        assertNotNull(Validation.registrationPassword("Abcdefgh"))     // no number
        assertNull(Validation.registrationPassword("Abcdefg1"))        // exactly 8
        assertNotNull(Validation.confirmPassword("Abcdefg1", "Abcdefg2"))
        assertNull(Validation.confirmPassword("Abcdefg1", "Abcdefg1"))
    }

    @Test fun loginPassword_onlyRequiresNonEmpty() {
        assertNotNull(Validation.loginPassword(""))
        assertNull(Validation.loginPassword("weak")) // existing credentials must not be rejected by registration rule
    }

    @Test fun bookingTimes_are_0800_to_1600_inclusive() {
        assertEquals(9, Validation.BOOKING_TIMES.size)
        assertEquals("08:00", Validation.BOOKING_TIMES.first())
        assertEquals("16:00", Validation.BOOKING_TIMES.last())
        assertNull(Validation.bookingTime("12:00"))
        assertNotNull(Validation.bookingTime("07:00"))
        assertNotNull(Validation.bookingTime("17:00"))
        assertNotNull(Validation.bookingTime("08:30"))
        assertNotNull(Validation.bookingTime(null))
    }

    @Test fun bookingDate_tomorrowOrLater_inJohannesburg() {
        val clock = clockAt("2026-10-18T08:00:00Z") // 10:00 SAST on 18 Oct
        assertNotNull(Validation.bookingDate(null, clock))
        assertNotNull(Validation.bookingDate(LocalDate.of(2026, 10, 17), clock))
        assertNotNull(Validation.bookingDate(LocalDate.of(2026, 10, 18), clock)) // today is rejected
        assertNull(Validation.bookingDate(LocalDate.of(2026, 10, 19), clock))
    }

    @Test fun bookingDate_johannesburgMidnightBoundary() {
        // 21:59:59Z = 23:59:59 SAST on 18 Oct -> tomorrow is 19 Oct
        val before = clockAt("2026-10-18T21:59:59Z")
        assertNull(Validation.bookingDate(LocalDate.of(2026, 10, 19), before))
        // 22:00:00Z = 00:00:00 SAST on 19 Oct -> 19 Oct is now "today" and rejected
        val after = clockAt("2026-10-18T22:00:00Z")
        assertNotNull(Validation.bookingDate(LocalDate.of(2026, 10, 19), after))
        assertNull(Validation.bookingDate(LocalDate.of(2026, 10, 20), after))
    }

    @Test fun bookingDate_ignoresDeviceTimezone() {
        val clock = clockAt("2026-10-18T21:59:59Z")
        for (zone in listOf("Pacific/Auckland", "America/Los_Angeles", "UTC", "Africa/Johannesburg")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            assertNull("zone $zone", Validation.bookingDate(LocalDate.of(2026, 10, 19), clock))
            assertNotNull("zone $zone", Validation.bookingDate(LocalDate.of(2026, 10, 18), clock))
        }
    }

    @Test fun bookingAddress_and_description_boundaries() {
        assertNotNull(Validation.bookingAddress("1234"))
        assertNull(Validation.bookingAddress("12345"))
        assertNull(Validation.bookingAddress("a".repeat(200)))
        assertNotNull(Validation.bookingAddress("a".repeat(201)))
        assertNull(Validation.bookingDescription("a".repeat(500)))
        assertNotNull(Validation.bookingDescription("a".repeat(501)))
        assertNull(Validation.bookingDescription(""))
    }

    @Test fun profilePhone_optionalButPatterned() {
        assertNull(Validation.profilePhone(""))
        assertNull(Validation.profilePhone("0821234567"))
        assertNull(Validation.profilePhone("+27 82 123 4567"))
        assertNotNull(Validation.profilePhone("12345"))
        assertNotNull(Validation.profilePhone("08212345678901234"))
        assertNotNull(Validation.profilePhone("082-123-4567"))
        assertNull(Validation.profileAddress("a".repeat(200)))
        assertNotNull(Validation.profileAddress("a".repeat(201)))
    }

    @Test fun contact_boundaries() {
        assertNotNull(Validation.contactSubject("ab"))
        assertNull(Validation.contactSubject("abc"))
        assertNotNull(Validation.contactSubject("a".repeat(121)))
        assertNotNull(Validation.contactMessage("123456789"))
        assertNull(Validation.contactMessage("1234567890"))
        assertNull(Validation.contactMessage("a".repeat(1000)))
        assertNotNull(Validation.contactMessage("a".repeat(1001)))
    }

    @Test fun wholeBooking_collectsFieldErrors() {
        val clock = clockAt("2026-10-18T08:00:00Z")
        val bad = BookingInput(null, null, null, "", "a".repeat(501), false)
        assertEquals(setOf("service", "date", "time", "address", "description", "terms"), Validation.booking(bad, clock).keys)
        val good = BookingInput("svc", LocalDate.of(2026, 10, 19), "09:00", "12 Main Road, Johannesburg", "", true)
        assertTrue(Validation.booking(good, clock).isEmpty())
    }
}
