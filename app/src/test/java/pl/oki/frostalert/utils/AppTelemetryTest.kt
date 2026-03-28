package pl.oki.frostalert.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppTelemetryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        AppTelemetry.reset(context)
    }

    @Test
    fun `initial state has all zero counts`() {
        assertEquals(0, AppTelemetry.getWorkerSuccessCount(context))
        assertEquals(0, AppTelemetry.getWorkerRetryCount(context))
        assertEquals(0, AppTelemetry.getWorkerFailureCount(context))
        assertEquals(0L, AppTelemetry.getLastWorkerRunMs(context))
        assertNull(AppTelemetry.getLastErrorMessage(context))
    }

    @Test
    fun `recordWorkerSuccess increments success count`() {
        AppTelemetry.recordWorkerSuccess(context)
        AppTelemetry.recordWorkerSuccess(context)
        assertEquals(2, AppTelemetry.getWorkerSuccessCount(context))
    }

    @Test
    fun `recordWorkerSuccess updates last run timestamp`() {
        val before = System.currentTimeMillis()
        AppTelemetry.recordWorkerSuccess(context)
        val after = System.currentTimeMillis()
        val recorded = AppTelemetry.getLastWorkerRunMs(context)
        assertTrue(recorded in before..after)
    }

    @Test
    fun `recordWorkerRetry increments retry count`() {
        AppTelemetry.recordWorkerRetry(context)
        AppTelemetry.recordWorkerRetry(context)
        AppTelemetry.recordWorkerRetry(context)
        assertEquals(3, AppTelemetry.getWorkerRetryCount(context))
    }

    @Test
    fun `recordWorkerRetry saves error message`() {
        AppTelemetry.recordWorkerRetry(context, "Network timeout")
        assertEquals("Network timeout", AppTelemetry.getLastErrorMessage(context))
    }

    @Test
    fun `recordWorkerRetry without error message does not overwrite existing error`() {
        AppTelemetry.recordWorkerRetry(context, "First error")
        AppTelemetry.recordWorkerRetry(context, null)
        // First error should still be present since null was passed
        assertEquals("First error", AppTelemetry.getLastErrorMessage(context))
    }

    @Test
    fun `recordWorkerFailure increments failure count`() {
        AppTelemetry.recordWorkerFailure(context)
        assertEquals(1, AppTelemetry.getWorkerFailureCount(context))
    }

    @Test
    fun `recordWorkerFailure saves error message`() {
        AppTelemetry.recordWorkerFailure(context, "Permanent failure")
        assertEquals("Permanent failure", AppTelemetry.getLastErrorMessage(context))
    }

    @Test
    fun `counters accumulate independently`() {
        repeat(3) { AppTelemetry.recordWorkerSuccess(context) }
        repeat(2) { AppTelemetry.recordWorkerRetry(context, "retry") }
        repeat(1) { AppTelemetry.recordWorkerFailure(context, "fail") }

        assertEquals(3, AppTelemetry.getWorkerSuccessCount(context))
        assertEquals(2, AppTelemetry.getWorkerRetryCount(context))
        assertEquals(1, AppTelemetry.getWorkerFailureCount(context))
    }

    @Test
    fun `reset clears all telemetry`() {
        AppTelemetry.recordWorkerSuccess(context)
        AppTelemetry.recordWorkerRetry(context, "error")
        AppTelemetry.recordWorkerFailure(context, "fail")

        AppTelemetry.reset(context)

        assertEquals(0, AppTelemetry.getWorkerSuccessCount(context))
        assertEquals(0, AppTelemetry.getWorkerRetryCount(context))
        assertEquals(0, AppTelemetry.getWorkerFailureCount(context))
        assertEquals(0L, AppTelemetry.getLastWorkerRunMs(context))
        assertNull(AppTelemetry.getLastErrorMessage(context))
    }
}
