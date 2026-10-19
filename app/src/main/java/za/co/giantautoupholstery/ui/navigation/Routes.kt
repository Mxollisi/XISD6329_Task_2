package za.co.giantautoupholstery.ui.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val SERVICES = "services?query={query}"
    const val BOOKINGS = "bookings"
    const val PROFILE = "profile"
    const val SERVICE_DETAIL = "service/{id}"
    const val BOOK = "book?serviceId={serviceId}"
    const val BOOKING_DETAIL = "booking/{id}"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val ABOUT = "about"
    const val CONTACT = "contact"
    const val API_SYNC = "api-sync"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val VERIFY = "verify"
    const val NOT_FOUND = "not-found"

    val AUTH_SCREENS = setOf(LOGIN, REGISTER, FORGOT, VERIFY)

    fun servicesRoute(query: String) = "services?query=" + Uri.encode(query)
    fun serviceDetail(id: String) = "service/$id"
    fun book(serviceId: String?) = if (serviceId == null) "book" else "book?serviceId=$serviceId"
    fun bookingDetail(id: String) = "booking/$id"

    fun title(route: String?): String = when {
        route == null -> "Giant Auto Upholstery"
        route == HOME -> "Giant Auto Upholstery"
        route.startsWith("services") -> "Services"
        route == BOOKINGS -> "Bookings"
        route == PROFILE -> "Profile"
        route == SERVICE_DETAIL -> "Service details"
        route.startsWith("book?") || route == "book" -> "New booking"
        route == BOOKING_DETAIL -> "Booking details"
        route == NOTIFICATIONS -> "Notifications"
        route == SETTINGS -> "Settings"
        route == HELP -> "Help"
        route == ABOUT -> "About"
        route == CONTACT -> "Contact"
        route == API_SYNC -> "API & Cloud Sync"
        route == LOGIN -> "Sign in"
        route == REGISTER -> "Create account"
        route == FORGOT -> "Forgot password"
        route == VERIFY -> "Confirm email"
        route == NOT_FOUND -> "Not found"
        else -> "Giant Auto Upholstery"
    }
}
