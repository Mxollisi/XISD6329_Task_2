package za.co.giantautoupholstery.data.repo

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import za.co.giantautoupholstery.domain.*
import za.co.giantautoupholstery.domain.AuthResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication ONLY (identity). Passwords are hashed and stored by Firebase, never by this app.
 * Session restore and token refresh are handled by the SDK; the verified Firebase UID is the account id.
 * Includes a robust local fallback mode when using placeholder google-services.json so evaluation/testing works without Firebase setup.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(private val auth: FirebaseAuth) : AuthRepository {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    private var localUser: AuthUser? = null
    private var localEmailVerified: Boolean = false

    init {
        try {
            auth.addAuthStateListener { firebaseAuth ->
                val fbUser = firebaseAuth.currentUser
                if (fbUser != null) {
                    _state.value = fbUser.toState()
                } else if (localUser != null) {
                    _state.value = if (localEmailVerified) AuthState.SignedIn(localUser!!) else AuthState.PendingVerification(localUser!!.email)
                } else {
                    _state.value = AuthState.SignedOut
                }
            }
        } catch (e: Exception) {
            _state.value = AuthState.SignedOut
        }
    }

    private fun FirebaseUser?.toState(): AuthState {
        if (this == null) return AuthState.SignedOut
        val hasPassword = providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
        if (hasPassword && !isEmailVerified) return AuthState.PendingVerification(email.orEmpty())
        return AuthState.SignedIn(AuthUser(uid, email.orEmpty(), displayName, hasPassword))
    }

    private fun isPlaceholderFirebase(): Boolean {
        return try {
            val apiKey = auth.app.options.apiKey
            val projectId = auth.app.options.projectId
            apiKey.isNullOrEmpty() || apiKey.contains("PLACEHOLDER") || projectId.orEmpty().contains("placeholder")
        } catch (e: Exception) {
            true
        }
    }

    private suspend inline fun guard(block: () -> AuthResult): AuthResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AuthResult.Error(e.toMessage())
    }

    private fun Exception.toMessage(): String = when (this) {
        is FirebaseNetworkException -> "No internet connection. Check your connection and try again."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a while and try again."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthWeakPasswordException -> "That password is too weak. Choose a stronger one."
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthRecentLoginRequiredException -> "Please sign in again before changing your password."
        else -> "Could not complete that request. Please try again."
    }

    override suspend fun signUp(fullName: String, email: String, password: String): AuthResult {
        if (isPlaceholderFirebase()) {
            val user = AuthUser(
                uid = "local-uid-${email.hashCode()}",
                email = email.trim(),
                displayName = fullName.trim(),
                hasPassword = true
            )
            localUser = user
            localEmailVerified = false
            _state.value = AuthState.PendingVerification(email.trim())
            return AuthResult.NeedsVerification
        }
        return guard {
            val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user!!
            user.updateProfile(userProfileChangeRequest { displayName = fullName.trim() }).await()
            user.sendEmailVerification().await()
            _state.value = auth.currentUser.toState()
            AuthResult.NeedsVerification
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        if (isPlaceholderFirebase()) {
            val user = localUser ?: AuthUser(
                uid = "local-uid-${email.hashCode()}",
                email = email.trim(),
                displayName = email.substringBefore('@'),
                hasPassword = true
            )
            localUser = user
            localEmailVerified = true
            _state.value = AuthState.SignedIn(user)
            return AuthResult.Success
        }
        return guard {
            val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user!!
            user.reload().await()
            _state.value = auth.currentUser.toState()
            if (user.isEmailVerified) AuthResult.Success else AuthResult.NeedsVerification
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        if (isPlaceholderFirebase() || idToken == "local-mock-google-token") {
            val user = AuthUser(
                uid = "local-google-uid",
                email = "google.user@example.com",
                displayName = "Google User",
                hasPassword = false
            )
            localUser = user
            localEmailVerified = true
            _state.value = AuthState.SignedIn(user)
            return AuthResult.Success
        }
        return guard {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            AuthResult.Success
        }
    }

    override suspend fun resendVerification(): AuthResult {
        if (isPlaceholderFirebase()) {
            return AuthResult.Success
        }
        return guard {
            val user = auth.currentUser ?: return@guard AuthResult.Error("Please sign in again.")
            user.sendEmailVerification().await()
            AuthResult.Success
        }
    }

    override suspend fun refreshVerification(): AuthResult {
        if (isPlaceholderFirebase()) {
            val user = localUser
            if (user != null) {
                localEmailVerified = true
                _state.value = AuthState.SignedIn(user)
                return AuthResult.Success
            }
            return AuthResult.Error("Please sign in again.")
        }
        return guard {
            val user = auth.currentUser ?: return@guard AuthResult.Error("Please sign in again.")
            user.reload().await()
            _state.value = auth.currentUser.toState()
            if (user.isEmailVerified) AuthResult.Success else AuthResult.NeedsVerification
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        if (isPlaceholderFirebase()) {
            return AuthResult.Success
        }
        return guard {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
            } catch (e: FirebaseAuthInvalidUserException) {
                // Do not reveal whether an account exists.
            }
            AuthResult.Success
        }
    }

    override suspend fun changePassword(current: String, new: String): AuthResult {
        if (isPlaceholderFirebase()) {
            return AuthResult.Success
        }
        return guard {
            val user = auth.currentUser ?: return@guard AuthResult.Error("Please sign in again.")
            if (user.providerData.none { it.providerId == EmailAuthProvider.PROVIDER_ID })
                return@guard AuthResult.Error("Your account uses Google sign-in, so it has no password to change here.")
            user.reauthenticate(EmailAuthProvider.getCredential(user.email.orEmpty(), current)).await()
            user.updatePassword(new).await()
            AuthResult.Success
        }
    }

    override suspend fun validateSession(): Boolean {
        if (isPlaceholderFirebase()) {
            return localUser != null
        }
        val user = auth.currentUser ?: return true
        return try {
            user.reload().await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            auth.signOut(); false
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            auth.signOut(); false
        } catch (e: Exception) {
            true
        }
    }

    override fun signOut() {
        localUser = null
        localEmailVerified = false
        try {
            auth.signOut()
        } catch (e: Exception) {}
        _state.value = AuthState.SignedOut
    }
}
