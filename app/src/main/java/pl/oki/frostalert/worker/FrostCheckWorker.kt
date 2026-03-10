package pl.oki.frostalert.worker

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.NotificationHelper
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.widget.FrostWidgetProvider
import pl.oki.frostalert.widget.updateAppWidget
import java.util.Calendar
import java.util.Locale

class FrostCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "FrostCheckWorker"
        private const val MAX_RETRIES = 3
    }

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
        val locationRepository = LocationRepository(applicationContext, settingsDataStore)
        val userPreferences = settingsDataStore.userPreferencesFlow.first()
        val isCarMode = inputData.getBoolean("IS_CAR_MODE", false)

        // POPRAWKA: Respektujemy wyciszenie (ignoreUntil) zawsze, 
        // nawet w porannym trybie samochodu, jeśli użytkownik np. założył matę.
        if (System.currentTimeMillis() < userPreferences.ignoreUntil) {
            Log.d(TAG, "Pomijanie: Alerty są obecnie wyciszone (mata lub ignore)")
            return Result.success()
        }

        val tempThreshold = if (userPreferences.isAutoModeEnabled) 1.0 else userPreferences.tempThreshold
        val humidityThreshold = if (userPreferences.isAutoModeEnabled) 75.0 else userPreferences.humidityThreshold.toDouble()
        val precipitationThreshold = if (userPreferences.isAutoModeEnabled) 0.2 else userPreferences.precipitationThreshold
        val sensitivity = userPreferences.sensitivity
        val appMode = userPreferences.appMode

        return try {
            val location = locationRepository.getEffectiveLocation()
            if (location == null) {
                Log.w(TAG, "Nie udało się pobrać lokalizacji")
                return Result.retry()
            }

            val weatherResult = OpenMeteoApi.getWeather(location.latitude, location.longitude)

            when (weatherResult) {
                is AppResult.Error -> {
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
                }
                is AppResult.Success -> {
                    val weather = weatherResult.data
                    val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)

                    val hasRisk = WeatherCalculations.hasFrostRisk(
                        temp = minTemp,
                        humidity = weather.current.humidity,
                        precip = weather.current.precipitation,
                        weatherCode = weather.current.weatherCode,
                        tempThreshold = tempThreshold,
                        humidityThreshold = humidityThreshold,
                        precipitationThreshold = precipitationThreshold,
                        sensitivity = sensitivity,
                        windSpeed = weather.current.windSpeed,
                        appMode = appMode
                    )

                    val record = TemperatureRecord(
                        timestamp = System.currentTimeMillis(),
                        minTemp = minTemp,
                        hasRisk = hasRisk
                    )
                    FrostDatabase.getDatabase(applicationContext).temperatureDao().insert(record)

                    // Odświeżanie widgetów
                    val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, FrostWidgetProvider::class.java))
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(applicationContext, appWidgetManager, appWidgetId)
                    }

                    if (isCarMode) {
                        if (hasRisk) {
                            NotificationHelper.createNotificationChannel(applicationContext)
                            val title = if (appMode == 1) "Ryzyko przymrozku w ogrodzie!" else "Ryzyko lodu na szybach!"
                            val message = "Prognozowane min: ${String.format(Locale.US, "%.1f", minTemp)}°C."
                            NotificationHelper.sendNotification(applicationContext, title, message)
                        }
                    } else {
                        val calendar = Calendar.getInstance()
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

                        // Alerty wieczorne/nocne tylko w wyznaczonych godzinach
                        if (currentHour >= userPreferences.alertStartHour || currentHour < userPreferences.alertEndHour) {
                            if (hasRisk) {
                                NotificationHelper.createNotificationChannel(applicationContext)
                                val title = if (appMode == 1) "Uwaga na rośliny!" else "Uwaga, szron!"
                                NotificationHelper.sendNotification(applicationContext, title, "Możliwy przymrozek w nocy.")
                            }
                        }
                    }
                    Result.success()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }
}
