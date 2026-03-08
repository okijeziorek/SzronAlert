package pl.oki.frostalert.utils

/**
 * Unified error handling pattern for the entire application
 */
sealed class AppError(open val message: String, open val cause: Throwable? = null) {

    // Network errors
    data class NetworkError(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class TimeoutError(override val message: String = "Przekroczono czas oczekiwania", override val cause: Throwable? = null) : AppError(message, cause)
    data class NoInternetError(override val message: String = "Brak połączenia internetowego", override val cause: Throwable? = null) : AppError(message, cause)

    // Location errors
    data class LocationError(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class LocationPermissionError(override val message: String = "Brak uprawnień do lokalizacji", override val cause: Throwable? = null) : AppError(message, cause)

    // API errors
    data class ApiError(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class InvalidApiResponseError(override val message: String = "Nieprawidłowa odpowiedź serwera", override val cause: Throwable? = null) : AppError(message, cause)

    // Database errors
    data class DatabaseError(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)

    // Validation errors
    data class ValidationError(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)

    // Generic errors
    data class UnknownError(override val message: String = "Wystąpił nieznany błąd", override val cause: Throwable? = null) : AppError(message, cause)

    companion object {
        fun fromThrowable(throwable: Throwable): AppError {
            return when (throwable) {
                is java.net.UnknownHostException,
                is java.net.ConnectException -> NoInternetError(cause = throwable)
                is java.net.SocketTimeoutException -> TimeoutError(cause = throwable)
                is java.io.IOException -> NetworkError("Błąd połączenia: ${throwable.message}", throwable)
                is SecurityException -> LocationPermissionError(cause = throwable)
                else -> UnknownError("Błąd: ${throwable.message ?: "Nieznany błąd"}", throwable)
            }
        }
    }
}

/**
 * Result wrapper for operations that can fail
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw RuntimeException(error.message, error.cause)
    }

    fun errorOrNull(): AppError? = when (this) {
        is Success -> null
        is Error -> error
    }

    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)
        fun error(error: AppError): AppResult<Nothing> = Error(error)
        fun error(message: String, cause: Throwable? = null): AppResult<Nothing> = Error(AppError.UnknownError(message, cause))
    }
}

/**
 * Extension functions for easier error handling
 */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> {
    return when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Error -> this
    }
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) {
        action(data)
    }
    return this
}

inline fun <T> AppResult<T>.onError(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) {
        action(error)
    }
    return this
}

inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onError: (AppError) -> R
): R {
    return when (this) {
        is AppResult.Success -> onSuccess(data)
        is AppResult.Error -> onError(error)
    }
}
