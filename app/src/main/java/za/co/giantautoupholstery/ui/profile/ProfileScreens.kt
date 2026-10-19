package za.co.giantautoupholstery.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import za.co.giantautoupholstery.core.initials
import za.co.giantautoupholstery.core.ui.*
import za.co.giantautoupholstery.domain.AuthState
import za.co.giantautoupholstery.ui.AppViewModel
import za.co.giantautoupholstery.ui.navigation.Routes

@Composable
fun ProfileScreen(nav: NavHostController, appVm: AppViewModel, authState: AuthState) {
    val profile by appVm.profile.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (authState is AuthState.SignedIn) {
            val name = profile?.fullName ?: authState.user.displayName
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Avatar(initials(name))
                Text(name.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                Text(authState.user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(Spacing.s))
            NavRow("Settings") { nav.navigate(Routes.SETTINGS) }
        } else {
            Text("You are browsing as a guest.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { nav.navigate(Routes.LOGIN) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Sign in") }
        }
        NavRow("Help") { nav.navigate(Routes.HELP) }
        NavRow("About") { nav.navigate(Routes.ABOUT) }
        NavRow("Contact") { nav.navigate(Routes.CONTACT) }
        NavRow("API & Cloud Sync") { nav.navigate(Routes.API_SYNC) }
        if (authState is AuthState.SignedIn) {
            Spacer(Modifier.height(Spacing.m))
            OutlinedButton(onClick = { confirmLogout = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Log out") }
        }
    }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false }, title = { Text("Log out?") }, text = { Text("You can sign back in at any time. Your saved bookings stay privately on this phone for your account.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; appVm.signOut() }) { Text("Log out") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Stay signed in") } },
    )
}

@Composable
fun NavRow(label: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

@Composable
fun SettingsScreen(authState: AuthState, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionNote by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionNote = if (granted) null else "Notification permission was denied. Your in-app inbox still works."
    }
    if (!s.loaded) { LoadingState(); return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionTitle("Your details")
        FormField(s.fullName, { v -> vm.edit { copy(fullName = v) } }, "Full name", error = s.errors["fullName"], enabled = !s.saving)
        FormField(s.phone, { v -> vm.edit { copy(phone = v) } }, "Phone (optional)", error = s.errors["phone"], keyboardType = KeyboardType.Phone, enabled = !s.saving)
        FormField(s.address, { v -> vm.edit { copy(address = v) } }, "Address (optional)", error = s.errors["address"], enabled = !s.saving)
        s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        BusyButton("Save details", s.saving, vm::saveDetails)

        Spacer(Modifier.height(Spacing.m))
        SectionTitle("Notifications")
        SwitchRow("Email notifications", "Preference saved. Email delivery is not configured in this version.", s.emailNotifications) { vm.savePreferences(email = it) }
        SwitchRow("Device notifications", "Shows alerts on this phone for booking updates. Not remote push.", s.pushNotifications) { on ->
            vm.savePreferences(push = on)
            if (on && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        Spacer(Modifier.height(Spacing.m))
        SectionTitle("Appearance")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                FilterChip(selected = s.themeMode == key, onClick = { vm.savePreferences(theme = key) }, label = { Text(label) })
            }
        }

        Spacer(Modifier.height(Spacing.m))
        SectionTitle("Password")
        if (authState is AuthState.SignedIn && !authState.user.hasPassword) {
            Text("You signed in with Google, so there is no password to change here.", style = MaterialTheme.typography.bodyMedium)
        } else {
            PasswordField(s.currentPassword, { v -> vm.edit { copy(currentPassword = v) } }, "Current password", error = s.passwordErrors["current"], enabled = !s.passwordBusy)
            PasswordField(s.newPassword, { v -> vm.edit { copy(newPassword = v) } }, "New password", error = s.passwordErrors["new"], enabled = !s.passwordBusy)
            PasswordField(s.confirmPassword, { v -> vm.edit { copy(confirmPassword = v) } }, "Confirm new password", error = s.passwordErrors["confirm"], enabled = !s.passwordBusy)
            s.passwordMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            BusyButton("Change password", s.passwordBusy, vm::changePassword)
        }

        if (vm.isDebug) {
            Spacer(Modifier.height(Spacing.l))
            SectionTitle("Developer tools (debug builds only)")
            Text("Inserts fictional demonstration rows so every table has at least 10 records. Simulated profiles cannot sign in.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = vm::runFixtures, enabled = !s.fixtureBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Run fixture setup") }
            OutlinedButton(onClick = vm::fixtureCounts, enabled = !s.fixtureBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Show table counts") }
            OutlinedButton(onClick = vm::cleanFixtures, enabled = !s.fixtureBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Clean up fixtures") }
            s.fixtureReport?.forEach { (t, n) -> Text("$t: $n ${if (n >= 10) "✓" else "✗"}", style = MaterialTheme.typography.bodyMedium) }
            s.fixtureMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
