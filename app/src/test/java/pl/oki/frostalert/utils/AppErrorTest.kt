package pl.oki.frostalert.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AppErrorTest {

    // ── AppError.fromThrowable ────────────────────────────────────────────────

    @Test
    fun `fromThrowable maps UnknownHostException to NoInternetError`() {
        val error = AppError.fromThrowable(UnknownHostException("host not found"))
        assertTrue("Should be NoInternetError", error is AppError.NoInternetError)
    }

    @Test
    fun `fromThrowable maps ConnectException to NoInternetError`() {
        val error = AppError.fromThrowable(ConnectException("connection refused"))
        assertTrue("Should be NoInternetError", error is AppError.NoInternetError)
    }

    @Test
    fun `fromThrowable maps SocketTimeoutException to TimeoutError`() {
        val error = AppError.fromThrowable(SocketTimeoutException("read timed out"))
        assertTrue("Should be TimeoutError", error is AppError.TimeoutError)
    }

    @Test
    fun `fromThrowable maps IOException to NetworkError`() {
        val error = AppError.fromThrowable(IOException("network failure"))
        assertTrue("Should be NetworkError", error is AppError.NetworkError)
        assertTrue("Message should contain original error text",
            error.message.contains("Błąd połączenia"))
    }

    @Test
    fun `fromThrowable maps SecurityException to LocationPermissionError`() {
        val error = AppError.fromThrowable(SecurityException("permission denied"))
        assertTrue("Should be LocationPermissionError", error is AppError.LocationPermissionError)
    }

    @Test
    fun `fromThrowable maps unknown exception to UnknownError`() {
        val error = AppError.fromThrowable(RuntimeException("something went wrong"))
        assertTrue("Should be UnknownError", error is AppError.UnknownError)
        assertTrue("Message should contain original error text",
            error.message.contains("something went wrong"))
    }

    @Test
    fun `fromThrowable preserves cause`() {
        val cause = RuntimeException("original cause")
        val error = AppError.fromThrowable(cause)
        assertEquals(cause, error.cause)
    }
}

class AppResultTest {

    // ── isSuccess / isError ───────────────────────────────────────────────────

    @Test
    fun `Success isSuccess returns true`() {
        val result: AppResult<Int> = AppResult.Success(42)
        assertTrue(result.isSuccess())
        assertFalse(result.isError())
    }

    @Test
    fun `Error isError returns true`() {
        val result: AppResult<Int> = AppResult.Error(AppError.UnknownError())
        assertTrue(result.isError())
        assertFalse(result.isSuccess())
    }

    // ── getOrNull ─────────────────────────────────────────────────────────────

    @Test
    fun `getOrNull returns data for Success`() {
        val result: AppResult<String> = AppResult.Success("hello")
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null for Error`() {
        val result: AppResult<String> = AppResult.Error(AppError.UnknownError())
        assertNull(result.getOrNull())
    }

    // ── getOrThrow ────────────────────────────────────────────────────────────

    @Test
    fun `getOrThrow returns data for Success`() {
        val result: AppResult<Int> = AppResult.Success(99)
        assertEquals(99, result.getOrThrow())
    }

    @Test(expected = RuntimeException::class)
    fun `getOrThrow throws for Error`() {
        val result: AppResult<Int> = AppResult.Error(AppError.UnknownError("failure"))
        result.getOrThrow()
    }

    // ── errorOrNull ───────────────────────────────────────────────────────────

    @Test
    fun `errorOrNull returns null for Success`() {
        val result: AppResult<Int> = AppResult.Success(1)
        assertNull(result.errorOrNull())
    }

    @Test
    fun `errorOrNull returns error for Error`() {
        val appError = AppError.NetworkError("timeout")
        val result: AppResult<Int> = AppResult.Error(appError)
        assertEquals(appError, result.errorOrNull())
    }

    // ── map ───────────────────────────────────────────────────────────────────

    @Test
    fun `map transforms Success data`() {
        val result: AppResult<Int> = AppResult.Success(5)
        val mapped = result.map { it * 2 }
        assertEquals(10, (mapped as AppResult.Success).data)
    }

    @Test
    fun `map passes through Error unchanged`() {
        val error = AppError.NetworkError("error")
        val result: AppResult<Int> = AppResult.Error(error)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is AppResult.Error)
        assertEquals(error, (mapped as AppResult.Error).error)
    }

    // ── onSuccess ─────────────────────────────────────────────────────────────

    @Test
    fun `onSuccess invokes action for Success`() {
        var invoked = false
        val result: AppResult<Int> = AppResult.Success(1)
        result.onSuccess { invoked = true }
        assertTrue(invoked)
    }

    @Test
    fun `onSuccess does not invoke action for Error`() {
        var invoked = false
        val result: AppResult<Int> = AppResult.Error(AppError.UnknownError())
        result.onSuccess { invoked = true }
        assertFalse(invoked)
    }

    // ── onError ───────────────────────────────────────────────────────────────

    @Test
    fun `onError invokes action for Error`() {
        var invoked = false
        val result: AppResult<Int> = AppResult.Error(AppError.UnknownError())
        result.onError { invoked = true }
        assertTrue(invoked)
    }

    @Test
    fun `onError does not invoke action for Success`() {
        var invoked = false
        val result: AppResult<Int> = AppResult.Success(1)
        result.onError { invoked = true }
        assertFalse(invoked)
    }

    // ── fold ──────────────────────────────────────────────────────────────────

    @Test
    fun `fold calls onSuccess for Success`() {
        val result: AppResult<Int> = AppResult.Success(42)
        val folded = result.fold(
            onSuccess = { "success:$it" },
            onError = { "error" }
        )
        assertEquals("success:42", folded)
    }

    @Test
    fun `fold calls onError for Error`() {
        val result: AppResult<Int> = AppResult.Error(AppError.NetworkError("down"))
        val folded = result.fold(
            onSuccess = { "success" },
            onError = { "error:${it.message}" }
        )
        assertEquals("error:down", folded)
    }

    // ── companion factory methods ──────────────────────────────────────────────

    @Test
    fun `success companion creates Success result`() {
        val result = AppResult.success(123)
        assertTrue(result is AppResult.Success)
        assertEquals(123, (result as AppResult.Success).data)
    }

    @Test
    fun `error companion with AppError creates Error result`() {
        val appError = AppError.DatabaseError("db failed")
        val result = AppResult.error(appError)
        assertTrue(result is AppResult.Error)
        assertEquals(appError, (result as AppResult.Error).error)
    }

    @Test
    fun `error companion with message creates UnknownError result`() {
        val result = AppResult.error("something went wrong")
        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is AppError.UnknownError)
        assertTrue(error.message.contains("something went wrong"))
    }

    // ── AppResult.onSuccess chains return self ──────────────────────────────────

    @Test
    fun `onSuccess returns original result for chaining`() {
        val original: AppResult<Int> = AppResult.Success(7)
        val returned = original.onSuccess { }
        assertTrue(returned === original)
    }

    @Test
    fun `onError returns original result for chaining`() {
        val original: AppResult<Int> = AppResult.Error(AppError.UnknownError())
        val returned = original.onError { }
        assertTrue(returned === original)
    }
}
