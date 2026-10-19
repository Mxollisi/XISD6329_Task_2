package za.co.giantautoupholstery.core

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** All appointment dates are interpreted in the business timezone, never the phone's arbitrary zone. */
val JOHANNESBURG: ZoneId = ZoneId.of("Africa/Johannesburg")

fun Clock.today(): LocalDate = LocalDate.now(this.withZone(JOHANNESBURG))
