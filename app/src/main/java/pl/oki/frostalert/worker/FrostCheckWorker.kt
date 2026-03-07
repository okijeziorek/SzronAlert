package pl.oki.frostalert.worker

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.location.Location
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.utils.NotificationHelper
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.widget.FrostWidgetProvider
import pl.oki.frostalert.widget.updateAppWidget
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume

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
        val sensitivity = userPreferences.sensitivity

        return try {
            val location = if (userPreferences.isManualLocationEnabled) {
                Location("manual").apply {
                    latitude = userPreferences.manualLatitude
                    longitude = userPreferences.manualLongitude
                }
            } else {
                getCurrentLocation(applicationContext)
            }
            
            val weather = OpenMeteoApi.getWeather(location.latitude, location.longitude)
            val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
            
            val hasRisk = WeatherCalculations.hasFrostRisk(
                minTemp, 
                weather.current.humidity, 
                weather.current.precipitation, 
                weather.current.weatherCode, 
                tempThreshold, 
                humidityThreshold, 
                precipitationThreshold,
                sensitivity = sensitivity
            )

            val record = TemperatureRecord(
                timestamp = System.currentTimeMillis(),
                minTemp = minTemp,
                hasRisk = hasRisk
            )
            val db = FrostDatabase.getDatabase(applicationContext)
            db.temperatureDao().insert(record)

            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, FrostWidgetProvider::class.java))
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(applicationContext, appWidgetManager, appWidgetId)
            }

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
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(context: Context): Location {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && (System.currentTimeMillis() - location.time) < 30 * 60 * 1000) {
                    continuation.resume(location)
                } else {
                    val cts = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { freshLocation ->
                        if (freshLocation != null) {
                            continuation.resume(freshLocation)
                        } else {
                            continuation.resume(Location("").apply {
                                latitude = 52.2297
                                longitude = 21.0122
                            })
                        }
                    }.addOnFailureListener {
                        continuation.resume(Location("").apply {
                            latitude = 52.2297
                            longitude = 21.0122
                        })
                    }
                }
            }.addOnFailureListener {
                continuation.resume(Location("").apply {
                    latitude = 52.2297
                    longitude = 21.0122
                })
            }
        }
    }
}
