package za.co.giantautoupholstery.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun signUp(fullName: String, email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun resendVerification(): AuthResult
    /** Reloads the Firebase user and publishes the new state (used after the user taps the email link). */
    suspend fun refreshVerification(): AuthResult
    suspend fun sendPasswordReset(email: String): AuthResult
    suspend fun changePassword(current: String, new: String): AuthResult
    /** false when Firebase says the account/session is no longer valid (the user is signed out). */
    suspend fun validateSession(): Boolean
    fun signOut()
}

interface ProfileRepository {
    fun observe(uid: String): Flow<Profile?>
    /** Creates the local profile + exactly one welcome notification in one transaction (no-op if it exists). */
    suspend fun ensureProfile(uid: String, fullName: String): Result<Unit>
    suspend fun updateDetails(uid: String, fullName: String, phone: String, address: String): Result<Unit>
    suspend fun updatePreferences(uid: String, email: Boolean, push: Boolean, themeMode: String): Result<Unit>
}

interface ServiceRepository {
    fun observeAll(): Flow<List<Service>>
    fun observe(id: String): Flow<Service?>
}

interface BookingRepository {
    fun observeAll(uid: String): Flow<List<Booking>>
    fun observe(uid: String, id: String): Flow<Booking?>
    suspend fun create(uid: String, input: BookingInput, submissionId: String): Result<Booking>
    suspend fun cancel(uid: String, bookingId: String): Result<Unit>
}

interface NotificationRepository {
    fun observeAll(uid: String): Flow<List<AppNotification>>
    fun observeUnreadCount(uid: String): Flow<Int>
    suspend fun markRead(uid: String, id: String): Result<Unit>
    suspend fun markAllRead(uid: String): Result<Unit>
    suspend fun delete(uid: String, id: String): Result<Unit>
}

interface SupportRepository {
    fun observeFaqs(): Flow<List<Faq>>
    suspend fun saveContact(ownerUid: String?, input: ContactInput): Result<Unit>
}

interface SettingsRepository {
    val themeMode: Flow<String>
    suspend fun setThemeMode(mode: String)
}

/** Posts an OS notification for a locally generated event (NOT remote push). */
interface LocalNotifier {
    fun notify(title: String, body: String)
}
