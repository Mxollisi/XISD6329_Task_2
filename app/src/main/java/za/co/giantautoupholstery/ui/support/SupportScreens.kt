package za.co.giantautoupholstery.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.giantautoupholstery.core.BusinessInfo
import za.co.giantautoupholstery.core.ui.*
import za.co.giantautoupholstery.ui.Load

private val guide = listOf(
    "Browse services from the Services tab. You can do this without an account.",
    "Create an account (confirm your email) or choose Continue with Google.",
    "Open a service and tap Book this service, or use Book on the Home screen.",
    "Choose a date (tomorrow or later), an hourly time from 08:00 to 16:00, and confirm your address.",
    "Submit the request and note your reference number, for example GAU-ABC123.",
    "Find your request under Bookings. Pending requests can be cancelled from the booking details.",
    "Check the bell icon for your notifications inbox. Tap a notification to mark it read.",
    "Open Profile → Settings to change your details, notification preferences, theme and password.",
    "Use Contact to write to us. In this version your message is saved on this phone only.",
)

@Composable
fun HelpScreen(vm: HelpViewModel = hiltViewModel()) {
    val faqs by vm.faqs.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = Spacing.screen, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        item { SectionTitle("How to use the app") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                guide.forEachIndexed { i, step -> Text("${i + 1}. $step", style = MaterialTheme.typography.bodyLarge) }
            }
        }
        item { Spacer(Modifier.height(Spacing.s)); SectionTitle("Frequently asked questions") }
        when (val f = faqs) {
            Load.Loading -> item { LoadingState(Modifier.height(120.dp)) }
            is Load.Failed -> item { Text(f.message) }
            Load.Missing -> {}
            is Load.Data -> items(f.value, key = { it.id }) { faq ->
                var open by rememberSaveable(faq.id) { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth().clickable { open = !open }) {
                    Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(faq.question, style = MaterialTheme.typography.titleMedium)
                        if (open) Text(faq.answer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text("Giant Auto Upholstery", style = MaterialTheme.typography.headlineMedium)
        Text("Giant Auto Upholstery is a South African upholstery business. This app lets customers browse upholstery services, request bookings, track them on their phone and keep their preferences.", style = MaterialTheme.typography.bodyLarge)
        SectionTitle("What we do")
        Text("Furniture recovering and repair, leather repair and colour restoration, foam replacement, vehicle seat and roof-lining work, steering wheel wraps, and boat and caravan upholstery.", style = MaterialTheme.typography.bodyLarge)
        SectionTitle("Practical details")
        Text("Address: ${BusinessInfo.address ?: "To be confirmed by the business"}")
        Text("Operating hours: ${BusinessInfo.hours ?: "To be confirmed by the business"}")
        Text("Phone: ${BusinessInfo.phone ?: "To be confirmed by the business"}")
        Text("Email: ${BusinessInfo.email ?: "To be confirmed by the business"}")
        Text("Service prices in this version are demonstration catalogue values pending business confirmation.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ContactScreen(vm: ContactViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Send us a message", style = MaterialTheme.typography.headlineSmall)
        Text("In this version your message is saved on this phone only. It is not sent to the business.", style = MaterialTheme.typography.bodySmall)
        FormField(s.name, { v -> vm.edit { copy(name = v) } }, "Name", error = s.errors["name"], enabled = !s.submitting)
        FormField(s.email, { v -> vm.edit { copy(email = v) } }, "Email", error = s.errors["email"], keyboardType = KeyboardType.Email, enabled = !s.submitting)
        FormField(s.subject, { v -> vm.edit { copy(subject = v) } }, "Subject", error = s.errors["subject"], enabled = !s.submitting)
        FormField(s.message, { v -> vm.edit { copy(message = v) } }, "Message", error = s.errors["message"], singleLine = false, minLines = 4, enabled = !s.submitting, supporting = "${s.message.length}/1000")
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (s.saved) Text("Saved on this device.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
        BusyButton("Save message", s.submitting, vm::submit)

        // Only shown when genuine details are supplied in BusinessInfo.
        BusinessInfo.phone?.let { p -> OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$p"))) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Call $p") } }
        BusinessInfo.email?.let { e -> OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$e"))) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Email $e") } }
        BusinessInfo.address?.let { a -> OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(a)))) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Open map") } }
        if (BusinessInfo.phone == null && BusinessInfo.email == null && BusinessInfo.address == null)
            Text("Phone, email and map actions will appear once the business confirms its contact details.", style = MaterialTheme.typography.bodySmall)
    }
}
