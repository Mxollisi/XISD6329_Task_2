package za.co.giantautoupholstery.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import za.co.giantautoupholstery.core.ui.EmptyState
import za.co.giantautoupholstery.core.ui.LoadingState
import za.co.giantautoupholstery.core.ui.SignInRequired
import za.co.giantautoupholstery.domain.AuthState
import za.co.giantautoupholstery.ui.AppViewModel
import za.co.giantautoupholstery.ui.api.ApiSyncScreen
import za.co.giantautoupholstery.ui.auth.*
import za.co.giantautoupholstery.ui.bookings.*
import za.co.giantautoupholstery.ui.home.HomeScreen
import za.co.giantautoupholstery.ui.notifications.NotificationsScreen
import za.co.giantautoupholstery.ui.profile.ProfileScreen
import za.co.giantautoupholstery.ui.profile.SettingsScreen
import za.co.giantautoupholstery.ui.services.ServiceDetailScreen
import za.co.giantautoupholstery.ui.services.ServicesScreen
import za.co.giantautoupholstery.ui.support.AboutScreen
import za.co.giantautoupholstery.ui.support.ContactScreen
import za.co.giantautoupholstery.ui.support.HelpScreen

private data class TopLevel(val label: String, val icon: ImageVector, val base: String, val target: String)

private val topLevel = listOf(
    TopLevel("Home", Icons.Filled.Home, Routes.HOME, Routes.HOME),
    TopLevel("Services", Icons.Filled.Chair, "services", Routes.servicesRoute("")),
    TopLevel("Bookings", Icons.Filled.Receipt, Routes.BOOKINGS, Routes.BOOKINGS),
    TopLevel("Profile", Icons.Filled.Person, Routes.PROFILE, Routes.PROFILE),
)

/** Account-only destinations show a sign-in prompt for guests and remember where the user was going. */
@Composable
private fun Guarded(authState: AuthState, pending: String, onSignIn: (String) -> Unit, content: @Composable () -> Unit) {
    when (authState) {
        is AuthState.SignedIn -> content()
        AuthState.Loading -> LoadingState()
        else -> SignInRequired("Please sign in to continue.", onSignIn = { onSignIn(pending) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(appVm: AppViewModel) {
    val nav = rememberNavController()
    val authState by appVm.authState.collectAsStateWithLifecycle()
    val unread by appVm.unreadCount.collectAsStateWithLifecycle()
    val pending by appVm.pendingRoute.collectAsStateWithLifecycle()
    val sessionMessage by appVm.sessionMessage.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    var wasSignedIn by rememberSaveable { mutableStateOf(false) }

    // Auth-driven navigation: resume the intended destination after sign-in, route to email confirmation, reset on logout.
    LaunchedEffect(authState) {
        val current = nav.currentBackStackEntry?.destination?.route
        when (authState) {
            is AuthState.SignedIn -> {
                wasSignedIn = true
                if (current in Routes.AUTH_SCREENS) {
                    val target = pending
                    if (target != null) {
                        appVm.setPendingRoute(null)
                        try {
                            nav.navigate(target) { popUpTo(Routes.HOME) }
                        } catch (e: IllegalArgumentException) { // stale/unknown saved destination
                            nav.navigate(Routes.NOT_FOUND) { popUpTo(Routes.HOME) }
                        }
                    } else nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            }
            is AuthState.PendingVerification -> if (current == Routes.LOGIN || current == Routes.REGISTER) nav.navigate(Routes.VERIFY)
            AuthState.SignedOut -> if (wasSignedIn) {
                wasSignedIn = false
                nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } }
            }
            AuthState.Loading -> Unit
        }
    }
    LaunchedEffect(sessionMessage) {
        sessionMessage?.let { snackbar.showSnackbar(it); appVm.consumeSessionMessage() }
    }

    val showBottomBar = topLevel.any { route != null && route.startsWith(it.base) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(Routes.title(route)) },
                navigationIcon = {
                    if (!showBottomBar && nav.previousBackStackEntry != null) {
                        IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    }
                },
                actions = {
                    IconButton(onClick = { if (route != Routes.NOTIFICATIONS) nav.navigate(Routes.NOTIFICATIONS) }) {
                        BadgedBox(badge = { if (unread > 0) Badge { Text(unread.toString()) } }) {
                            Icon(Icons.Filled.Notifications, if (unread > 0) "Notifications, $unread unread" else "Notifications")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (showBottomBar) NavigationBar {
                topLevel.forEach { item ->
                    NavigationBarItem(
                        selected = route != null && route.startsWith(item.base),
                        onClick = {
                            nav.navigate(item.target) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, null) }, label = { Text(item.label) },
                    )
                }
            }
        },
    ) { inner ->
        val needSignIn: (String) -> Unit = { target -> appVm.setPendingRoute(target); nav.navigate(Routes.LOGIN) }
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(inner)) {
            composable(Routes.HOME) { HomeScreen(nav, authState) }
            composable(
                Routes.SERVICES,
                arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" }),
            ) { ServicesScreen(nav) }
            composable(Routes.SERVICE_DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { ServiceDetailScreen(nav) }
            composable(Routes.BOOKINGS) { Guarded(authState, Routes.BOOKINGS, needSignIn) { BookingsScreen(nav) } }
            composable(Routes.PROFILE) { ProfileScreen(nav, appVm, authState) }
            composable(
                Routes.BOOK,
                arguments = listOf(navArgument("serviceId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { e ->
                Guarded(authState, Routes.book(e.arguments?.getString("serviceId")), needSignIn) { NewBookingScreen(nav) }
            }
            composable(Routes.BOOKING_DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                Guarded(authState, Routes.bookingDetail(e.arguments?.getString("id").orEmpty()), needSignIn) { BookingDetailScreen(nav) }
            }
            composable(Routes.NOTIFICATIONS) { Guarded(authState, Routes.NOTIFICATIONS, needSignIn) { NotificationsScreen() } }
            composable(Routes.SETTINGS) { Guarded(authState, Routes.SETTINGS, needSignIn) { SettingsScreen(authState) } }
            composable(Routes.HELP) { HelpScreen() }
            composable(Routes.ABOUT) { AboutScreen() }
            composable(Routes.CONTACT) { ContactScreen() }
            composable(Routes.API_SYNC) { ApiSyncScreen(nav) }
            composable(Routes.LOGIN) { LoginScreen(nav) }
            composable(Routes.REGISTER) { RegisterScreen(nav) }
            composable(Routes.FORGOT) { ForgotPasswordScreen(nav) }
            composable(Routes.VERIFY) { VerifyEmailScreen(nav, authState) }
            composable(Routes.NOT_FOUND) { UnknownDestination(onHome = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }) }
        }
    }
}

@Composable
fun UnknownDestination(onHome: () -> Unit) {
    EmptyState("Page not found", "That screen does not exist.") { Button(onClick = onHome) { Text("Go home") } }
}
