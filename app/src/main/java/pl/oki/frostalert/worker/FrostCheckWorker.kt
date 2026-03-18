package pl.oki.frostalert.worker

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.NotificationHelper
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.utils.TrendCalculations
import pl.oki.frostalert.widget.updateAppWidget
import pl.oki.frostalert.widget.FrostWidgetProvider
import pl.oki.frostalert.geofence.GeofenceManager
import java.util.Calendar
import java.util.Locale

@HiltWorker
class FrostCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsDataStore: SettingsDataStore,
    private val locationRepository: LocationRepository,
    private val temperatureDao: TemperatureDao
) : CoroutineWorker(appContext, params) {

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

        val userPreferences = settingsDataStore.userPreferencesFlow.first()
        val isCarMode = inputData.getBoolean("IS_CAR_MODE", false)

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
                    temperatureDao.insert(record)

                    // Aktualizuj Glance widget
                    pl.oki.frostalert.widget.FrostGlanceWidget().updateAll(applicationContext)

                    // SPRAWDŹ ZMIANĘ TRENDU
                    if (userPreferences.isTrendChangeNotificationsEnabled) {
                        try {
                            val recordsAfterInsert = temperatureDao.getRecentRecords().first()
                            val newTrend = TrendCalculations.calculateWeeklyTrend(recordsAfterInsert).trend
                            val oldTrend = userPreferences.lastTrend
                            if (newTrend != oldTrend && oldTrend != null) {
                                // Trend się zmienił
                                val trendMessage = when (newTrend) {
                                    TrendCalculations.TrendDirection.UP -> "Robi się cieplej! 📈"
                                    TrendCalculations.TrendDirection.DOWN -> "Robi się chłodniej! 📉"
                                    TrendCalculations.TrendDirection.STABLE -> "Trend się ustabilizował ➡️"
                                }
                                NotificationHelper.createNotificationChannel(applicationContext)
                                NotificationHelper.sendNotification(
                                    applicationContext,
                                    "Zmiana trendu temperatury",
                                    trendMessage
                                )
                            }
                            // Zaktualizuj ostatni trend
                            settingsDataStore.updateLastTrend(newTrend)
                        } catch (e: Exception) {
                            Log.w(TAG, "Błąd sprawdzania zmiany trendu: ${e.message}")
                        }
                    }

                    // SPRAWDŹ GEOFENCING - czy użytkownik wjechał w rejon z wyższym ryzykiem
                    if (!isCarMode && userPreferences.isGeofencingEnabled) {
                        try {

                            val geofencingResult = locationRepository.checkGeofencingRisk(location, userPreferences)
                            when (geofencingResult) {
                                is pl.oki.frostalert.data.repository.GeofencingResult.HigherRiskNearby -> {
                                    val direction = geofencingResult.direction
                                    val riskIncrease = geofencingResult.riskIncrease
                                    val currentMinTemp = geofencingResult.currentRisk.minTemp

                                    NotificationHelper.createNotificationChannel(applicationContext)
                                    val title = "⚠️ Wyższe ryzyko szronu w okolicy!"
                                    val message = "W kierunku $direction ryzyko jest o ${(riskIncrease * 100).toInt()}% wyższe. " +
                                                 "Aktualna prognoza: ${String.format(Locale.US, "%.1f", currentMinTemp)}°C."
                                    NotificationHelper.sendNotification(applicationContext, title, message)
                                }
                                is pl.oki.frostalert.data.repository.GeofencingResult.Error -> {
                                    Log.w(TAG, "Błąd sprawdzania geofencing: ${geofencingResult.message}")
                                }
                                else -> {
                                    // NoRisk - nic nie robimy
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Wyjątek podczas sprawdzania geofencing: ${e.message}")
                        }
                    }

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
