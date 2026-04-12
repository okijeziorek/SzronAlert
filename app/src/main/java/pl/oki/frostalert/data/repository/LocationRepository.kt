package pl.oki.frostalert.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.data.remote.WeatherResponse
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.GeofenceRateLimiter
import pl.oki.frostalert.utils.WeatherCalculations
import kotlin.coroutines.resume

class LocationRepository(private val context: Context, private val settingsDataStore: SettingsDataStore) {

    companion object {
        private const val TAG = "LocationRepository"

        /** Maximum age of a cached GPS fix before requesting a fresh one (20 minutes). */
        private const val LOCATION_MAX_AGE_MS = 20 * 60 * 1000L

        // Approximate degree offsets for nearby-location checks (~20 km at mid-latitudes).
        /** Offset in degrees for cardinal directions (N/S/E/W ≈ 20 km). */
        private const val NEARBY_OFFSET_DEG = 0.18
        /** Offset in degrees for diagonal directions (≈ 20 km / √2 ≈ 14 km). */
        private const val NEARBY_DIAGONAL_OFFSET_DEG = 0.127

        /** Minimum risk-level increase (0–1 scale) to consider a nearby location "riskier". */
        private const val RISK_INCREASE_THRESHOLD = 0.3
    }

    suspend fun getEffectiveLocation(): Location? {
        val prefs = settingsDataStore.userPreferencesFlow.first()
        
        return if (prefs.isManualLocationEnabled) {
            val lat = prefs.manualLatitude
            val lon = prefs.manualLongitude
            if (!isValidCoordinate(lat, lon)) {
                Log.w(TAG, "Invalid manual coordinates: lat=$lat lon=$lon")
                null
            } else {
                Location("manual").apply {
                    latitude = lat
                    longitude = lon
                }
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
                if (location != null && (System.currentTimeMillis() - location.time) < LOCATION_MAX_AGE_MS) {
                    continuation.resume(location)
                } else {
                    val cts = CancellationTokenSource()
                    continuation.invokeOnCancellation { cts.cancel() }
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { freshLocation ->
                        continuation.resume(freshLocation)
                    }.addOnFailureListener {
                        cts.cancel()
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }
        }
    }

    /**
     * Sprawdza czy użytkownik wjechał w rejon z wyższym ryzykiem szronu.
     * Porównuje aktualną lokalizację z sąsiednimi punktami geograficznymi.
     */
    suspend fun checkGeofencingRisk(
        currentLocation: Location,
        userPrefs: pl.oki.frostalert.data.local.UserPreferences,
        currentWeather: WeatherResponse? = null
    ): GeofencingResult {
        if (!userPrefs.isGeofencingEnabled) {
            return GeofencingResult.NoRisk
        }

        if (!isValidCoordinate(currentLocation.latitude, currentLocation.longitude)) {
            return GeofencingResult.Error("Nieprawidłowe współrzędne lokalizacji")
        }

        // Fresh limiter for each trigger cycle — this is intentional: GeofenceRateLimiter
        // tracks calls within a single geofence trigger (max 8 API calls per trigger).
        val rateLimiter = GeofenceRateLimiter()

        // Compute risk for current location — reuse pre-fetched data when available
        val currentRiskData = if (currentWeather != null) {
            computeRiskFromWeather(currentWeather, currentLocation.latitude, currentLocation.longitude, userPrefs)
        } else {
            // Fallback: fetch weather (counts against rate limit)
            if (!rateLimiter.tryAcquire()) {
                return GeofencingResult.Error("Przekroczono limit zapytań API")
            }
            val currentRisk = getFrostRiskForLocation(currentLocation.latitude, currentLocation.longitude, userPrefs)
            if (currentRisk is AppResult.Error) {
                return GeofencingResult.Error("Błąd pobierania danych pogodowych")
            }
            (currentRisk as AppResult.Success).data
        }

        val d = NEARBY_OFFSET_DEG
        val dDiag = NEARBY_DIAGONAL_OFFSET_DEG
        val nearbyLocations = listOf(
            Pair(Location("").apply { latitude = currentLocation.latitude + d; longitude = currentLocation.longitude }, "północ"),
            Pair(Location("").apply { latitude = currentLocation.latitude - d; longitude = currentLocation.longitude }, "południe"),
            Pair(Location("").apply { latitude = currentLocation.latitude; longitude = currentLocation.longitude + d }, "wschód"),
            Pair(Location("").apply { latitude = currentLocation.latitude; longitude = currentLocation.longitude - d }, "zachód"),
            Pair(Location("").apply { latitude = currentLocation.latitude + dDiag; longitude = currentLocation.longitude + dDiag }, "północny-wschód"),
            Pair(Location("").apply { latitude = currentLocation.latitude - dDiag; longitude = currentLocation.longitude + dDiag }, "południowy-wschód"),
            Pair(Location("").apply { latitude = currentLocation.latitude + dDiag; longitude = currentLocation.longitude - dDiag }, "północny-zachód"),
            Pair(Location("").apply { latitude = currentLocation.latitude - dDiag; longitude = currentLocation.longitude - dDiag }, "południowy-zachód")
        )

        var maxNearbyRisk = 0.0
        var riskierDirection: String? = null

        for ((nearbyLoc, dir) in nearbyLocations) {
            if (!rateLimiter.tryAcquire()) break

            val nearbyRisk = getFrostRiskForLocation(nearbyLoc.latitude, nearbyLoc.longitude, userPrefs)
            if (nearbyRisk is AppResult.Success) {
                val nearbyRiskData = nearbyRisk.data
                if (nearbyRiskData.riskLevel > currentRiskData.riskLevel + RISK_INCREASE_THRESHOLD) {
                    if (nearbyRiskData.riskLevel > maxNearbyRisk) {
                        maxNearbyRisk = nearbyRiskData.riskLevel
                        riskierDirection = dir
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
     * Computes frost risk data from an already-fetched [WeatherResponse], avoiding
     * a redundant network call when the caller already has weather for this location.
     */
    private fun computeRiskFromWeather(
        weather: WeatherResponse,
        lat: Double,
        lon: Double,
        userPrefs: pl.oki.frostalert.data.local.UserPreferences
    ): FrostRiskData {
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
            windSpeed = weather.current.windSpeed,
            appMode = userPrefs.appMode
        )
        val riskLevel = WeatherCalculations.calculateRiskLevel(hasRisk, minTemp)
        return FrostRiskData(
            minTemp = minTemp,
            hasRisk = hasRisk,
            riskLevel = riskLevel,
            locationName = getLocationName(lat, lon)
        )
    }

    /**
     * Pobiera poziom ryzyka szronu dla konkretnej lokalizacji
     */
    private suspend fun getFrostRiskForLocation(lat: Double, lon: Double, userPrefs: pl.oki.frostalert.data.local.UserPreferences): AppResult<FrostRiskData> {
        return try {
            val weatherResult = OpenMeteoApi.getWeather(lat, lon)
            when (weatherResult) {
                is AppResult.Success -> {
                    AppResult.Success(computeRiskFromWeather(weatherResult.data, lat, lon, userPrefs))
                }
                is AppResult.Error -> weatherResult
            }
        } catch (e: Exception) {
            AppResult.Error(pl.oki.frostalert.utils.AppError.NetworkError("Błąd pobierania danych: ${e.message}"))
        }
    }

    private fun getLocationName(lat: Double, lon: Double): String {
        return "%.2f, %.2f".format(lat, lon)
    }

    /** Returns true if latitude is in [-90, 90] and longitude is in [-180, 180]. */
    private fun isValidCoordinate(lat: Double, lon: Double): Boolean {
        return lat in -90.0..90.0 && lon in -180.0..180.0
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
