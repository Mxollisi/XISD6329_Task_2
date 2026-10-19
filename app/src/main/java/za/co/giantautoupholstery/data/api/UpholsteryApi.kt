package za.co.giantautoupholstery.data.api

import kotlinx.coroutines.delay
import za.co.giantautoupholstery.core.safeCall
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standardized API response wrapper encapsulating HTTP status codes, status messages,
 * success flags, payload data, timestamps, and transport metadata.
 */
data class ApiResponse<T>(
    val statusCode: Int,        // e.g. 200, 201, 400, 500
    val statusText: String,     // e.g. "OK", "Created", "Bad Request", "Internal Server Error"
    val success: Boolean,
    val message: String,
    val data: T?,
    val timestamp: Long = System.currentTimeMillis(),
    val headers: Map<String, String> = emptyMap(),
)

data class ServiceDto(
    val id: String,
    val name: String,
    val category: String,
    val priceCents: Long,
    val duration: String,
    val description: String,
    val imageRef: String?,
)

data class FaqDto(
    val id: String,
    val question: String,
    val answer: String,
    val displayOrder: Int,
)

data class BookingSyncRequest(
    val uid: String,
    val submissions: List<BookingSyncItem>,
)

data class BookingSyncItem(
    val reference: String,
    val serviceId: String,
    val preferredDate: String,
    val preferredTime: String,
    val address: String,
    val status: String,
)

data class BookingSyncResponse(
    val syncedCount: Int,
    val failedCount: Int,
    val syncToken: String,
    val message: String,
)

data class SupportTicketRequest(
    val name: String,
    val email: String,
    val subject: String,
    val message: String,
    val priority: String = "Normal",
)

data class SupportTicketResponse(
    val ticketId: String,
    val status: String,
    val estimatedResponseHours: Int,
    val createdAt: Long,
)

/**
 * Fully asynchronous REST API client simulating live HTTP/REST endpoints with status codes,
 * JSON response wrapping, custom request headers, timeout handling, mock latency, and error simulation.
 */
@Singleton
class UpholsteryApiClient @Inject constructor(private val clock: Clock) {

    private val baseHeaders = mapOf(
        "Content-Type" to "application/json",
        "Accept" to "application/json",
        "User-Agent" to "GiantAutoUpholstery-Android/2.0",
        "X-Client-Version" to "2.0.0",
        "X-Environment" to "Production-Simulation",
    )

    suspend fun getServices(simulateError: Boolean = false): ApiResponse<List<ServiceDto>> {
        val startTime = System.currentTimeMillis()
        delay(350L) // Mock latency
        val requestId = UUID.randomUUID().toString()
        val headers = baseHeaders + mapOf("X-Request-Id" to requestId, "X-RateLimit-Remaining" to "98")

        if (simulateError) {
            return ApiResponse(
                statusCode = 500,
                statusText = "Internal Server Error",
                success = false,
                message = "Simulated cloud database connection failure while fetching catalog.",
                data = null,
                timestamp = clock.millis(),
                headers = headers,
            )
        }

        val services = listOf(
            ServiceDto("serv-1", "Full Leather Re-upholstery", "Leather", 450000L, "3 days", "Complete restoration of automotive leather interior with premium imported hides.", null),
            ServiceDto("serv-2", "Foam Cushion Replacement", "Sofa", 120000L, "1 day", "High-density polyurethane foam replacement for sagging car seats and cushions.", null),
            ServiceDto("serv-3", "Convertible Soft Top Repair", "Auto", 320000L, "2 days", "Specialist stitching, waterproofing, and window replacement for convertible tops.", null),
            ServiceDto("serv-4", "Custom Steering Wheel Wrap", "Leather", 85000L, "4 hours", "Hand-stitched genuine leather wrapping with custom contrast accent stitching.", null),
            ServiceDto("serv-5", "Headliner Replacement", "Auto", 210000L, "1 day", "Sagging roof lining replacement with factory-matched acoustic fabric.", null),
        )

        return ApiResponse(
            statusCode = 200,
            statusText = "OK",
            success = true,
            message = "Successfully retrieved ${services.size} upholstery services from cloud API.",
            data = services,
            timestamp = clock.millis(),
            headers = headers,
        )
    }

    suspend fun getFaqs(): ApiResponse<List<FaqDto>> {
        val startTime = System.currentTimeMillis()
        delay(250L)
        val requestId = UUID.randomUUID().toString()
        val headers = baseHeaders + mapOf("X-Request-Id" to requestId, "X-RateLimit-Remaining" to "99")

        val faqs = listOf(
            FaqDto("faq-1", "How long does a full leather upholstery job take?", "Typically 2 to 4 working days depending on vehicle complexity.", 1),
            FaqDto("faq-2", "Do you provide a warranty on workmanship?", "Yes, all our re-upholstery and stitching work carries a 2-year craftsmanship guarantee.", 2),
            FaqDto("faq-3", "Can I choose custom leather colors and stitching?", "Absolutely! We offer over 50 leather color swatches and various contrasting thread colors.", 3),
            FaqDto("faq-4", "Are mobile inspection services available?", "Yes, our technicians can inspect vehicle interiors on-site within Gauteng.", 4),
        )

        return ApiResponse(
            statusCode = 200,
            statusText = "OK",
            success = true,
            message = "Successfully retrieved FAQs from cloud API.",
            data = faqs,
            timestamp = clock.millis(),
            headers = headers,
        )
    }

    suspend fun syncBookings(request: BookingSyncRequest): ApiResponse<BookingSyncResponse> {
        val startTime = System.currentTimeMillis()
        delay(500L)
        val requestId = UUID.randomUUID().toString()
        val headers = baseHeaders + mapOf("X-Request-Id" to requestId, "Authorization" to "Bearer token-${request.uid.take(8)}")

        if (request.uid.isBlank() || request.submissions.isEmpty()) {
            return ApiResponse(
                statusCode = 400,
                statusText = "Bad Request",
                success = false,
                message = "Invalid synchronization payload: UID and at least one booking submission are required.",
                data = null,
                timestamp = clock.millis(),
                headers = headers,
            )
        }

        val syncToken = "sync-token-${UUID.randomUUID().toString().take(8)}"
        val response = BookingSyncResponse(
            syncedCount = request.submissions.size,
            failedCount = 0,
            syncToken = syncToken,
            message = "Successfully synchronized ${request.submissions.size} booking(s) with cloud server.",
        )

        return ApiResponse(
            statusCode = 201,
            statusText = "Created",
            success = true,
            message = "Cloud synchronization committed successfully.",
            data = response,
            timestamp = clock.millis(),
            headers = headers,
        )
    }

    suspend fun submitSupportTicket(request: SupportTicketRequest): ApiResponse<SupportTicketResponse> {
        val startTime = System.currentTimeMillis()
        delay(400L)
        val requestId = UUID.randomUUID().toString()
        val headers = baseHeaders + mapOf("X-Request-Id" to requestId)

        if (request.name.isBlank() || request.email.isBlank() || request.message.isBlank()) {
            return ApiResponse(
                statusCode = 400,
                statusText = "Bad Request",
                success = false,
                message = "Validation failed: Name, email, and message are mandatory for support tickets.",
                data = null,
                timestamp = clock.millis(),
                headers = headers,
            )
        }

        val ticketId = "TICKET-${(1000..9999).random()}"
        val response = SupportTicketResponse(
            ticketId = ticketId,
            status = "Open",
            estimatedResponseHours = 4,
            createdAt = clock.millis(),
        )

        return ApiResponse(
            statusCode = 201,
            statusText = "Created",
            success = true,
            message = "Support ticket $ticketId successfully submitted to customer support queue.",
            data = response,
            timestamp = clock.millis(),
            headers = headers,
        )
    }
}
