package za.co.giantautoupholstery.ui.services

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import za.co.giantautoupholstery.core.formatRand
import za.co.giantautoupholstery.core.ui.*
import za.co.giantautoupholstery.domain.SERVICE_CATEGORIES
import za.co.giantautoupholstery.ui.Load
import za.co.giantautoupholstery.ui.navigation.Routes

@Composable
fun ServicesScreen(nav: NavHostController, vm: ServicesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { vm.query.value = it }, singleLine = true,
            label = { Text("Search by name or description") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            items(listOf("All") + SERVICE_CATEGORIES) { c ->
                FilterChip(selected = c == category, onClick = { vm.category.value = c }, label = { Text(c) })
            }
        }
        Spacer(Modifier.height(Spacing.s))
        when (val u = ui) {
            ServicesUi.Loading -> LoadingState()
            ServicesUi.Error -> ErrorState("Could not read services stored on this device.", vm::retry)
            is ServicesUi.Empty -> EmptyState(
                if (u.catalogueEmpty) "No services yet" else "No matching services",
                if (u.catalogueEmpty) "The local catalogue is empty." else "Try a different search or category.",
            )
            is ServicesUi.Content -> LazyColumn(contentPadding = Spacing.screen, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                items(u.items, key = { it.id }) { s -> ServiceCard(s, onClick = { nav.navigate(Routes.serviceDetail(s.id)) }) }
                item { Text("Demonstration catalogue. \"From\" prices are estimates, not quotations.", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun ServiceDetailScreen(nav: NavHostController, vm: ServiceDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        Load.Loading -> LoadingState()
        Load.Missing -> EmptyState("Service not found", "This service is no longer available.")
        is Load.Failed -> ErrorState(s.message, { nav.popBackStack() })
        is Load.Data -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            ServiceImage(s.value, Modifier.size(96.dp))
            Text(s.value.name, style = MaterialTheme.typography.headlineMedium)
            Text(s.value.category + " · Typical duration " + s.value.duration, style = MaterialTheme.typography.labelLarge)
            Text(s.value.description, style = MaterialTheme.typography.bodyLarge)
            Text("From " + formatRand(s.value.priceCents), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("Estimate only. The final price is not confirmed by this app.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { nav.navigate(Routes.book(s.value.id)) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Book this service") }
        }
    }
}
