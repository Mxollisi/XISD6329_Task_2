package za.co.giantautoupholstery.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import za.co.giantautoupholstery.core.ui.*
import za.co.giantautoupholstery.domain.AuthState
import za.co.giantautoupholstery.ui.navigation.Routes

@Composable
private fun AuthColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.s), content = content,
    )
}

@Composable
private fun Messages(s: AuthFormState) {
    s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    s.info?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun GoogleButton(enabled: Boolean, onToken: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            scope.launch {
                requestGoogleIdToken(context).fold(
                    onSuccess = onToken,
                    onFailure = { if (it !is GoogleCancelled) onError(it.message ?: "Google sign-in failed.") },
                )
            }
        },
        enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text("Continue with Google") }
}

@Composable
fun LoginScreen(nav: NavHostController, vm: AuthViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    AuthColumn {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
        FormField(s.email, { v -> vm.edit { copy(email = v) } }, "Email", error = s.errors["email"], keyboardType = KeyboardType.Email, enabled = !s.busy)
        PasswordField(s.password, { v -> vm.edit { copy(password = v) } }, "Password", error = s.errors["password"], enabled = !s.busy)
        Messages(s)
        BusyButton("Sign in", s.busy, vm::login)
        TextButton(onClick = { nav.navigate(Routes.FORGOT) }, Modifier.heightIn(min = 48.dp)) { Text("Forgot password?") }
        HorizontalDivider()
        GoogleButton(!s.busy, vm::googleToken, vm::showError)
        TextButton(onClick = { nav.navigate(Routes.REGISTER) }, Modifier.heightIn(min = 48.dp)) { Text("New here? Create an account") }
    }
}

@Composable
fun RegisterScreen(nav: NavHostController, vm: AuthViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    AuthColumn {
        Text("Create your account", style = MaterialTheme.typography.headlineMedium)
        FormField(s.fullName, { v -> vm.edit { copy(fullName = v) } }, "Full name", error = s.errors["fullName"], enabled = !s.busy)
        FormField(s.email, { v -> vm.edit { copy(email = v) } }, "Email", error = s.errors["email"], keyboardType = KeyboardType.Email, enabled = !s.busy)
        PasswordField(s.password, { v -> vm.edit { copy(password = v) } }, "Password", error = s.errors["password"], enabled = !s.busy)
        Text("At least 8 characters, one uppercase letter and one number.", style = MaterialTheme.typography.bodySmall)
        PasswordField(s.confirm, { v -> vm.edit { copy(confirm = v) } }, "Confirm password", error = s.errors["confirm"], enabled = !s.busy)
        Messages(s)
        Text("We will email you a link to confirm your address before you can sign in.", style = MaterialTheme.typography.bodySmall)
        BusyButton("Create account", s.busy, vm::register)
        HorizontalDivider()
        GoogleButton(!s.busy, vm::googleToken, vm::showError)
        TextButton(onClick = { nav.popBackStack() }, Modifier.heightIn(min = 48.dp)) { Text("I already have an account") }
    }
}

@Composable
fun ForgotPasswordScreen(nav: NavHostController, vm: AuthViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    AuthColumn {
        Text("Reset your password", style = MaterialTheme.typography.headlineMedium)
        Text("Enter your account email and we will send a reset link. The link opens Firebase's secure reset page in your browser; after setting a new password, return here and sign in.", style = MaterialTheme.typography.bodyMedium)
        FormField(s.email, { v -> vm.edit { copy(email = v) } }, "Email", error = s.errors["email"], keyboardType = KeyboardType.Email, enabled = !s.busy)
        Messages(s)
        BusyButton("Send reset link", s.busy, vm::sendReset)
        TextButton(onClick = { nav.popBackStack() }, Modifier.heightIn(min = 48.dp)) { Text("Back to sign in") }
    }
}

@Composable
fun VerifyEmailScreen(nav: NavHostController, authState: AuthState, vm: AuthViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val email = (authState as? AuthState.PendingVerification)?.email.orEmpty()
    AuthColumn {
        Text("Confirm your email", style = MaterialTheme.typography.headlineMedium)
        Text("We sent a confirmation link to $email. Open it, then tap the button below.", style = MaterialTheme.typography.bodyLarge)
        Messages(s)
        BusyButton("I've confirmed my email", s.busy, vm::refreshVerification)
        OutlinedButton(onClick = vm::resendVerification, enabled = !s.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Resend email") }
        TextButton(
            onClick = { vm.signOut(); nav.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) } },
            Modifier.heightIn(min = 48.dp),
        ) { Text("Use a different account") }
    }
}
