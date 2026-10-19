package za.co.giantautoupholstery.data

import za.co.giantautoupholstery.data.local.FaqEntity
import za.co.giantautoupholstery.data.local.GiantDatabase
import za.co.giantautoupholstery.data.local.ServiceEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-safe reference data: the DEMONSTRATION catalogue (prices pending business confirmation) and FAQs.
 * Uses deterministic ids + INSERT IGNORE so it is idempotent and never overwrites existing rows.
 * It never inserts profiles, bookings, notifications or contact messages.
 */
@Singleton
class CatalogSeeder @Inject constructor(private val db: GiantDatabase) {

    private fun id(key: String) = UUID.nameUUIDFromBytes("gau-$key".toByteArray()).toString()

    private data class S(val key: String, val name: String, val cat: String, val rand: Long, val dur: String, val desc: String)

    private val raw = listOf(
        S("dining-chair", "Dining Chair Recovering", "Chairs", 350, "1–2 days", "New fabric or vinyl on dining chair seats, with the old covering removed and the seat base checked."),
        S("office-chair", "Office Chair Repair", "Chairs", 600, "1 day", "Repair of worn or torn office chair seats and backs, including padding and covering."),
        S("steering-wheel", "Steering Wheel Wrap", "Auto", 850, "1 day", "Re-wrapping a worn steering wheel in new leather or similar material for a fresh grip and look."),
        S("foam", "Foam Replacement", "Sofa", 900, "1–2 days", "Replacing flat or sagging seat foam so cushions regain their shape and comfort."),
        S("leather-colour", "Leather Colour Restoration", "Leather", 1100, "1–2 days", "Restoring faded or worn leather colour on sofas, chairs and car seats."),
        S("leather-sofa", "Leather Sofa Repair", "Leather", 1200, "1–2 days", "Repair of cracks, tears and worn areas on leather sofas."),
        S("roof-lining", "Car Roof Lining", "Auto", 1500, "1 day", "Replacing a sagging or stained vehicle roof lining."),
        S("car-seat", "Car Seat Reupholstery", "Auto", 2800, "2–3 days", "Full reupholstery of car seats in fabric, vinyl or leather."),
        S("sofa", "Sofa Reupholstery", "Sofa", 3500, "5–7 days", "Complete reupholstery of lounge furniture with your chosen material."),
        S("boat-caravan", "Boat & Caravan Upholstery", "Auto", 4000, "5–10 days", "Upholstery for boat and caravan seating and interiors, classified under Auto."),
    )

    val services: List<ServiceEntity> = raw.map {
        ServiceEntity(id(it.key), it.name, it.cat, it.rand * 100, it.dur, it.desc, 1_700_000_000_000L, null)
    }

    val faqs: List<FaqEntity> = listOf(
        "How do I request a booking?" to "Sign in, open Services, choose a service and tap Book this service. Pick a preferred date and time, confirm your address and submit.",
        "Is the price I see final?" to "No. Prices are \"from\" estimates for the service. The final cost depends on the work needed and is not confirmed by this app.",
        "Does the app confirm my booking?" to "In this version a booking request is saved on your phone with a reference number. It is not sent to the business and staff confirmation is not part of this version.",
        "Can I choose any time?" to "You can request an hourly time from 08:00 to 16:00 on tomorrow or a later date. A requested time is a preference, not guaranteed availability.",
        "Can I cancel a booking?" to "Yes, you can cancel your own bookings while they are Pending. Open the booking and tap Cancel booking.",
        "How long does a job take?" to "Each service shows a typical duration. These are estimates and actual time depends on the job.",
        "How do I change my details or password?" to "Open Profile, then Settings. You can edit your name, phone and address and change your password if you signed up with email.",
        "Can I sign in with Google?" to "Yes. Choose Continue with Google on the sign-in screen. Your Google password is never seen or stored by this app.",
        "What do notifications do?" to "The Notifications inbox lists updates such as booking saved or cancelled. Mark them read or delete them from the inbox.",
        "How do I contact Giant Auto Upholstery?" to "Use the Contact screen. In this version your message is saved on this device only and is not sent to the business.",
    ).mapIndexed { i, (q, a) -> FaqEntity(id("faq-$i"), q, a, i + 1) }

    suspend fun seed() {
        db.serviceDao().insertAllIgnore(services)
        db.faqDao().insertAllIgnore(faqs)
    }
}
