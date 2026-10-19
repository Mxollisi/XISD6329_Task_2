package za.co.giantautoupholstery.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.giantautoupholstery.BuildConfig
import za.co.giantautoupholstery.core.Validation
import za.co.giantautoupholstery.core.ValidationException
import za.co.giantautoupholstery.core.userMessage
import za.co.giantautoupholstery.data.DevFixtures
import za.co.giantautoupholstery.domain.*
import za.co.giantautoupholstery.ui.currentUid
import javax.inject.Inject

data class SettingsState(
    val loaded: Boolean = false,
    val fullName: String = "", val phone: String = "", val address: String = "",
    val emailNotifications: Boolean = true, val pushNotifications: Boolean = true, val themeMode: String = "system",
    val errors: Map<String, String> = emptyMap(), val saving: Boolean = false, val message: String? = null,
    val currentPassword: String = "", val newPassword: String = "", val confirmPassword: String = "",
    val passwordBusy: Boolean = false, val passwordErrors: Map<String, String> = emptyMap(), val passwordMessage: String? = null,
    val fixtureReport: Map<String, Int>? = null, val fixtureMessage: String? = null, val fixtureBusy: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository, private val profiles: ProfileRepository,
    private val settings: SettingsRepository, private val fixtures: DevFixtures,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    val isDebug = BuildConfig.DEBUG

    init {
        viewModelScope.launch {
            val uid = auth.currentUid() ?: return@launch
            val p = profiles.observe(uid).filterNotNull().first() // populate once; later edits are the user's
            _state.update {
                it.copy(loaded = true, fullName = p.fullName, phone = p.phone.orEmpty(), address = p.address.orEmpty(),
                    emailNotifications = p.emailNotifications, pushNotifications = p.pushNotifications, themeMode = p.themeMode)
            }
        }
    }

    fun edit(f: SettingsState.() -> SettingsState) = _state.update { it.f().copy(message = null) }

    fun saveDetails() {
        val s = _state.value
        val uid = auth.currentUid() ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true, errors = emptyMap(), message = null) }
        viewModelScope.launch {
            profiles.updateDetails(uid, s.fullName, s.phone, s.address).fold(
                onSuccess = { _state.update { it.copy(saving = false, message = "Profile saved.") } },
                onFailure = { e -> _state.update { if (e is ValidationException) it.copy(saving = false, errors = e.errors) else it.copy(saving = false, message = e.userMessage()) } },
            )
        }
    }

    /** Preferences persist immediately (Room profile + device DataStore for the theme). */
    fun savePreferences(email: Boolean = _state.value.emailNotifications, push: Boolean = _state.value.pushNotifications, theme: String = _state.value.themeMode) {
        val uid = auth.currentUid() ?: return
        _state.update { it.copy(emailNotifications = email, pushNotifications = push, themeMode = theme) }
        viewModelScope.launch {
            settings.setThemeMode(theme)
            profiles.updatePreferences(uid, email, push, theme).onFailure { e -> _state.update { it.copy(message = e.userMessage()) } }
        }
    }

    fun changePassword() {
        val s = _state.value
        if (s.passwordBusy) return
        val errors = buildMap {
            Validation.loginPassword(s.currentPassword)?.let { put("current", it) }
            Validation.registrationPassword(s.newPassword)?.let { put("new", it) }
            Validation.confirmPassword(s.newPassword, s.confirmPassword)?.let { put("confirm", it) }
        }
        if (errors.isNotEmpty()) { _state.update { it.copy(passwordErrors = errors, passwordMessage = null) }; return }
        _state.update { it.copy(passwordBusy = true, passwordErrors = emptyMap(), passwordMessage = null) }
        viewModelScope.launch {
            val r = auth.changePassword(s.currentPassword, s.newPassword)
            _state.update {
                when (r) {
                    AuthResult.Success -> it.copy(passwordBusy = false, passwordMessage = "Password changed.", currentPassword = "", newPassword = "", confirmPassword = "")
                    is AuthResult.Error -> it.copy(passwordBusy = false, passwordMessage = r.message)
                    AuthResult.NeedsVerification -> it.copy(passwordBusy = false, passwordMessage = "Please confirm your email first.")
                }
            }
        }
    }

    // ---- Development-only fixture tools (UI only shown when BuildConfig.DEBUG) ----
    fun runFixtures() = fixtureOp { fixtures.setup(auth.currentUid()) }
    fun fixtureCounts() = fixtureOp { Result.success(fixtures.countReport()) }
    fun cleanFixtures() = fixtureOp { fixtures.cleanup() }

    private fun fixtureOp(op: suspend () -> Result<Map<String, Int>>) {
        if (!isDebug || _state.value.fixtureBusy) return
        _state.update { it.copy(fixtureBusy = true) }
        viewModelScope.launch {
            op().fold(
                onSuccess = { r ->
                    val short = fixtures.shortfalls(r)
                    _state.update { it.copy(fixtureBusy = false, fixtureReport = r, fixtureMessage = if (short.isEmpty()) "All six tables have at least ${DevFixtures.MIN_RECORDS} records." else "Below ${DevFixtures.MIN_RECORDS}: ${short.joinToString()}") }
                },
                onFailure = { e -> _state.update { it.copy(fixtureBusy = false, fixtureMessage = e.userMessage()) } },
            )
        }
    }
}
