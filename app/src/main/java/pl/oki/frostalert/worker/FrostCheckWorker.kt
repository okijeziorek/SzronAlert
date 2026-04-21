package pl.oki.frostalert.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.AppTelemetry
import pl.oki.frostalert.utils.NetworkMonitor
import pl.oki.frostalert.utils.NotificationHelper
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.utils.TrendCalculations
import pl.oki.frostalert.widget.WidgetSyncHelper
import pl.oki.frostalert.R
import pl.oki.frostalert.receiver.MorningUserPresentReceiver
import java.time.LocalDate
import java.util.Calendar
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

@HiltWorker
class FrostCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsDataStore: SettingsDataStore,
    private val locationRepository: LocationRepository,
    private val temperatureDao: TemperatureDao,
    private val networkMonitor: NetworkMonitor,
    private val morningWakeLearningRepository: MorningWakeLearningRepository
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
                applicationContext.getString(R.string.notification_test_title),
                applicationContext.getString(R.string.notification_test_message)
            )
            return Result.success()
        }

        // Explicit network guard — WorkManager's CONNECTED constraint is enforced at scheduling
        // time, but connectivity can drop before the coroutine actually runs.  Bail early with
        // retry so the exponential backoff kicks in rather than burning a network call that will
        // fail anyway.
        if (!networkMonitor.isCurrentlyOnline()) {
            val msg = "Network unavailable at runtime"
            Log.w(TAG, "$msg (attempt ${runAttemptCount + 1}/$MAX_RETRIES) — retrying")
            return if (runAttemptCount < MAX_RETRIES) {
                AppTelemetry.recordWorkerRetry(applicationContext, msg)
                Result.retry()
            } else {
                AppTelemetry.recordWorkerFailure(applicationContext, "$msg after $MAX_RETRIES attempts")
                Result.failure()
            }
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

        try {
            val location = locationRepository.getEffectiveLocation()
            if (location == null) {
                Log.w(TAG, "Location unavailable (attempt ${runAttemptCount + 1}/$MAX_RETRIES) — retrying")
                return if (runAttemptCount < MAX_RETRIES) {
                    AppTelemetry.recordWorkerRetry(applicationContext, "Location unavailable")
                    Result.retry()
                } else {
                    AppTelemetry.recordWorkerFailure(applicationContext, "Location unavailable after $MAX_RETRIES attempts")
                    Result.failure()
                }
            }

            val weatherResult = OpenMeteoApi.getWeather(location.latitude, location.longitude)

            when (weatherResult) {
                is AppResult.Error -> {
                    val errorType = weatherResult.error::class.simpleName
                    val errorMessage = weatherResult.error.message
                    val errorCause = weatherResult.error.cause
                    Log.w(TAG, "Weather fetch error [$errorType] (attempt ${runAttemptCount + 1}/$MAX_RETRIES): $errorMessage", errorCause)
                    return if (runAttemptCount < MAX_RETRIES) {
                        AppTelemetry.recordWorkerRetry(applicationContext, "[$errorType] $errorMessage")
                        Result.retry()
                    } else {
                        AppTelemetry.recordWorkerFailure(applicationContext, "[$errorType] $errorMessage after $MAX_RETRIES attempts")
                        Result.failure()
                    }
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

                    val frostProbability = WeatherCalculations.calculateFrostProbability(
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
                        hasRisk = hasRisk,
                        frostProbability = frostProbability
                    )
                    temperatureDao.insert(record)

                    // SPRAWDŹ ZMIANĘ TRENDU
                    try {
                        val recordsAfterInsert = temperatureDao.getRecentRecords().first()
                        val newTrend = TrendCalculations.calculateWeeklyTrend(recordsAfterInsert).trend
                        val oldTrend = userPreferences.lastTrend
                        val pendingTrend = userPreferences.pendingTrend

                        if (oldTrend == null) {
                            // Inicjalizacja – zapisz pierwszy wynik bez powiadomienia
                            settingsDataStore.updateLastTrend(newTrend)
                        } else {
                            when {
                                newTrend == oldTrend -> {
                                    // Trend stabilny – czyść kandydata jeśli istnieje
                                    if (pendingTrend != null) settingsDataStore.updatePendingTrend(null)
                                }
                                newTrend == pendingTrend -> {
                                    // Potwierdzona zmiana trendu (2 kolejne sprawdzenia) – wyślij powiadomienie
                                    if (userPreferences.isTrendChangeNotificationsEnabled) {
                                        val trendMessage = when (newTrend) {
                                            TrendCalculations.TrendDirection.UP -> applicationContext.getString(R.string.notification_trend_warming)
                                            TrendCalculations.TrendDirection.DOWN -> applicationContext.getString(R.string.notification_trend_cooling)
                                            TrendCalculations.TrendDirection.STABLE -> applicationContext.getString(R.string.notification_trend_stable)
                                        }
                                        NotificationHelper.createNotificationChannel(applicationContext)
                                        NotificationHelper.sendNotification(
                                            applicationContext,
                                            applicationContext.getString(R.string.notification_trend_title),
                                            trendMessage
                                        )
                                    }
                                    // Zaktualizuj potwierdzony trend i wyczyść kandydata
                                    settingsDataStore.updateLastTrend(newTrend)
                                    settingsDataStore.updatePendingTrend(null)
                                }
                                else -> {
                                    // Nowy kandydat na zmianę – czekaj na potwierdzenie w kolejnym sprawdzeniu
                                    settingsDataStore.updatePendingTrend(newTrend)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Błąd sprawdzania zmiany trendu: ${e.message}")
                    }

                    // SPRAWDŹ GEOFENCING - czy użytkownik wjechał w rejon z wyższym ryzykiem
                    if (!isCarMode && userPreferences.isGeofencingEnabled) {
                        try {

                            val geofencingResult = locationRepository.checkGeofencingRisk(location, userPreferences, currentWeather = weather)
                            when (geofencingResult) {
                                is pl.oki.frostalert.data.repository.GeofencingResult.HigherRiskNearby -> {
                                    AppTelemetry.recordGeofenceTrigger(applicationContext)
                                    val direction = geofencingResult.direction
                                    val riskIncrease = geofencingResult.riskIncrease
                                    val currentMinTemp = geofencingResult.currentRisk.minTemp

                                    NotificationHelper.createNotificationChannel(applicationContext)
                                    val title = applicationContext.getString(R.string.notification_geofence_title)
                                    val message = applicationContext.getString(
                                        R.string.notification_geofence_message,
                                        direction,
                                        (riskIncrease * 100).toInt(),
                                        currentMinTemp
                                    )
                                    NotificationHelper.sendNotification(applicationContext, title, message,
                                        isMataOptionEnabled = userPreferences.isMataOptionEnabled)
                                }
                                is pl.oki.frostalert.data.repository.GeofencingResult.Error -> {
                                    AppTelemetry.recordGeofenceError(applicationContext)
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

                    // Odświeżanie widgetów — aktualizuj oba ścieżki atomowo przez WidgetSyncHelper
                    try {
                        WidgetSyncHelper.updateAll(applicationContext)
                    } catch (e: Exception) {
                        Log.w(TAG, "Nie udało się zaktualizować widgetów: ${e.message}")
                    }

                    if (isCarMode) {
                        if (hasRisk) {
                            NotificationHelper.createNotificationChannel(applicationContext)
                            val title = if (appMode == 1) applicationContext.getString(R.string.notification_frost_garden_title) else applicationContext.getString(R.string.notification_frost_car_title)
                            val message = applicationContext.getString(R.string.notification_frost_forecast_message, minTemp)
                            NotificationHelper.sendNotification(applicationContext, title, message,
                                isMataOptionEnabled = userPreferences.isMataOptionEnabled)
                        }
                    } else {
                        val calendar = Calendar.getInstance()
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

                        if (currentHour >= userPreferences.alertStartHour || currentHour < userPreferences.alertEndHour) {
                            if (hasRisk) {
                                NotificationHelper.createNotificationChannel(applicationContext)
                                val title = if (appMode == 1) applicationContext.getString(R.string.notification_frost_garden_alert_title) else applicationContext.getString(R.string.notification_frost_car_alert_title)
                                NotificationHelper.sendNotification(applicationContext, title, applicationContext.getString(R.string.notification_frost_night_message),
                                    isMataOptionEnabled = userPreferences.isMataOptionEnabled)
                            }
                        }
                    }
                    AppTelemetry.recordWorkerSuccess(applicationContext)

                    // FALLBACK: Schedule morning brief if the USER_PRESENT broadcast was missed
                    // (e.g., OEM battery optimizations) and conditions are met.
                    if (!isCarMode && userPreferences.isMorningBriefEnabled) {
                        try {
                            val cal = Calendar.getInstance()
                            val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                            val epochDay = LocalDate.now().toEpochDay()
                            val inWindow = minuteOfDay in userPreferences.morningWindowStartMinute..userPreferences.morningWindowEndMinute
                            if (inWindow && morningWakeLearningRepository.shouldScheduleBriefToday(epochDay)) {
                                val workRequest = OneTimeWorkRequestBuilder<MorningBriefWorker>()
                                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                                    .build()
                                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                                    "${MorningUserPresentReceiver.WORK_NAME_PREFIX}$epochDay",
                                    ExistingWorkPolicy.KEEP,
                                    workRequest
                                )
                                Log.d(TAG, "Fallback: scheduled MorningBriefWorker for epoch day $epochDay")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Fallback morning brief scheduling failed: ${e.message}")
                        }
                    }

                    return Result.success()
                }
            }
        } catch (e: Exception) {
            val errorType = e::class.simpleName
            Log.e(TAG, "Unexpected error [$errorType] in doWork (attempt ${runAttemptCount + 1}/$MAX_RETRIES): ${e.message}", e)
            return if (runAttemptCount < MAX_RETRIES) {
                AppTelemetry.recordWorkerRetry(applicationContext, "[$errorType] ${e.message}")
                Result.retry()
            } else {
                AppTelemetry.recordWorkerFailure(applicationContext, "[$errorType] ${e.message}")
                Result.failure()
            }
        }
    }
}
