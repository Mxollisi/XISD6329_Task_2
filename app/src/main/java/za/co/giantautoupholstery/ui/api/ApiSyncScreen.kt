package za.co.giantautoupholstery.ui.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import za.co.giantautoupholstery.core.ui.*
import za.co.giantautoupholstery.data.api.*
import za.co.giantautoupholstery.data.local.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class ApiSyncState(
    val isLoading: Boolean = false,
    val connectionStatus: String = "Connected (Mock Live)",
    val pingMs: Long = 38L,
    val syncLogs: List<SyncLogEntity> = emptyList(),
    val lastResponseSummary: String? = null,
    val message: String? = null,
    val servicesCount: Int = 0,
)

@HiltViewModel
class ApiSyncViewModel @Inject constructor(
    private val apiClient: UpholsteryApiClient,
    private val db: GiantDatabase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(ApiSyncState())
    val state: StateFlow<ApiSyncState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            db.syncLogDao().observeAll().collect { logs ->
                _state.update { it.copy(syncLogs = logs) }
            }
        }
    }

    private suspend fun recordLog(
        endpoint: String,
        method: String,
        statusCode: Int,
        statusText: String,
        reqPayload: String?,
        respPayload: String?,
        duration: Long,
        success: Boolean,
        error: String?,
    ) {
        val log = SyncLogEntity(
            id = UUID.randomUUID().toString(),
            endpoint = endpoint,
            httpMethod = method,
            statusCode = statusCode,
            statusText = statusText,
            requestPayload = reqPayload,
            responsePayload = respPayload,
            durationMs = duration,
            success = success,
            errorMessage = error,
            timestamp = clock.millis(),
        )
        db.syncLogDao().insert(log)
    }

    fun testGetServices(simulateError: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            val start = System.currentTimeMillis()
            val response = apiClient.getServices(simulateError = simulateError)
            val duration = System.currentTimeMillis() - start

            recordLog(
                endpoint = "/api/v1/services",
                method = "GET",
                statusCode = response.statusCode,
                statusText = response.statusText,
                reqPayload = null,
                respPayload = response.message + (response.data?.let { " (${it.size} services)" } ?: ""),
                duration = duration,
                success = response.success,
                error = if (response.success) null else response.message,
            )

            _state.update {
                it.copy(
                    isLoading = false,
                    lastResponseSummary = "GET /api/v1/services -> ${response.statusCode} ${response.statusText} (${duration}ms): ${response.message}",
                    message = response.message,
                    connectionStatus = if (response.success) "Connected (Mock Live)" else "Degraded",
                )
            }
        }
    }

    fun testGetFaqs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            val start = System.currentTimeMillis()
            val response = apiClient.getFaqs()
            val duration = System.currentTimeMillis() - start

            recordLog(
                endpoint = "/api/v1/faqs",
                method = "GET",
                statusCode = response.statusCode,
                statusText = response.statusText,
                reqPayload = null,
                respPayload = response.message + (response.data?.let { " (${it.size} FAQs)" } ?: ""),
                duration = duration,
                success = response.success,
                error = if (response.success) null else response.message,
            )

            _state.update {
                it.copy(
                    isLoading = false,
                    lastResponseSummary = "GET /api/v1/faqs -> ${response.statusCode} ${response.statusText} (${duration}ms): ${response.message}",
                    message = response.message,
                )
            }
        }
    }

    fun testSyncBookings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            val start = System.currentTimeMillis()
            val req = BookingSyncRequest(
                uid = "client-user-uid-12345",
                submissions = listOf(
                    BookingSyncItem("GAU-8923-01", "serv-1", "2025-05-15", "10:00", "123 Main Road, Sandton, JHB", "Pending"),
                    BookingSyncItem("GAU-8923-02", "serv-2", "2025-05-16", "14:00", "45 Oxford Road, Rosebank, JHB", "Pending"),
                )
            )
            val response = apiClient.syncBookings(req)
            val duration = System.currentTimeMillis() - start

            recordLog(
                endpoint = "/api/v1/bookings/sync",
                method = "POST",
                statusCode = response.statusCode,
                statusText = response.statusText,
                reqPayload = "Submissions: ${req.submissions.size} items",
                respPayload = response.message + (response.data?.let { " [token: ${it.syncToken}]" } ?: ""),
                duration = duration,
                success = response.success,
                error = if (response.success) null else response.message,
            )

            _state.update {
                it.copy(
                    isLoading = false,
                    lastResponseSummary = "POST /api/v1/bookings/sync -> ${response.statusCode} ${response.statusText} (${duration}ms): ${response.message}",
                    message = response.message,
                )
            }
        }
    }

    fun testSupportTicket() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            val start = System.currentTimeMillis()
            val req = SupportTicketRequest(
                name = "Thabo Mokoena",
                email = "thabo@example.co.za",
                subject = "Leather Re-upholstery Inquiry",
                message = "I would like to get a quote for re-upholstering a 3-piece vintage lounge suite in genuine oxblood leather.",
                priority = "High",
            )
            val response = apiClient.submitSupportTicket(req)
            val duration = System.currentTimeMillis() - start

            recordLog(
                endpoint = "/api/v1/support/tickets",
                method = "POST",
                statusCode = response.statusCode,
                statusText = response.statusText,
                reqPayload = "Subject: ${req.subject}, Priority: ${req.priority}",
                respPayload = response.message + (response.data?.let { " [Ticket: ${it.ticketId}]" } ?: ""),
                duration = duration,
                success = response.success,
                error = if (response.success) null else response.message,
            )

            _state.update {
                it.copy(
                    isLoading = false,
                    lastResponseSummary = "POST /api/v1/support/tickets -> ${response.statusCode} ${response.statusText} (${duration}ms): ${response.message}",
                    message = response.message,
                )
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            db.syncLogDao().clear()
            _state.update { it.copy(message = "API sync logs cleared successfully.") }
        }
    }
}

@Composable
fun ApiSyncScreen(nav: NavHostController, vm: ApiSyncViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        // Connection Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Box(Modifier.size(12.dp).background(if (state.connectionStatus.contains("Connected")) Color(0xFF2E7D32) else Color(0xFFD32F2F), CircleShape))
                    Text("API Status: ${state.connectionStatus}", style = MaterialTheme.typography.titleMedium)
                }
                Text("Base URL: https://api.giantautoupholstery.co.za/v1", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                Text("Simulated Ping: ${state.pingMs}ms · SSL TLS 1.3 · Bearer Token Active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Action Buttons Grid / Column
        Text("Test REST API Endpoints", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Button(
                onClick = vm::testGetServices,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
            ) { Text("GET Services", style = MaterialTheme.typography.labelMedium) }

            Button(
                onClick = vm::testGetFaqs,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
            ) { Text("GET FAQs", style = MaterialTheme.typography.labelMedium) }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Button(
                onClick = vm::testSyncBookings,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
            ) { Text("POST Sync Bookings", style = MaterialTheme.typography.labelMedium) }

            Button(
                onClick = vm::testSupportTicket,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
            ) { Text("POST Support Ticket", style = MaterialTheme.typography.labelMedium) }
        }

        OutlinedButton(
            onClick = { vm.testGetServices(simulateError = true) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        ) { Text("Simulate 500 Server Error Test") }

        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        state.lastResponseSummary?.let { summary ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Spacing.s)) {
                    Text("Last Response Payload", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(summary, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(Spacing.xs))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recent API Sync Logs (${state.syncLogs.size})", style = MaterialTheme.typography.titleMedium)
            if (state.syncLogs.isNotEmpty()) {
                TextButton(onClick = vm::clearLogs) { Text("Clear logs") }
            }
        }

        if (state.syncLogs.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No API sync logs recorded yet. Test an endpoint above.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                items(state.syncLogs, key = { it.id }) { log ->
                    SyncLogItem(log)
                }
            }
        }
    }
}

@Composable
fun SyncLogItem(log: SyncLogEntity) {
    val statusColor = when (log.statusCode) {
        200, 201 -> Color(0xFF2E7D32)
        400 -> Color(0xFFEF6C00)
        else -> Color(0xFFD32F2F)
    }
    val timeStr = remember(log.timestamp) {
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(log.timestamp))
    }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Spacing.s), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = if (log.httpMethod == "GET") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraSmall) {
                        Text(log.httpMethod, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(log.endpoint, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
                }
                Surface(color = statusColor.copy(alpha = 0.15f), contentColor = statusColor, shape = MaterialTheme.shapes.extraSmall) {
                    Text("${log.statusCode} ${log.statusText}", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            log.responsePayload?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$timeStr · ${log.durationMs}ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(if (log.success) "Success" else "Failed", style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
        }
    }
}
