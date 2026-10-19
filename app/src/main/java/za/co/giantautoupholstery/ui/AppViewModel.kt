package za.co.giantautoupholstery.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.giantautoupholstery.domain.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    notifications: NotificationRepository,
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    val authState: StateFlow<AuthState> = auth.state

    /** Destination the user wanted before being asked to sign in (restored after sign-in; survives process death). */
    val pendingRoute: StateFlow<String?> = savedState.getStateFlow<String?>("pending_route", null)
    fun setPendingRoute(route: String?) { savedState["pending_route"] = route }

    private val uid = auth.uidFlow()

    val unreadCount: StateFlow<Int> = uid
        .flatMapLatest { if (it == null) flowOf(0) else notifications.observeUnreadCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val profile: StateFlow<Profile?> = uid
        .flatMapLatest { if (it == null) flowOf(null) else profiles.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Signed-in users use their profile preference; guests use the device-level DataStore value. */
    val themeMode: StateFlow<String> = combine(profile, settings.themeMode) { p, device -> p?.themeMode ?: device }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    private val _sessionMessage = MutableStateFlow<String?>(null)
    val sessionMessage: StateFlow<String?> = _sessionMessage.asStateFlow()
    fun consumeSessionMessage() { _sessionMessage.value = null }

    init {
        viewModelScope.launch {
            auth.state.collect { s ->
                if (s is AuthState.SignedIn) {
                    // Create/retrieve the local Room profile (+ one welcome notification) after first authentication.
                    profiles.ensureProfile(s.user.uid, s.user.displayName ?: s.user.email.substringBefore('@'))
                }
            }
        }
        viewModelScope.launch {
            profile.filterNotNull().map { it.themeMode }.distinctUntilChanged().collect { settings.setThemeMode(it) }
        }
        viewModelScope.launch {
            if (!auth.validateSession()) _sessionMessage.value = "Your session has expired. Please sign in again."
        }
    }

    fun signOut() {
        auth.signOut()
        setPendingRoute(null)
    }
}
