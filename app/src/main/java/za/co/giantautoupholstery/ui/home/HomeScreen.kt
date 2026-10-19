package za.co.giantautoupholstery.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import za.co.giantautoupholstery.core.firstName
import za.co.giantautoupholstery.core.formatDate
import za.co.giantautoupholstery.core.ui.*
import za.co.giantautoupholstery.domain.AuthState
import za.co.giantautoupholstery.ui.navigation.Routes

@Composable
fun HomeScreen(nav: NavHostController, authState: AuthState, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val signedIn = authState is AuthState.SignedIn
    LazyColumn(Modifier.fillMaxSize(), contentPadding = Spacing.screen, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (!signedIn) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Icon(Icons.Filled.Chair, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Giant Auto Upholstery", style = MaterialTheme.typography.headlineLarge)
                        Text("Upholstery repair and restoration for furniture, leather and vehicles. Browse our services and request a booking.", style = MaterialTheme.typography.bodyLarge)
                        if (authState is AuthState.PendingVerification) {
                            Button(onClick = { nav.navigate(Routes.VERIFY) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Finish confirming your email") }
                        } else {
                            Button(onClick = { nav.navigate(Routes.REGISTER) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Sign in / Get started") }
                        }
                        OutlinedButton(onClick = { nav.navigate(Routes.servicesRoute("")) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Browse services") }
                    }
                }
            }
            item { OutlinedButton(onClick = { nav.navigate(Routes.LOGIN) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("I already have an account") } }
        } else {
            item { Text("Hello, ${firstName(state.name)}", style = MaterialTheme.typography.headlineMedium) }
            item { SearchField(onSearch = { q -> nav.navigate(Routes.servicesRoute(q)) }) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        QuickAction("Book", Icons.Filled.EventAvailable, Modifier.weight(1f)) { nav.navigate(Routes.book(null)) }
                        QuickAction("Services", Icons.Filled.Chair, Modifier.weight(1f)) { nav.navigate(Routes.servicesRoute("")) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        QuickAction("My Bookings", Icons.Filled.Receipt, Modifier.weight(1f)) { nav.navigate(Routes.BOOKINGS) }
                        QuickAction("Help", Icons.Filled.Help, Modifier.weight(1f)) { nav.navigate(Routes.HELP) }
                    }
                }
            }
            state.latest?.let { b ->
                item {
                    SectionTitle("Latest booking")
                    Spacer(Modifier.height(Spacing.s))
                    Card(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.bookingDetail(b.id)) }) {
                        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(b.reference, style = MaterialTheme.typography.titleMedium)
                                StatusChip(b.status)
                            }
                            Text(b.serviceName)
                            Text(formatDate(b.preferredDate) + " at " + b.preferredTime, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item { SectionTitle("Featured services") }
        items(state.featured, key = { it.id }) { s -> ServiceCard(s, onClick = { nav.navigate(Routes.serviceDetail(s.id)) }) }
        item { Text("Prices are estimates \"from\" amounts and not a quotation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SearchField(onSearch: (String) -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = q, onValueChange = { q = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
        label = { Text("Search services") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = { IconButton(onClick = { onSearch(q) }) { Icon(Icons.Filled.ArrowForward, "Search") } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(q) }),
    )
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) {
        Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(Spacing.s)); Text(label)
    }
}
