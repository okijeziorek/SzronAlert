package pl.oki.frostalert.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight worker-health telemetry stored in SharedPreferences.
 * Tracks success, retry and failure counts for FrostCheckWorker so that
 * DiagnosticsHelper can surface reliability metrics during closed testing.
 */
object AppTelemetry {

    private const val PREFS_NAME = "frost_telemetry"
    private const val KEY_WORKER_SUCCESS_COUNT = "worker_success_count"
    private const val KEY_WORKER_FAILURE_COUNT = "worker_failure_count"
    private const val KEY_WORKER_RETRY_COUNT = "worker_retry_count"
    private const val KEY_LAST_WORKER_RUN_MS = "last_worker_run_ms"
    private const val KEY_LAST_ERROR_MESSAGE = "last_error_message"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    /** Resets all telemetry counters (used in tests and DebugScreen). */
    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
