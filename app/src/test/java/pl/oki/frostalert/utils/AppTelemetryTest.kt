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

    // ── Worker telemetry ──────────────────────────────────────────────────────

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

    // ── Geofence telemetry ────────────────────────────────────────────────────

    @Test
    fun `initial geofence counts are zero`() {
        assertEquals(0, AppTelemetry.getGeofenceTriggerCount(context))
        assertEquals(0, AppTelemetry.getGeofenceErrorCount(context))
        assertEquals(0L, AppTelemetry.getLastGeofenceTriggerMs(context))
    }

    @Test
    fun `recordGeofenceTrigger increments count and updates timestamp`() {
        val before = System.currentTimeMillis()
        AppTelemetry.recordGeofenceTrigger(context)
        AppTelemetry.recordGeofenceTrigger(context)
        val after = System.currentTimeMillis()

        assertEquals(2, AppTelemetry.getGeofenceTriggerCount(context))
        assertTrue(AppTelemetry.getLastGeofenceTriggerMs(context) in before..after)
    }

    @Test
    fun `recordGeofenceError increments error count`() {
        AppTelemetry.recordGeofenceError(context)
        assertEquals(1, AppTelemetry.getGeofenceErrorCount(context))
    }

    // ── Widget telemetry ──────────────────────────────────────────────────────

    @Test
    fun `initial widget counts are zero`() {
        assertEquals(0, AppTelemetry.getWidgetUpdateCount(context))
        assertEquals(0, AppTelemetry.getWidgetErrorCount(context))
        assertEquals(0L, AppTelemetry.getLastWidgetUpdateMs(context))
    }

    @Test
    fun `recordWidgetUpdate increments count and updates timestamp`() {
        val before = System.currentTimeMillis()
        AppTelemetry.recordWidgetUpdate(context)
        val after = System.currentTimeMillis()

        assertEquals(1, AppTelemetry.getWidgetUpdateCount(context))
        assertTrue(AppTelemetry.getLastWidgetUpdateMs(context) in before..after)
    }

    @Test
    fun `recordWidgetError increments error count`() {
        AppTelemetry.recordWidgetError(context)
        AppTelemetry.recordWidgetError(context)
        assertEquals(2, AppTelemetry.getWidgetErrorCount(context))
    }

    // ── Billing telemetry ─────────────────────────────────────────────────────

    @Test
    fun `initial billing counts are zero`() {
        assertEquals(0, AppTelemetry.getBillingPurchaseCount(context))
        assertEquals(0, AppTelemetry.getBillingRestoreCount(context))
        assertEquals(0, AppTelemetry.getBillingErrorCount(context))
        assertEquals(0L, AppTelemetry.getLastBillingEventMs(context))
    }

    @Test
    fun `recordBillingPurchase increments count and updates timestamp`() {
        val before = System.currentTimeMillis()
        AppTelemetry.recordBillingPurchase(context)
        val after = System.currentTimeMillis()

        assertEquals(1, AppTelemetry.getBillingPurchaseCount(context))
        assertTrue(AppTelemetry.getLastBillingEventMs(context) in before..after)
    }

    @Test
    fun `recordBillingRestore increments restore count`() {
        AppTelemetry.recordBillingRestore(context)
        assertEquals(1, AppTelemetry.getBillingRestoreCount(context))
    }

    @Test
    fun `recordBillingError increments error count`() {
        AppTelemetry.recordBillingError(context)
        assertEquals(1, AppTelemetry.getBillingErrorCount(context))
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Test
    fun `reset clears all telemetry`() {
        AppTelemetry.recordWorkerSuccess(context)
        AppTelemetry.recordWorkerRetry(context, "error")
        AppTelemetry.recordWorkerFailure(context, "fail")
        AppTelemetry.recordGeofenceTrigger(context)
        AppTelemetry.recordGeofenceError(context)
        AppTelemetry.recordWidgetUpdate(context)
        AppTelemetry.recordWidgetError(context)
        AppTelemetry.recordBillingPurchase(context)
        AppTelemetry.recordBillingRestore(context)
        AppTelemetry.recordBillingError(context)

        AppTelemetry.reset(context)

        assertEquals(0, AppTelemetry.getWorkerSuccessCount(context))
        assertEquals(0, AppTelemetry.getWorkerRetryCount(context))
        assertEquals(0, AppTelemetry.getWorkerFailureCount(context))
        assertEquals(0L, AppTelemetry.getLastWorkerRunMs(context))
        assertNull(AppTelemetry.getLastErrorMessage(context))
        assertEquals(0, AppTelemetry.getGeofenceTriggerCount(context))
        assertEquals(0, AppTelemetry.getGeofenceErrorCount(context))
        assertEquals(0, AppTelemetry.getWidgetUpdateCount(context))
        assertEquals(0, AppTelemetry.getWidgetErrorCount(context))
        assertEquals(0, AppTelemetry.getBillingPurchaseCount(context))
        assertEquals(0, AppTelemetry.getBillingRestoreCount(context))
        assertEquals(0, AppTelemetry.getBillingErrorCount(context))
    }
}
