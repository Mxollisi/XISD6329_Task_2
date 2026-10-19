package za.co.giantautoupholstery.core

import kotlinx.coroutines.CancellationException

class ValidationException(val errors: Map<String, String>) : Exception(errors.values.firstOrNull() ?: "Invalid input")
class DomainException(message: String) : Exception(message)

/** runCatching that never swallows coroutine cancellation. */
inline fun <T> safeCall(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

fun Throwable.userMessage(): String = when (this) {
    is ValidationException, is DomainException -> message ?: "Something went wrong"
    is android.database.sqlite.SQLiteException -> "A storage error occurred on this device. Please try again."
    else -> "Something went wrong. Please try again."
}
