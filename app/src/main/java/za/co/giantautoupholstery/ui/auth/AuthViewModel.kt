package za.co.giantautoupholstery.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.giantautoupholstery.core.Validation
import za.co.giantautoupholstery.domain.AuthRepository
import za.co.giantautoupholstery.domain.AuthResult
import javax.inject.Inject

/** Passwords live only in this ViewModel's memory (never SavedStateHandle, DataStore, SQLite or logs). */
data class AuthFormState(
    val fullName: String = "", val email: String = "", val password: String = "", val confirm: String = "",
    val busy: Boolean = false, val errors: Map<String, String> = emptyMap(),
    val error: String? = null, val info: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    fun edit(f: AuthFormState.() -> AuthFormState) = _state.update { it.f().copy(error = null, info = null) }

    private fun handle(r: AuthResult, onSuccessInfo: String? = null) = _state.update {
        when (r) {
            AuthResult.Success -> it.copy(busy = false, info = onSuccessInfo)
            AuthResult.NeedsVerification -> it.copy(busy = false, info = "Please confirm your email address to continue.")
            is AuthResult.Error -> it.copy(busy = false, error = r.message)
        }
    }

    fun login() {
        val s = _state.value
        if (s.busy) return
        val errors = buildMap {
            Validation.email(s.email)?.let { put("email", it) }
            Validation.loginPassword(s.password)?.let { put("password", it) }
        }
        if (errors.isNotEmpty()) { _state.update { it.copy(errors = errors) }; return }
        _state.update { it.copy(busy = true, errors = emptyMap(), error = null) }
        viewModelScope.launch { handle(auth.signIn(s.email, s.password)) }
    }

    fun register() {
        val s = _state.value
        if (s.busy) return
        val errors = buildMap {
            Validation.fullName(s.fullName)?.let { put("fullName", it) }
            Validation.email(s.email)?.let { put("email", it) }
            Validation.registrationPassword(s.password)?.let { put("password", it) }
            Validation.confirmPassword(s.password, s.confirm)?.let { put("confirm", it) }
        }
        if (errors.isNotEmpty()) { _state.update { it.copy(errors = errors) }; return }
        _state.update { it.copy(busy = true, errors = emptyMap(), error = null) }
        viewModelScope.launch { handle(auth.signUp(s.fullName, s.email, s.password)) }
    }

    fun sendReset() {
        val s = _state.value
        if (s.busy) return
        Validation.email(s.email)?.let { e -> _state.update { it.copy(errors = mapOf("email" to e)) }; return }
        _state.update { it.copy(busy = true, errors = emptyMap(), error = null) }
        viewModelScope.launch {
            handle(auth.sendPasswordReset(s.email), "If an account exists for that email, a reset link has been sent. Open it in your email app, set a new password in the page that opens, then sign in here.")
        }
    }

    fun resendVerification() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch { handle(auth.resendVerification(), "Verification email sent. Check your inbox and spam folder.") }
    }

    fun refreshVerification() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.refreshVerification()) {
                AuthResult.Success -> _state.update { it.copy(busy = false) } // AuthState flips to SignedIn -> navigation
                AuthResult.NeedsVerification -> _state.update { it.copy(busy = false, info = "Your email is not confirmed yet. Open the link in the email we sent, then try again.") }
                is AuthResult.Error -> _state.update { it.copy(busy = false, error = r.message) }
            }
        }
    }

    fun googleToken(idToken: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch { handle(auth.signInWithGoogle(idToken)) }
    }

    fun showError(message: String) = _state.update { it.copy(busy = false, error = message) }
    fun signOut() = auth.signOut()
}
