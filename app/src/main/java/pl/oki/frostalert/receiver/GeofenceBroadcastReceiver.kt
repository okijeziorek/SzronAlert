package pl.oki.frostalert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.GeofenceRecord

/**
 * Parsed components of a geofence request ID.
 *
 * Supports two formats:
 * - `"geofence:<lat>:<lon>"` or `"geofence:<lat>:<lon>:<direction>"` (prefixed)
 * - `"<lat>:<lon>"` or `"<lat>:<lon>:<direction>"` (bare)
 */
data class ParsedGeofenceId(
    val latitude: Double,
    val longitude: Double,
    val direction: String?
)

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"

        /**
         * Parses a geofence request ID into its lat/lon/direction components.
         *
         * @return [ParsedGeofenceId] with extracted coordinates, or `null` if the
         *         [requestId] is blank, has too few parts, or contains non-numeric
         *         latitude/longitude values.
         */
        fun parseRequestId(requestId: String?): ParsedGeofenceId? {
            if (requestId.isNullOrBlank()) return null

            val parts = requestId.split(":")
            if (parts.size < 2) return null

            val hasPrefix = parts.firstOrNull() == "geofence"
            val latIndex = if (hasPrefix) 1 else 0
            val lonIndex = if (hasPrefix) 2 else 1
            val directionIndex = if (hasPrefix) 3 else 2

            // Must have at least lat + lon parts
            if (parts.size <= lonIndex) return null

            val lat = parts.getOrNull(latIndex)?.toDoubleOrNull() ?: return null
            val lon = parts.getOrNull(lonIndex)?.toDoubleOrNull() ?: return null
            val direction = parts.getOrNull(directionIndex)?.takeIf { it.isNotBlank() }

            return ParsedGeofenceId(lat, lon, direction)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Log.w(TAG, "GeofencingEvent null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.w(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofence = triggeringGeofences?.firstOrNull()
            if (geofence != null) {
                val parsed = parseRequestId(geofence.requestId)
                val lat = parsed?.latitude ?: 0.0
                val lon = parsed?.longitude ?: 0.0
                val direction = parsed?.direction

                val db = FrostDatabase.getDatabase(context)
                val geofenceDao = db.run { this.geofenceDao() }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Load user preferences so risk assessment uses their configured thresholds,
                        // reducing false-positive geofence alerts.
                        val prefs = pl.oki.frostalert.data.local.SettingsDataStore(context)
                            .userPreferencesFlow.first()

                        // Pobierz bieżące warunki pogodowe dla punktu
                        val weatherResult = pl.oki.frostalert.data.remote.OpenMeteoApi.getWeather(lat, lon)
                        var minTemp = 0.0
                        var hasRisk = false
                        var riskLevel = 0.0
                        var locationName: String? = null

                        if (weatherResult is pl.oki.frostalert.utils.AppResult.Success) {
                            val weather = weatherResult.data
                            minTemp = pl.oki.frostalert.utils.WeatherCalculations.getNightMinTemp(weather.hourly)
                            hasRisk = pl.oki.frostalert.utils.WeatherCalculations.hasFrostRisk(
                                temp = minTemp,
                                humidity = weather.current.humidity,
                                precip = weather.current.precipitation,
                                weatherCode = weather.current.weatherCode,
                                // 1.0°C is the auto-mode default threshold (matches SettingsDataStore default).
                                tempThreshold = if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                                humidityThreshold = prefs.humidityThreshold.toDouble(),
                                precipitationThreshold = prefs.precipitationThreshold,
                                sensitivity = prefs.sensitivity,
                                windSpeed = weather.current.windSpeed,
                                appMode = prefs.appMode
                            )
                            riskLevel = when {
                                hasRisk && minTemp < -5 -> 1.0
                                hasRisk && minTemp < 0 -> 0.7
                                hasRisk -> 0.5
                                minTemp < 2 -> 0.2
                                else -> 0.0
                            }
                            locationName = "${"%.2f".format(lat)}, ${"%.2f".format(lon)}"
                        }

                        val record = GeofenceRecord(
                            timestamp = System.currentTimeMillis(),
                            latitude = lat,
                            longitude = lon,
                            direction = direction,
                            minTemp = minTemp,
                            hasRisk = hasRisk,
                            riskLevel = riskLevel,
                            locationName = locationName
                        )
                        geofenceDao.insert(record)
                    } catch (e: Exception) {
                        Log.w(TAG, "DB insert failed: ${e.message}")
                    }
                }
            }
        }
    }
}
