package pl.oki.frostalert.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Thin wrapper around Firebase Analytics and Crashlytics.
 *
 * Call [init] once from [pl.oki.frostalert.FrostApplication.onCreate] after Firebase has been
 * initialized. Every method is safe to call before [init] — calls are silently ignored when
 * Firebase is not available (e.g. no credentials in local.properties during development).
 *
 * ## Tracked events (Faza 6 — Telemetria)
 * | Event name         | Trigger                                          |
 * |--------------------|--------------------------------------------------|
 * | `worker_success`   | FrostCheckWorker completed without error         |
 * | `widget_refresh`   | Both Glance and classic widgets updated          |
 * | `purchase_attempt` | User tapped "Buy PRO" (billing flow launched)    |
 * | `frost_alert_sent` | Push notification for frost risk sent to user    |
 */
object AnalyticsHelper {

    private const val TAG = "AnalyticsHelper"

    // Event names
    const val EVENT_WORKER_SUCCESS = "worker_success"
    const val EVENT_WIDGET_REFRESH = "widget_refresh"
    const val EVENT_PURCHASE_ATTEMPT = "purchase_attempt"
    const val EVENT_FROST_ALERT_SENT = "frost_alert_sent"
    const val EVENT_PURCHASE_COMPLETE = "purchase_complete"
    const val EVENT_BILLING_ERROR = "billing_error"
    const val EVENT_GEOFENCE_TRIGGER = "geofence_trigger"

    // Param keys
    private const val PARAM_APP_MODE = "app_mode"
    private const val PARAM_MIN_TEMP = "min_temp_rounded"
    private const val PARAM_PRODUCT_ID = "product_id"

    @Volatile private var analytics: FirebaseAnalytics? = null
    @Volatile private var crashlytics: FirebaseCrashlytics? = null

    /**
     * Must be called once from Application.onCreate() after [com.google.firebase.FirebaseApp]
     * has been initialized.
     */
    fun init(context: Context) {
        runCatching {
            analytics = FirebaseAnalytics.getInstance(context)
            crashlytics = FirebaseCrashlytics.getInstance()
            Log.d(TAG, "Firebase Analytics + Crashlytics initialized")
        }.onFailure {
            Log.w(TAG, "Firebase init failed — analytics disabled: ${it.message}")
        }
    }

    // ── Worker ────────────────────────────────────────────────────────────────

    /** Called when FrostCheckWorker completes successfully. */
    fun logWorkerSuccess() {
        logEvent(EVENT_WORKER_SUCCESS, null)
    }

    // ── Widget ────────────────────────────────────────────────────────────────

    /** Called when both widget update paths (Glance + classic) succeeded. */
    fun logWidgetRefresh() {
        logEvent(EVENT_WIDGET_REFRESH, null)
    }

    // ── Billing ───────────────────────────────────────────────────────────────

    /** Called when the user initiates a purchase flow (taps "Buy PRO"). */
    fun logPurchaseAttempt(productId: String) {
        val params = Bundle().apply {
            putString(PARAM_PRODUCT_ID, productId)
        }
        logEvent(EVENT_PURCHASE_ATTEMPT, params)
    }

    /** Called when a purchase is acknowledged and PRO access granted. */
    fun logPurchaseComplete(productId: String) {
        val params = Bundle().apply {
            putString(PARAM_PRODUCT_ID, productId)
        }
        logEvent(EVENT_PURCHASE_COMPLETE, params)
    }

    /** Called when a billing API error occurs. */
    fun logBillingError() {
        logEvent(EVENT_BILLING_ERROR, null)
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    /**
     * Called when a frost-risk push notification is sent to the user.
     *
     * @param appMode 0 = car mode, 1 = garden mode
     * @param minTemp Forecasted minimum temperature (rounded to nearest integer for privacy)
     */
    fun logFrostAlertSent(appMode: Int, minTemp: Double) {
        val params = Bundle().apply {
            putString(PARAM_APP_MODE, if (appMode == 1) "garden" else "car")
            putLong(PARAM_MIN_TEMP, minTemp.toLong())
        }
        logEvent(EVENT_FROST_ALERT_SENT, params)
    }

    // ── Geofence ──────────────────────────────────────────────────────────────

    /** Called when a geofence-based higher-risk nearby alert is triggered. */
    fun logGeofenceTrigger() {
        logEvent(EVENT_GEOFENCE_TRIGGER, null)
    }

    // ── Crashlytics ───────────────────────────────────────────────────────────

    /** Log a non-fatal exception to Crashlytics for monitoring. */
    fun recordNonFatalException(throwable: Throwable) {
        runCatching { crashlytics?.recordException(throwable) }
    }

    /** Attach a key-value string to subsequent Crashlytics reports. */
    fun setCustomKey(key: String, value: String) {
        runCatching { crashlytics?.setCustomKey(key, value) }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun logEvent(name: String, params: Bundle?) {
        runCatching {
            analytics?.logEvent(name, params)
        }.onFailure {
            Log.w(TAG, "Failed to log event '$name': ${it.message}")
        }
    }
}
