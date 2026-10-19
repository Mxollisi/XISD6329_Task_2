package za.co.giantautoupholstery.ui.bookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import za.co.giantautoupholstery.core.Validation
import za.co.giantautoupholstery.core.formatDate
import za.co.giantautoupholstery.core.formatRand
import za.co.giantautoupholstery.core.ui.*
import za.co.giantautoupholstery.domain.Booking
import za.co.giantautoupholstery.domain.BookingStatus
import za.co.giantautoupholstery.ui.Load
import za.co.giantautoupholstery.ui.navigation.Routes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun BookingsScreen(nav: NavHostController, vm: BookingsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Upcoming (${ui.upcoming.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Past (${ui.past.size})") })
        }
        when {
            ui.loading -> LoadingState()
            ui.error -> ErrorState("Could not read your bookings.", vm::retry)
            else -> {
                val list = if (tab == 0) ui.upcoming else ui.past
                if (list.isEmpty()) {
                    EmptyState(
                        if (tab == 0) "No upcoming bookings" else "No past bookings",
                        if (tab == 0) "Request a booking from any service." else "Completed and cancelled bookings appear here.",
                    ) { if (tab == 0) Button(onClick = { nav.navigate(Routes.book(null)) }) { Text("New booking") } }
                } else LazyColumn(contentPadding = Spacing.screen, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    items(list, key = { it.id }) { b -> BookingCard(b) { nav.navigate(Routes.bookingDetail(b.id)) } }
                }
            }
        }
    }
}

@Composable
fun BookingCard(b: Booking, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(b.reference, style = MaterialTheme.typography.titleMedium)
                StatusChip(b.status)
            }
            Text(b.serviceName, style = MaterialTheme.typography.bodyLarge)
            Text(formatDate(b.preferredDate) + " at " + b.preferredTime, style = MaterialTheme.typography.bodyMedium)
            Text(b.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BookingDetailScreen(nav: NavHostController, vm: BookingDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var confirm by rememberSaveable { mutableStateOf(false) }
    when (val s = state) {
        Load.Loading -> LoadingState()
        Load.Missing -> EmptyState("Booking not found", "It may belong to another account or no longer exist.")
        is Load.Failed -> ErrorState(s.message, { nav.popBackStack() })
        is Load.Data -> {
            val b = s.value
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(b.reference, style = MaterialTheme.typography.headlineMedium); StatusChip(b.status)
                }
                Detail("Service", b.serviceName)
                Detail("Preferred date", formatDate(b.preferredDate))
                Detail("Preferred time", b.preferredTime)
                Detail("Address", b.address)
                Detail("Description", b.description ?: "None provided")
                Text("This request is saved on this phone. It has not been sent to or confirmed by the business.", style = MaterialTheme.typography.bodySmall)
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                if (b.status == BookingStatus.Pending) {
                    OutlinedButton(onClick = { confirm = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel booking") }
                }
                if (b.status == BookingStatus.Cancelled && message != null) {
                    TextButton(onClick = { nav.navigate(Routes.NOTIFICATIONS) }) { Text("View notifications") }
                }
            }
            if (confirm) ConfirmDialog("Cancel this booking?", "Booking ${b.reference} will be marked Cancelled. This cannot be undone.", "Cancel booking",
                onConfirm = { confirm = false; vm.cancel() }, onDismiss = { confirm = false })
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBookingScreen(nav: NavHostController, vm: BookingFormViewModel = hiltViewModel()) {
    val f by vm.form.collectAsStateWithLifecycle()
    val services by vm.services.collectAsStateWithLifecycle()
    var serviceMenu by remember { mutableStateOf(false) }
    var timeMenu by remember { mutableStateOf(false) }
    var showDate by rememberSaveable { mutableStateOf(false) }

    val created = f.created
    if (created != null) {
        Column(Modifier.fillMaxSize().padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Booking request saved", style = MaterialTheme.typography.headlineMedium)
            Text("Your reference", style = MaterialTheme.typography.labelLarge)
            Text(created.reference, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text("Saved on this phone as Pending. It has not been sent to the business, and the time is a preference, not guaranteed availability.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { nav.navigate(Routes.BOOKINGS) { popUpTo(Routes.HOME) } }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("View my bookings") }
            TextButton(onClick = { nav.popBackStack() }) { Text("Done") }
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        // Service selector
        val selected = services.firstOrNull { it.id == f.serviceId }
        ExposedDropdownMenuBox(expanded = serviceMenu, onExpandedChange = { serviceMenu = it }) {
            OutlinedTextField(
                value = selected?.let { it.name + " (from " + formatRand(it.priceCents) + ")" } ?: "", onValueChange = {}, readOnly = true,
                label = { Text("Service") }, isError = f.errors["service"] != null,
                supportingText = { f.errors["service"]?.let { Text(it) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(serviceMenu) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = serviceMenu, onDismissRequest = { serviceMenu = false }) {
                services.forEach { s ->
                    DropdownMenuItem(text = { Text(s.name + " – from " + formatRand(s.priceCents)) }, onClick = { vm.edit { copy(serviceId = s.id) }; serviceMenu = false })
                }
            }
        }
        // Date
        OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(Spacing.s))
            Text(f.date?.let { formatDate(it) } ?: "Choose preferred date")
        }
        f.errors["date"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        // Time
        ExposedDropdownMenuBox(expanded = timeMenu, onExpandedChange = { timeMenu = it }) {
            OutlinedTextField(
                value = f.time.orEmpty(), onValueChange = {}, readOnly = true, label = { Text("Preferred time") },
                isError = f.errors["time"] != null, supportingText = { (f.errors["time"] ?: "Hourly, 08:00–16:00. Not guaranteed availability.").let { Text(it) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(timeMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = timeMenu, onDismissRequest = { timeMenu = false }) {
                Validation.BOOKING_TIMES.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { vm.edit { copy(time = t) }; timeMenu = false }) }
            }
        }
        FormField(f.address, { v -> vm.edit { copy(address = v) } }, "Address", error = f.errors["address"], enabled = !f.submitting)
        FormField(f.description, { v -> vm.edit { copy(description = v) } }, "Description (optional)", error = f.errors["description"],
            singleLine = false, minLines = 3, enabled = !f.submitting, supporting = "${f.description.length}/500")
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(f.terms, role = Role.Checkbox, onValueChange = { v -> vm.edit { copy(terms = v) } }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = f.terms, onCheckedChange = null)
            Spacer(Modifier.width(Spacing.s))
            Text("I understand this is a booking request (not a confirmed appointment) and prices are estimates.", style = MaterialTheme.typography.bodyMedium)
        }
        f.errors["terms"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        f.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        BusyButton("Submit booking request", f.submitting, vm::submit)
    }

    if (showDate) {
        val earliest = vm.earliestDate
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = f.date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(earliest)
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val d: LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.edit { copy(date = d) }
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}
