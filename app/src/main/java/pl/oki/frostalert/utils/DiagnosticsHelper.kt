package pl.oki.frostalert.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.flow.first
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticsHelper {

    suspend fun buildReport(
        context: Context,
        userDescription: String = "",
        db: FrostDatabase = FrostDatabase.getDatabase(context),
        settingsDataStore: SettingsDataStore = SettingsDataStore(context)
    ): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val now = dateFormat.format(Date())

        sb.appendLine(context.getString(R.string.diagnostics_title))
        sb.appendLine(context.getString(R.string.diagnostics_generated, now))
        sb.appendLine()

        if (userDescription.isNotBlank()) {
            sb.appendLine(context.getString(R.string.diagnostics_user_description_label))
            sb.appendLine(userDescription.trim())
            sb.appendLine()
        }

        // App info
        sb.appendLine(context.getString(R.string.diagnostics_app_section))
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            sb.appendLine(context.getString(R.string.diagnostics_version, pInfo.versionName, PackageInfoCompat.getLongVersionCode(pInfo)))
        } catch (_: PackageManager.NameNotFoundException) {
            sb.appendLine(context.getString(R.string.diagnostics_version_unknown))
        }
        sb.appendLine()

        // Device info
        sb.appendLine(context.getString(R.string.diagnostics_device_section))
        sb.appendLine(context.getString(R.string.diagnostics_device_model, "${Build.MANUFACTURER} ${Build.MODEL}"))
        sb.appendLine(context.getString(R.string.diagnostics_android_version, Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
        sb.appendLine()

        // Settings
        sb.appendLine(context.getString(R.string.diagnostics_settings_section))
        try {
            val prefs = settingsDataStore.userPreferencesFlow.first()
            val modeStr = if (prefs.appMode == 0)
                context.getString(R.string.diagnostics_mode_car)
            else
                context.getString(R.string.diagnostics_mode_garden)
            sb.appendLine(context.getString(R.string.diagnostics_mode, modeStr))
            sb.appendLine(context.getString(R.string.diagnostics_temp_threshold, "${prefs.tempThreshold}°C"))
            sb.appendLine(context.getString(R.string.diagnostics_humidity_threshold, prefs.humidityThreshold))
            sb.appendLine(context.getString(R.string.diagnostics_precip_threshold, "${prefs.precipitationThreshold}"))
            sb.appendLine(context.getString(R.string.diagnostics_sensitivity, prefs.sensitivity))
            sb.appendLine(context.getString(R.string.diagnostics_alert_time, prefs.alertStartHour, prefs.alertEndHour))
            sb.appendLine(context.getString(R.string.diagnostics_auto_mode, prefs.isAutoModeEnabled.toString()))
            sb.appendLine(context.getString(R.string.diagnostics_pro_forced, prefs.isProForced.toString()))
            sb.appendLine(context.getString(R.string.diagnostics_geofencing, prefs.isGeofencingEnabled.toString()))
            sb.appendLine(context.getString(R.string.diagnostics_trend_notif, prefs.isTrendChangeNotificationsEnabled.toString()))
            val locationStr = context.getString(R.string.diagnostics_manual_location, prefs.isManualLocationEnabled.toString()) +
                if (prefs.isManualLocationEnabled) context.getString(R.string.diagnostics_manual_location_name, prefs.manualLocationName) else ""
            sb.appendLine(locationStr)
            val unitStr = if (prefs.useFahrenheit) "°F" else "°C"
            sb.appendLine(context.getString(R.string.diagnostics_temp_unit, unitStr))
            val themeStr = when (prefs.theme) {
                0 -> context.getString(R.string.diagnostics_theme_light)
                1 -> context.getString(R.string.diagnostics_theme_dark)
                else -> context.getString(R.string.diagnostics_theme_system)
            }
            sb.appendLine(context.getString(R.string.diagnostics_theme, themeStr))
        } catch (e: Exception) {
            sb.appendLine(context.getString(R.string.diagnostics_read_error, e.message))
        }
        sb.appendLine()

        // DB stats + recent records
        sb.appendLine(context.getString(R.string.diagnostics_db_section))
        try {
            val allRecords = db.temperatureDao().getAllRecords()
            val riskCount = db.temperatureDao().getRiskCount()
            val feedbackCount = db.calibrationDao().getTotalFeedbackCount()
            sb.appendLine(context.getString(R.string.diagnostics_records_count, allRecords.size))
            sb.appendLine(context.getString(R.string.diagnostics_risk_count, riskCount))
            sb.appendLine(context.getString(R.string.diagnostics_calibration_count, feedbackCount))
            sb.appendLine()
            sb.appendLine(context.getString(R.string.diagnostics_last_5_records))
            val recent = allRecords.sortedByDescending { it.timestamp }.take(5)
            if (recent.isEmpty()) {
                sb.appendLine("  ${context.getString(R.string.diagnostics_no_data)}")
            } else {
                recent.forEach { record ->
                    val ts = dateFormat.format(Date(record.timestamp))
                    val risk = if (record.hasRisk) context.getString(R.string.diagnostics_risk) else context.getString(R.string.diagnostics_ok)
                    sb.appendLine("  $ts | ${record.minTemp}°C | $risk")
                }
            }
        } catch (e: Exception) {
            sb.appendLine(context.getString(R.string.diagnostics_db_error, e.message))
        }
        sb.appendLine()

        // Telemetry section
        sb.appendLine(context.getString(R.string.diagnostics_worker_section))
        val successCount = AppTelemetry.getWorkerSuccessCount(context)
        val retryCount = AppTelemetry.getWorkerRetryCount(context)
        val failureCount = AppTelemetry.getWorkerFailureCount(context)
        val lastRunMs = AppTelemetry.getLastWorkerRunMs(context)
        val lastError = AppTelemetry.getLastErrorMessage(context)
        sb.appendLine(context.getString(R.string.diagnostics_success_count, successCount))
        sb.appendLine(context.getString(R.string.diagnostics_retry_count, retryCount))
        sb.appendLine(context.getString(R.string.diagnostics_critical_errors, failureCount))
        val totalRuns = successCount + failureCount
        if (totalRuns > 0) {
            val successRate = (successCount.toDouble() / totalRuns * 100).toInt()
            sb.appendLine(context.getString(R.string.diagnostics_success_rate, successRate))
        }
        val lastRunStr = if (lastRunMs > 0L) dateFormat.format(Date(lastRunMs)) else context.getString(R.string.diagnostics_no_run_data)
        sb.appendLine(context.getString(R.string.diagnostics_last_run, lastRunStr))
        if (lastError != null) sb.appendLine(context.getString(R.string.diagnostics_last_error, lastError))
        sb.appendLine()

        sb.appendLine(context.getString(R.string.diagnostics_footer))
        return sb.toString()
    }
}
