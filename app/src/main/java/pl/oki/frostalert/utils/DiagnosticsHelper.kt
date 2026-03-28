package pl.oki.frostalert.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.first
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

        sb.appendLine("=== FrostAlert Raport Diagnostyczny ===")
        sb.appendLine("Wygenerowano: $now")
        sb.appendLine()

        if (userDescription.isNotBlank()) {
            sb.appendLine("--- Opis problemu ---")
            sb.appendLine(userDescription.trim())
            sb.appendLine()
        }

        // App info
        sb.appendLine("--- Aplikacja ---")
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            sb.appendLine("Wersja: ${pInfo.versionName} (build ${pInfo.longVersionCode})")
        } catch (_: PackageManager.NameNotFoundException) {
            sb.appendLine("Wersja: nieznana")
        }
        sb.appendLine()

        // Device info
        sb.appendLine("--- Urządzenie ---")
        sb.appendLine("Model: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine()

        // Settings
        sb.appendLine("--- Ustawienia ---")
        try {
            val prefs = settingsDataStore.userPreferencesFlow.first()
            sb.appendLine("Tryb: ${if (prefs.appMode == 0) "Samochód" else "Ogród"}")
            sb.appendLine("Próg temp: ${prefs.tempThreshold}°C")
            sb.appendLine("Próg wilgotności: ${prefs.humidityThreshold}%")
            sb.appendLine("Próg opadów: ${prefs.precipitationThreshold} mm")
            sb.appendLine("Czułość: ${prefs.sensitivity}")
            sb.appendLine("Alert od-do: ${prefs.alertStartHour}:00–${prefs.alertEndHour}:00")
            sb.appendLine("Tryb auto: ${prefs.isAutoModeEnabled}")
            sb.appendLine("Tryb PRO (wymuszony): ${prefs.isProForced}")
            sb.appendLine("Geofencing: ${prefs.isGeofencingEnabled}")
            sb.appendLine("Trend powiadomienia: ${prefs.isTrendChangeNotificationsEnabled}")
            sb.appendLine("Lokalizacja ręczna: ${prefs.isManualLocationEnabled}" +
                    if (prefs.isManualLocationEnabled) " (${prefs.manualLocationName})" else "")
            sb.appendLine("Jednostka temp: ${if (prefs.useFahrenheit) "°F" else "°C"}")
            sb.appendLine("Motyw: ${when (prefs.theme) { 0 -> "Jasny"; 1 -> "Ciemny"; else -> "Systemowy" }}")
        } catch (e: Exception) {
            sb.appendLine("Błąd odczytu ustawień: ${e.message}")
        }
        sb.appendLine()

        // DB stats + recent records
        sb.appendLine("--- Baza Danych ---")
        try {
            val allRecords = db.temperatureDao().getAllRecords()
            val riskCount = db.temperatureDao().getRiskCount()
            val feedbackCount = db.calibrationDao().getTotalFeedbackCount()
            sb.appendLine("Rekordów temp: ${allRecords.size}")
            sb.appendLine("Ryzyk szronu: $riskCount")
            sb.appendLine("Feedbacków kalibracji: $feedbackCount")
            sb.appendLine()
            sb.appendLine("Ostatnie 5 rekordów:")
            val recent = allRecords.sortedByDescending { it.timestamp }.take(5)
            if (recent.isEmpty()) {
                sb.appendLine("  (brak danych)")
            } else {
                recent.forEach { record ->
                    val ts = dateFormat.format(Date(record.timestamp))
                    val risk = if (record.hasRisk) "RYZYKO" else "OK"
                    sb.appendLine("  $ts | ${record.minTemp}°C | $risk")
                }
            }
        } catch (e: Exception) {
            sb.appendLine("Błąd odczytu bazy: ${e.message}")
        }
        sb.appendLine()

        // Telemetry section
        sb.appendLine("--- Telemetria Worker ---")
        val successCount = AppTelemetry.getWorkerSuccessCount(context)
        val retryCount = AppTelemetry.getWorkerRetryCount(context)
        val failureCount = AppTelemetry.getWorkerFailureCount(context)
        val lastRunMs = AppTelemetry.getLastWorkerRunMs(context)
        val lastError = AppTelemetry.getLastErrorMessage(context)
        sb.appendLine("Sukcesy: $successCount")
        sb.appendLine("Ponowienia: $retryCount")
        sb.appendLine("Błędy krytyczne: $failureCount")
        val totalRuns = successCount + failureCount
        if (totalRuns > 0) {
            val successRate = (successCount.toDouble() / totalRuns * 100).toInt()
            sb.appendLine("Skuteczność: $successRate%")
        }
        sb.appendLine("Ostatnie uruchomienie: ${if (lastRunMs > 0L) dateFormat.format(Date(lastRunMs)) else "brak danych"}")
        if (lastError != null) sb.appendLine("Ostatni błąd: $lastError")
        sb.appendLine()

        sb.appendLine("=== Koniec raportu ===")
        return sb.toString()
    }
}
