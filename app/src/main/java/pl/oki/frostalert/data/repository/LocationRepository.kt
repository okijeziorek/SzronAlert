package pl.oki.frostalert.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.WeatherCalculations
import kotlin.coroutines.resume
import kotlin.math.abs

class LocationRepository(private val context: Context, private val settingsDataStore: SettingsDataStore) {

    suspend fun getEffectiveLocation(): Location? {
        val prefs = settingsDataStore.userPreferencesFlow.first()
        
        return if (prefs.isManualLocationEnabled) {
            Location("manual").apply {
                latitude = prefs.manualLatitude
                longitude = prefs.manualLongitude
            }
        } else {
            getCurrentGpsLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentGpsLocation(): Location? {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                // Jeśli ostatnia lokalizacja jest świeża (np. młodsza niż 20 min), używamy jej
                if (location != null && (System.currentTimeMillis() - location.time) < 20 * 60 * 1000) {
                    continuation.resume(location)
                } else {
                    // W przeciwnym razie żądamy nowej, ale z balansem energii
                    val cts = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { freshLocation ->
                        continuation.resume(freshLocation)
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }
        }
    }

    /**
     * Sprawdza czy użytkownik wjechał w rejon z wyższym ryzykiem szronu
     * Porównuje aktualną lokalizację z sąsiednimi punktami geograficznymi
     */
    suspend fun checkGeofencingRisk(currentLocation: Location, userPrefs: pl.oki.frostalert.data.local.UserPreferences): GeofencingResult {
        if (!userPrefs.isGeofencingEnabled) {
            return GeofencingResult.NoRisk
        }

        // Sprawdź ryzyko w aktualnej lokalizacji
        val currentRisk = getFrostRiskForLocation(currentLocation.latitude, currentLocation.longitude, userPrefs)
        if (currentRisk is AppResult.Error) {
            return GeofencingResult.Error("Błąd pobierania danych pogodowych")
        }

        val currentRiskData = (currentRisk as AppResult.Success).data

        // Sprawdź ryzyko w sąsiednich lokalizacjach (w promieniu 10-50km)
        val nearbyLocations = listOf(
            // N (północ) - 20km
            Location("").apply {
                latitude = currentLocation.latitude + 0.18
                longitude = currentLocation.longitude
            },
            // S (południe) - 20km
            Location("").apply {
                latitude = currentLocation.latitude - 0.18
                longitude = currentLocation.longitude
            },
            // E (wschód) - 20km
            Location("").apply {
                latitude = currentLocation.latitude
                longitude = currentLocation.longitude + 0.18
            },
            // W (zachód) - 20km
            Location("").apply {
                latitude = currentLocation.latitude
                longitude = currentLocation.longitude - 0.18
            }
        )

        var maxNearbyRisk = 0.0
        var riskierDirection: String? = null

        for ((index, nearbyLoc) in nearbyLocations.withIndex()) {
            val nearbyRisk = getFrostRiskForLocation(nearbyLoc.latitude, nearbyLoc.longitude, userPrefs)
            if (nearbyRisk is AppResult.Success) {
                val nearbyRiskData = nearbyRisk.data
                if (nearbyRiskData.riskLevel > currentRiskData.riskLevel + 0.3) { // Co najmniej 30% wyższe ryzyko
                    if (nearbyRiskData.riskLevel > maxNearbyRisk) {
                        maxNearbyRisk = nearbyRiskData.riskLevel
                        riskierDirection = when (index) {
                            0 -> "północ"
                            1 -> "południe"
                            2 -> "wschód"
                            3 -> "zachód"
                            else -> "nieznany"
                        }
                    }
                }
            }
        }

        return if (riskierDirection != null) {
            GeofencingResult.HigherRiskNearby(
                direction = riskierDirection,
                riskIncrease = maxNearbyRisk - currentRiskData.riskLevel,
                currentRisk = currentRiskData
            )
        } else {
            GeofencingResult.NoRisk
        }
    }

    /**
     * Pobiera poziom ryzyka szronu dla konkretnej lokalizacji
     */
    private suspend fun getFrostRiskForLocation(lat: Double, lon: Double, userPrefs: pl.oki.frostalert.data.local.UserPreferences): AppResult<FrostRiskData> {
        return try {
            val weatherResult = OpenMeteoApi.getWeather(lat, lon)
            when (weatherResult) {
                is AppResult.Success -> {
                    val weather = weatherResult.data
                    val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                    val hasRisk = WeatherCalculations.hasFrostRisk(
                        temp = minTemp,
                        humidity = weather.current.humidity,
                        precip = weather.current.precipitation,
                        weatherCode = weather.current.weatherCode,
                        tempThreshold = if (userPrefs.isAutoModeEnabled) 1.0 else userPrefs.tempThreshold,
                        humidityThreshold = userPrefs.humidityThreshold.toDouble(),
                        precipitationThreshold = userPrefs.precipitationThreshold,
                        sensitivity = userPrefs.sensitivity,
                        windSpeed = 0.0, // Zakładamy brak wiatru dla uproszczenia
                        appMode = userPrefs.appMode
                    )

                    // Oblicz poziom ryzyka (0.0 - 1.0)
                    val riskLevel = when {
                        hasRisk && minTemp < -5 -> 1.0  // Bardzo wysokie ryzyko
                        hasRisk && minTemp < 0 -> 0.7   // Wysokie ryzyko
                        hasRisk -> 0.5                  // Umiarkowane ryzyko
                        minTemp < 2 -> 0.2              // Niskie ryzyko
                        else -> 0.0                     // Brak ryzyka
                    }

                    AppResult.Success(FrostRiskData(
                        minTemp = minTemp,
                        hasRisk = hasRisk,
                        riskLevel = riskLevel,
                        locationName = getLocationName(lat, lon)
                    ))
                }
                is AppResult.Error -> weatherResult
            }
        } catch (e: Exception) {
            AppResult.Error(pl.oki.frostalert.utils.AppError.NetworkError("Błąd pobierania danych: ${e.message}"))
        }
    }

    /**
     * Prosta funkcja do określenia nazwy lokalizacji (można rozszerzyć o reverse geocoding)
     */
    private fun getLocationName(lat: Double, lon: Double): String {
        return "%.2f, %.2f".format(lat, lon)
    }
}

/**
 * Wynik sprawdzenia geofencing
 */
sealed class GeofencingResult {
    data object NoRisk : GeofencingResult()
    data class HigherRiskNearby(
        val direction: String,
        val riskIncrease: Double,
        val currentRisk: FrostRiskData
    ) : GeofencingResult()
    data class Error(val message: String) : GeofencingResult()
}

/**
 * Dane ryzyka szronu dla lokalizacji
 */
data class FrostRiskData(
    val minTemp: Double,
    val hasRisk: Boolean,
    val riskLevel: Double, // 0.0 - 1.0
    val locationName: String
)
