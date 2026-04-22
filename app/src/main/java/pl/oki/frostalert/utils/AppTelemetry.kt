package pl.oki.frostalert.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight worker-health telemetry stored in SharedPreferences.
 * Tracks success, retry and failure counts for FrostCheckWorker so that
 * DiagnosticsHelper can surface reliability metrics during closed testing.
 *
 * Extended in Phase 5 with geofence, widget, and billing event tracking.
 */
object AppTelemetry {

    private const val PREFS_NAME = "frost_telemetry"

    // Worker counters
    private const val KEY_WORKER_SUCCESS_COUNT = "worker_success_count"
    private const val KEY_WORKER_FAILURE_COUNT = "worker_failure_count"
    private const val KEY_WORKER_RETRY_COUNT = "worker_retry_count"
    private const val KEY_LAST_WORKER_RUN_MS = "last_worker_run_ms"
    private const val KEY_LAST_ERROR_MESSAGE = "last_error_message"

    // Geofence counters
    private const val KEY_GEOFENCE_TRIGGER_COUNT = "geofence_trigger_count"
    private const val KEY_GEOFENCE_ERROR_COUNT = "geofence_error_count"
    private const val KEY_LAST_GEOFENCE_TRIGGER_MS = "last_geofence_trigger_ms"

    // Widget counters
    private const val KEY_WIDGET_UPDATE_COUNT = "widget_update_count"
    private const val KEY_WIDGET_ERROR_COUNT = "widget_error_count"
    private const val KEY_LAST_WIDGET_UPDATE_MS = "last_widget_update_ms"

    // Billing counters
    private const val KEY_BILLING_PURCHASE_COUNT = "billing_purchase_count"
    private const val KEY_BILLING_RESTORE_COUNT = "billing_restore_count"
    private const val KEY_BILLING_ERROR_COUNT = "billing_error_count"
    private const val KEY_LAST_BILLING_EVENT_MS = "last_billing_event_ms"

    // Morning Brief counters
    private const val KEY_MORNING_BRIEF_SUCCESS_COUNT = "morning_brief_success_count"
    private const val KEY_MORNING_BRIEF_FAILURE_COUNT = "morning_brief_failure_count"
    private const val KEY_MORNING_BRIEF_RETRY_COUNT = "morning_brief_retry_count"
    private const val KEY_LAST_MORNING_BRIEF_MS = "last_morning_brief_ms"
    private const val KEY_LAST_MORNING_BRIEF_ERROR = "last_morning_brief_error"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Worker telemetry ──────────────────────────────────────────────────────

    fun recordWorkerSuccess(context: Context) {
        prefs(context).edit()
            .putInt(KEY_WORKER_SUCCESS_COUNT, getWorkerSuccessCount(context) + 1)
            .putLong(KEY_LAST_WORKER_RUN_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordWorkerRetry(context: Context, errorMessage: String? = null) {
        val edit = prefs(context).edit()
            .putInt(KEY_WORKER_RETRY_COUNT, getWorkerRetryCount(context) + 1)
        if (errorMessage != null) edit.putString(KEY_LAST_ERROR_MESSAGE, errorMessage)
        edit.apply()
    }

    fun recordWorkerFailure(context: Context, errorMessage: String? = null) {
        val edit = prefs(context).edit()
            .putInt(KEY_WORKER_FAILURE_COUNT, getWorkerFailureCount(context) + 1)
        if (errorMessage != null) edit.putString(KEY_LAST_ERROR_MESSAGE, errorMessage)
        edit.apply()
    }

    fun getWorkerSuccessCount(context: Context): Int =
        prefs(context).getInt(KEY_WORKER_SUCCESS_COUNT, 0)

    fun getWorkerRetryCount(context: Context): Int =
        prefs(context).getInt(KEY_WORKER_RETRY_COUNT, 0)

    fun getWorkerFailureCount(context: Context): Int =
        prefs(context).getInt(KEY_WORKER_FAILURE_COUNT, 0)

    fun getLastWorkerRunMs(context: Context): Long =
        prefs(context).getLong(KEY_LAST_WORKER_RUN_MS, 0L)

    fun getLastErrorMessage(context: Context): String? =
        prefs(context).getString(KEY_LAST_ERROR_MESSAGE, null)

    // ── Geofence telemetry ────────────────────────────────────────────────────

    fun recordGeofenceTrigger(context: Context) {
        prefs(context).edit()
            .putInt(KEY_GEOFENCE_TRIGGER_COUNT, getGeofenceTriggerCount(context) + 1)
            .putLong(KEY_LAST_GEOFENCE_TRIGGER_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordGeofenceError(context: Context) {
        prefs(context).edit()
            .putInt(KEY_GEOFENCE_ERROR_COUNT, getGeofenceErrorCount(context) + 1)
            .apply()
    }

    fun getGeofenceTriggerCount(context: Context): Int =
        prefs(context).getInt(KEY_GEOFENCE_TRIGGER_COUNT, 0)

    fun getGeofenceErrorCount(context: Context): Int =
        prefs(context).getInt(KEY_GEOFENCE_ERROR_COUNT, 0)

    fun getLastGeofenceTriggerMs(context: Context): Long =
        prefs(context).getLong(KEY_LAST_GEOFENCE_TRIGGER_MS, 0L)

    // ── Widget telemetry ──────────────────────────────────────────────────────

    fun recordWidgetUpdate(context: Context) {
        prefs(context).edit()
            .putInt(KEY_WIDGET_UPDATE_COUNT, getWidgetUpdateCount(context) + 1)
            .putLong(KEY_LAST_WIDGET_UPDATE_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordWidgetError(context: Context) {
        prefs(context).edit()
            .putInt(KEY_WIDGET_ERROR_COUNT, getWidgetErrorCount(context) + 1)
            .apply()
    }

    fun getWidgetUpdateCount(context: Context): Int =
        prefs(context).getInt(KEY_WIDGET_UPDATE_COUNT, 0)

    fun getWidgetErrorCount(context: Context): Int =
        prefs(context).getInt(KEY_WIDGET_ERROR_COUNT, 0)

    fun getLastWidgetUpdateMs(context: Context): Long =
        prefs(context).getLong(KEY_LAST_WIDGET_UPDATE_MS, 0L)

    // ── Billing telemetry ─────────────────────────────────────────────────────

    fun recordBillingPurchase(context: Context) {
        prefs(context).edit()
            .putInt(KEY_BILLING_PURCHASE_COUNT, getBillingPurchaseCount(context) + 1)
            .putLong(KEY_LAST_BILLING_EVENT_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordBillingRestore(context: Context) {
        prefs(context).edit()
            .putInt(KEY_BILLING_RESTORE_COUNT, getBillingRestoreCount(context) + 1)
            .putLong(KEY_LAST_BILLING_EVENT_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordBillingError(context: Context) {
        prefs(context).edit()
            .putInt(KEY_BILLING_ERROR_COUNT, getBillingErrorCount(context) + 1)
            .putLong(KEY_LAST_BILLING_EVENT_MS, System.currentTimeMillis())
            .apply()
    }

    fun getBillingPurchaseCount(context: Context): Int =
        prefs(context).getInt(KEY_BILLING_PURCHASE_COUNT, 0)

    fun getBillingRestoreCount(context: Context): Int =
        prefs(context).getInt(KEY_BILLING_RESTORE_COUNT, 0)

    fun getBillingErrorCount(context: Context): Int =
        prefs(context).getInt(KEY_BILLING_ERROR_COUNT, 0)

    fun getLastBillingEventMs(context: Context): Long =
        prefs(context).getLong(KEY_LAST_BILLING_EVENT_MS, 0L)

    // ── Reset ─────────────────────────────────────────────────────────────────

    /** Resets all telemetry counters (used in tests and DebugScreen). */
    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // ── Morning Brief telemetry ───────────────────────────────────────────────

    fun recordMorningBriefSuccess(context: Context) {
        prefs(context).edit()
            .putInt(KEY_MORNING_BRIEF_SUCCESS_COUNT, getMorningBriefSuccessCount(context) + 1)
            .putLong(KEY_LAST_MORNING_BRIEF_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordMorningBriefRetry(context: Context, errorMessage: String? = null) {
        val edit = prefs(context).edit()
            .putInt(KEY_MORNING_BRIEF_RETRY_COUNT, getMorningBriefRetryCount(context) + 1)
        if (errorMessage != null) edit.putString(KEY_LAST_MORNING_BRIEF_ERROR, errorMessage)
        edit.apply()
    }

    fun recordMorningBriefFailure(context: Context, errorMessage: String? = null) {
        val edit = prefs(context).edit()
            .putInt(KEY_MORNING_BRIEF_FAILURE_COUNT, getMorningBriefFailureCount(context) + 1)
        if (errorMessage != null) edit.putString(KEY_LAST_MORNING_BRIEF_ERROR, errorMessage)
        edit.apply()
    }

    fun getMorningBriefSuccessCount(context: Context): Int =
        prefs(context).getInt(KEY_MORNING_BRIEF_SUCCESS_COUNT, 0)

    fun getMorningBriefRetryCount(context: Context): Int =
        prefs(context).getInt(KEY_MORNING_BRIEF_RETRY_COUNT, 0)

    fun getMorningBriefFailureCount(context: Context): Int =
        prefs(context).getInt(KEY_MORNING_BRIEF_FAILURE_COUNT, 0)

    fun getLastMorningBriefMs(context: Context): Long =
        prefs(context).getLong(KEY_LAST_MORNING_BRIEF_MS, 0L)

    fun getLastMorningBriefError(context: Context): String? =
        prefs(context).getString(KEY_LAST_MORNING_BRIEF_ERROR, null)
}
