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
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Log.w("GeofenceReceiver", "GeofencingEvent null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.w("GeofenceReceiver", "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofence = triggeringGeofences?.firstOrNull()
            if (geofence != null) {
                // W tym prostym podejściu zapisujemy tylko współrzędne geofencu
                val requestId = geofence.requestId
                // requestId może zawierać informacje o lat/lon jeśli zarejestrowano w tym formacie
                // Spróbujmy sparsować lat/lon z requestId jeśli dostępne: id = "lat:lon:dir" lub podobnie

                val parts = requestId.split(":")
                val hasPrefix = parts.firstOrNull() == "geofence"
                val latIndex = if (hasPrefix) 1 else 0
                val lonIndex = if (hasPrefix) 2 else 1
                val directionIndex = if (hasPrefix) 3 else 2

                val lat = parts.getOrNull(latIndex)?.toDoubleOrNull() ?: 0.0
                val lon = parts.getOrNull(lonIndex)?.toDoubleOrNull() ?: 0.0
                val direction = parts.getOrNull(directionIndex)

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
                        Log.w("GeofenceReceiver", "DB insert failed: ${e.message}")
                    }
                }
            }
        }
    }
}

