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

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        /** Prefix used when constructing geofence request IDs. */
        private const val GEOFENCE_ID_PREFIX = "geofence"
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

                // Parse lat/lon from requestId format "geofence:<lat>:<lon>"
                val parsed = parseGeofenceId(requestId)
                if (parsed == null) {
                    Log.w(TAG, "Could not parse geofence requestId: $requestId")
                    return
                }
                val (lat, lon) = parsed

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
                            direction = null,
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

    /**
     * Parses a geofence request ID in the format "geofence:<lat>:<lon>" and returns (lat, lon).
     * Returns null if the format is invalid or coordinates cannot be parsed.
     */
    private fun parseGeofenceId(requestId: String): Pair<Double, Double>? {
        val parts = requestId.split(":")
        if (parts.size < 3 || parts[0] != GEOFENCE_ID_PREFIX) return null
        val lat = parts[1].toDoubleOrNull() ?: return null
        val lon = parts[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }
}

