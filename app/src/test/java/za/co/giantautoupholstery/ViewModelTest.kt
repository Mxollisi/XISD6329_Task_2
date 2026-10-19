package za.co.giantautoupholstery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import androidx.lifecycle.SavedStateHandle
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import za.co.giantautoupholstery.domain.*
import za.co.giantautoupholstery.ui.auth.AuthViewModel
import za.co.giantautoupholstery.ui.services.ServicesUi
import za.co.giantautoupholstery.ui.services.ServicesViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val sample = listOf(
        Service("1", "Dining Chair Recovering", "Chairs", 35_000, "1–2 days", "Seats", null),
        Service("2", "Leather Sofa Repair", "Leather", 120_000, "1–2 days", "Cracks", null),
    )

    private class FakeServices(val source: Flow<List<Service>>) : ServiceRepository {
        override fun observeAll() = source
        override fun observe(id: String) = flowOf<Service?>(null)
    }

    private fun <T> StateFlow<T>.collectIn(scope: kotlinx.coroutines.CoroutineScope) = onEach { }.launchIn(scope)

    @Test fun services_content_empty_noMatch_and_error() = runTest {
        val vm = ServicesViewModel(FakeServices(flowOf(sample)), SavedStateHandle())
        vm.ui.collectIn(backgroundScope)
        yield()
        assertTrue(vm.ui.value is ServicesUi.Content)
        vm.query.value = "zzz"
        yield()
        assertEquals(ServicesUi.Empty(false), vm.ui.value)

        val empty = ServicesViewModel(FakeServices(flowOf(emptyList())), SavedStateHandle())
        empty.ui.collectIn(backgroundScope)
        yield()
        assertEquals(ServicesUi.Empty(true), empty.ui.value)

        val failing = ServicesViewModel(FakeServices(flow { throw IllegalStateException("db") }), SavedStateHandle())
        failing.ui.collectIn(backgroundScope)
        yield()
        assertEquals(ServicesUi.Error, failing.ui.value)
    }

    private class FakeAuth : AuthRepository {
        var signInCalls = 0
        override val state = MutableStateFlow<AuthState>(AuthState.SignedOut)
        override suspend fun signUp(fullName: String, email: String, password: String): AuthResult = AuthResult.NeedsVerification
        override suspend fun signIn(email: String, password: String): AuthResult { signInCalls++; return AuthResult.Error("Incorrect email or password.") }
        override suspend fun signInWithGoogle(idToken: String): AuthResult = AuthResult.Success
        override suspend fun resendVerification(): AuthResult = AuthResult.Success
        override suspend fun refreshVerification(): AuthResult = AuthResult.NeedsVerification
        override suspend fun sendPasswordReset(email: String): AuthResult = AuthResult.Success
        override suspend fun changePassword(current: String, new: String): AuthResult = AuthResult.Success
        override suspend fun validateSession() = true
        override fun signOut() {}
    }

    @Test fun login_validatesBeforeCallingAuth_and_showsSafeError() = runTest {
        val auth = FakeAuth()
        val vm = AuthViewModel(auth)
        vm.login()
        assertEquals(0, auth.signInCalls)
        assertNotNull(vm.state.value.errors["email"])
        assertNotNull(vm.state.value.errors["password"])

        vm.edit { copy(email = "a@b.co", password = "weak") } // weak password is allowed at login
        vm.login()
        assertEquals(1, auth.signInCalls)
        assertEquals("Incorrect email or password.", vm.state.value.error)
        assertFalse(vm.state.value.busy)
    }

    @Test fun register_requires_matching_strong_password() = runTest {
        val vm = AuthViewModel(FakeAuth())
        vm.edit { copy(fullName = "Mxo Mngomeni", email = "a@b.co", password = "weakpass", confirm = "weakpass") }
        vm.register()
        assertNotNull(vm.state.value.errors["password"])
        vm.edit { copy(password = "Strong123", confirm = "Different1") }
        vm.register()
        assertNotNull(vm.state.value.errors["confirm"])
        vm.edit { copy(confirm = "Strong123") }
        vm.register()
        assertEquals("Please confirm your email address to continue.", vm.state.value.info)
    }
}
