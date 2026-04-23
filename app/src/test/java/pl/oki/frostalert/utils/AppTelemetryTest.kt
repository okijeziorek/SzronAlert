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

    // ── Security telemetry ────────────────────────────────────────────────────

    @Test
    fun `initial security counts are zero`() {
        assertEquals(0, AppTelemetry.getRootDetectedCount(context))
        assertEquals(0, AppTelemetry.getTamperingDetectedCount(context))
        assertEquals(0, AppTelemetry.getIntegrityFailureCount(context))
        assertEquals(0L, AppTelemetry.getLastSecurityEventMs(context))
    }

    @Test
    fun `recordSecurityEvent increments root counter for root_detected type`() {
        AppTelemetry.recordSecurityEvent(
            context,
            AppTelemetry.SecurityEvent(type = "root_detected", severity = "high", details = "su binary found")
        )
        assertEquals(1, AppTelemetry.getRootDetectedCount(context))
    }

    @Test
    fun `recordSecurityEvent increments tampering counter for hook types`() {
        AppTelemetry.recordSecurityEvent(
            context,
            AppTelemetry.SecurityEvent(type = "hook_detected", severity = "critical", details = "Xposed")
        )
        AppTelemetry.recordSecurityEvent(
            context,
            AppTelemetry.SecurityEvent(type = "frida_detected", severity = "critical", details = "port 27042")
        )
        assertEquals(2, AppTelemetry.getTamperingDetectedCount(context))
    }

    @Test
    fun `recordSecurityEvent increments integrity counter for integrity_failed type`() {
        AppTelemetry.recordSecurityEvent(
            context,
            AppTelemetry.SecurityEvent(type = "integrity_failed", severity = "critical", details = "verdict NOK")
        )
        AppTelemetry.recordSecurityEvent(
            context,
            AppTelemetry.SecurityEvent(type = "periodic_integrity_failed", severity = "critical", details = "details")
        )
        assertEquals(2, AppTelemetry.getIntegrityFailureCount(context))
    }

    @Test
    fun `recordSecurityEvent updates last security event timestamp`() {
        val before = System.currentTimeMillis()
        AppTelemetry.recordSecurityEvent(
            context,
            AppTelemetry.SecurityEvent(type = "root_detected", severity = "high", details = "test")
        )
        val after = System.currentTimeMillis()
        assertTrue(AppTelemetry.getLastSecurityEventMs(context) in before..after)
    }

    @Test
    fun `getRecentSecurityEvents returns recorded events in descending timestamp order`() {
        AppTelemetry.recordSecurityEvent(
            context,
            AppTelemetry.SecurityEvent(type = "root_detected", severity = "high", details = "first")
        )
        Thread.sleep(5) // ensure different timestamp
        AppTelemetry.recordSecurityEvent(
            context,
            AppTelemetry.SecurityEvent(type = "hook_detected", severity = "critical", details = "second")
        )

        val events = AppTelemetry.getRecentSecurityEvents(context)
        assertEquals(2, events.size)
        // Most recent first
        assertEquals("hook_detected", events[0].type)
        assertEquals("root_detected", events[1].type)
    }

    @Test
    fun `getRecentSecurityEvents correctly parses fields with pipe characters in details`() {
        val details = "Issues: App modified | Verdict: HOOKED"
        AppTelemetry.recordSecurityEvent(
            context,
            AppTelemetry.SecurityEvent(type = "periodic_integrity_failed", severity = "critical", details = details)
        )

        val events = AppTelemetry.getRecentSecurityEvents(context)
        assertEquals(1, events.size)
        assertEquals("periodic_integrity_failed", events[0].type)
        assertEquals("critical", events[0].severity)
        assertEquals(details, events[0].details)
    }

    @Test
    fun `getRecentSecurityEvents caps stored events at MAX_SECURITY_EVENTS`() {
        // Record 55 events (above the 50-event cap)
        repeat(55) { i ->
            AppTelemetry.recordSecurityEvent(
                context,
                AppTelemetry.SecurityEvent(type = "root_detected", severity = "high", details = "event $i")
            )
        }

        val events = AppTelemetry.getRecentSecurityEvents(context)
        assertTrue("Expected at most 50 events, got ${events.size}", events.size <= 50)
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
