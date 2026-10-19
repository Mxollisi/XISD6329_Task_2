package za.co.giantautoupholstery.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.giantautoupholstery.core.JOHANNESBURG
import za.co.giantautoupholstery.core.ui.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val stamp = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("en-ZA")).withZone(JOHANNESBURG)

@Composable
fun NotificationsScreen(vm: NotificationsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        message?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                    Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = vm::consumeMessage) { Text("Dismiss") }
                }
            }
        }
        when {
            ui.loading -> LoadingState()
            ui.error -> ErrorState("Could not read your notifications.", vm::retry)
            ui.items.isEmpty() -> EmptyState("No notifications", "Updates about your bookings will appear here.")
            else -> {
                val unread = ui.items.count { !it.isRead }
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.m), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("$unread unread", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = vm::markAllRead, enabled = unread > 0) { Text("Mark all read") }
                }
                LazyColumn(contentPadding = Spacing.screen, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    items(ui.items, key = { it.id }) { n ->
                        Card(Modifier.fillMaxWidth().clickable(enabled = !n.isRead) { vm.markRead(n.id) }) {
                            Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                if (!n.isRead) Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape).semantics { contentDescription = "Unread" })
                                Column(Modifier.weight(1f)) {
                                    Text(n.title, fontWeight = if (n.isRead) FontWeight.Normal else FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(n.body, style = MaterialTheme.typography.bodyMedium)
                                    Text(stamp.format(Instant.ofEpochMilli(n.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { vm.delete(n.id) }) { Icon(Icons.Filled.Delete, "Delete notification: ${n.title}") }
                            }
                        }
                    }
                }
            }
        }
    }
}
