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
        /** Prefix used when constructing geofence request IDs. */
        private const val GEOFENCE_ID_PREFIX = "geofence"

        /**
         * Parses a geofence request ID in two supported formats:
         * - Prefixed: `"geofence:<lat>:<lon>"` or `"geofence:<lat>:<lon>:<direction>"`
         * - Bare: `"<lat>:<lon>"` or `"<lat>:<lon>:<direction>"`
         *
         * @return [ParsedGeofenceId] on success, or `null` if the input is null/blank/malformed.
         */
        fun parseRequestId(requestId: String?): ParsedGeofenceId? {
            if (requestId.isNullOrBlank()) return null
            val parts = requestId.split(":")
            if (parts.isEmpty()) return null
            val lat: Double
            val lon: Double
            val direction: String?
            if (parts[0] == GEOFENCE_ID_PREFIX) {
                // Prefixed format: ["geofence", lat, lon, direction?]
                if (parts.size < 3) return null
                lat = parts[1].toDoubleOrNull() ?: return null
                lon = parts[2].toDoubleOrNull() ?: return null
                direction = parts.getOrNull(3)?.takeIf { it.isNotBlank() }
            } else {
                // Bare format: [lat, lon, direction?]
                if (parts.size < 2) return null
                lat = parts[0].toDoubleOrNull() ?: return null
                lon = parts[1].toDoubleOrNull() ?: return null
                direction = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
            }
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
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
                val requestId = geofence.requestId

                // Parse lat/lon (and optional direction) from requestId
                val parsed = parseRequestId(requestId)
                if (parsed == null) {
                    Log.w(TAG, "Could not parse geofence requestId: $requestId")
                    return
                }
                val lat = parsed.latitude
                val lon = parsed.longitude

                // goAsync() keeps the receiver alive while the coroutine runs.
                val pendingResult = goAsync()
                val db = FrostDatabase.getDatabase(context)
                val geofenceDao = db.geofenceDao()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefs = pl.oki.frostalert.data.local.SettingsDataStore(context)
                            .userPreferencesFlow.first()

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
                                tempThreshold = if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                                humidityThreshold = prefs.humidityThreshold.toDouble(),
                                precipitationThreshold = prefs.precipitationThreshold,
                                sensitivity = prefs.sensitivity,
                                windSpeed = weather.current.windSpeed,
                                appMode = prefs.appMode
                            )
                            riskLevel = pl.oki.frostalert.utils.WeatherCalculations.calculateRiskLevel(hasRisk, minTemp)
                            locationName = "${"%.2f".format(lat)}, ${"%.2f".format(lon)}"
                        }

                        val record = GeofenceRecord(
                            timestamp = System.currentTimeMillis(),
                            latitude = lat,
                            longitude = lon,
                            direction = parsed.direction,
                            minTemp = minTemp,
                            hasRisk = hasRisk,
                            riskLevel = riskLevel,
                            locationName = locationName
                        )
                        geofenceDao.insert(record)
                    } catch (e: Exception) {
                        Log.w(TAG, "DB insert failed: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

}
