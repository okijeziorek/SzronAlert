package pl.oki.frostalert

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.oki.frostalert.data.OpenMeteoApi
import pl.oki.frostalert.data.SettingsDataStore
import pl.oki.frostalert.utils.NotificationHelper
import pl.oki.frostalert.utils.WeatherCalculations
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FrostCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val isTest = inputData.getBoolean("IS_TEST", false)
        if (isTest) {
            NotificationHelper.createNotificationChannel(applicationContext)
            NotificationHelper.sendNotification(
                applicationContext,
                "Testowe powiadomienie",
                "To jest testowe powiadomienie o ryzyku szronu."
            )
            return Result.success()
        }

        val settingsDataStore = SettingsDataStore(applicationContext)
        val userPreferences = settingsDataStore.userPreferencesFlow.first()
        val isCarMode = inputData.getBoolean("IS_CAR_MODE", false)

        if (!isCarMode && System.currentTimeMillis() < userPreferences.ignoreUntil) {
            return Result.success()
        }

        val tempThreshold = if (userPreferences.isAutoModeEnabled) 2.0 else userPreferences.tempThreshold
        val humidityThreshold = if (userPreferences.isAutoModeEnabled) 80.0 else userPreferences.humidityThreshold.toDouble()
        val precipitationThreshold = if (userPreferences.isAutoModeEnabled) 0.1 else userPreferences.precipitationThreshold

        return try {
            val location = getLastLocation(applicationContext)
            val weather = OpenMeteoApi.getWeather(location.latitude, location.longitude)
            val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
            val hasRisk = WeatherCalculations.hasFrostRisk(minTemp, weather.current.humidity, weather.current.precipitation, weather.current.weatherCode, tempThreshold, humidityThreshold, precipitationThreshold)

            if (isCarMode) {
                val carNotificationTitle = if (hasRisk) "Ryzyko szronu lub lodu na szybach!" else "Brak ryzyka szronu lub lodu."
                val carNotificationMessage = "Prognozowana minimalna temperatura w nocy to ${String.format(Locale.US, "%.1f", minTemp)}°C."
                NotificationHelper.createNotificationChannel(applicationContext)
                NotificationHelper.sendNotification(applicationContext, carNotificationTitle, carNotificationMessage)
            } else {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

                if (currentHour >= userPreferences.alertStartHour || currentHour < userPreferences.alertEndHour) {
                    if (hasRisk) {
                        NotificationHelper.createNotificationChannel(applicationContext)
                        NotificationHelper.sendNotification(
                            applicationContext,
                            "Uwaga, ryzyko szronu!",
                            "Prognozowana minimalna temperatura w nocy to ${String.format(Locale.US, "%.1f", minTemp)}°C."
                        )
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            // Log error if needed
            Result.failure()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(context: Context): Location {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { continuation ->
            val task: Task<Location> = fusedLocationClient.lastLocation
            task.addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    val warsawLocation = Location("").apply {
                        latitude = 52.2297
                        longitude = 21.0122
                    }
                    continuation.resume(warsawLocation)
                }
            }
            task.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }
}