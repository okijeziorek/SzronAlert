package pl.oki.frostalert.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.AppTelemetry
import pl.oki.frostalert.utils.NetworkMonitor
import pl.oki.frostalert.utils.NotificationHelper
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.widget.WidgetSyncHelper
import pl.oki.frostalert.R
import java.time.LocalDate

/**
 * Sends the morning weather brief notification shortly after the user's first phone unlock.
 *
 * Triggered by [pl.oki.frostalert.receiver.MorningUserPresentReceiver] or by the fallback
 * check inside [FrostCheckWorker]. Uses unique work naming ("morning_brief_<epochDay>") with
 * [androidx.work.ExistingWorkPolicy.KEEP] as a hard anti-spam gate (max 1 notification/day).
 */
@HiltWorker
class MorningBriefWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsDataStore: SettingsDataStore,
    private val locationRepository: LocationRepository,
    private val networkMonitor: NetworkMonitor,
    private val morningWakeLearningRepository: MorningWakeLearningRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "MorningBriefWorker"
        private const val MAX_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        // Network guard: bail early so backoff can retry rather than wasting a failed API call
        if (!networkMonitor.isCurrentlyOnline()) {
            val msg = "Network unavailable at runtime"
            Log.w(TAG, "$msg (attempt ${runAttemptCount + 1}/$MAX_RETRIES) — retrying")
            return if (runAttemptCount < MAX_RETRIES) {
                AppTelemetry.recordMorningBriefRetry(applicationContext, msg)
                Result.retry()
            } else {
                AppTelemetry.recordMorningBriefFailure(applicationContext, "$msg after $MAX_RETRIES attempts")
                Result.failure()
            }
        }

        val prefs = settingsDataStore.userPreferencesFlow.first()

        // Feature gate
        if (!prefs.isMorningBriefEnabled) {
            Log.d(TAG, "Morning brief disabled — skipping")
            return Result.success()
        }

        // Anti-spam: unique work name already prevents double-run, but guard here too
        val epochDay = LocalDate.now().toEpochDay()
        if (!morningWakeLearningRepository.shouldScheduleBriefToday(epochDay)) {
            Log.d(TAG, "Morning brief already sent today (epoch day $epochDay) — skipping")
            return Result.success()
        }

        // Fetch location
        val location = try {
            locationRepository.getEffectiveLocation()
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission denied (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
            return if (runAttemptCount < MAX_RETRIES) {
                AppTelemetry.recordMorningBriefRetry(applicationContext, "Location permission denied")
                Result.retry()
            } else {
                AppTelemetry.recordMorningBriefFailure(applicationContext, "Location permission denied after $MAX_RETRIES attempts")
                Result.failure()
            }
        }

        if (location == null) {
            Log.w(TAG, "Location unavailable (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
            return if (runAttemptCount < MAX_RETRIES) {
                AppTelemetry.recordMorningBriefRetry(applicationContext, "Location unavailable")
                Result.retry()
            } else {
                AppTelemetry.recordMorningBriefFailure(applicationContext, "Location unavailable after $MAX_RETRIES attempts")
                Result.failure()
            }
        }

        // Fetch weather
        val weatherResult = try {
            OpenMeteoApi.getWeather(location.latitude, location.longitude)
        } catch (e: Exception) {
            Log.e(TAG, "Weather fetch exception (attempt ${runAttemptCount + 1}/$MAX_RETRIES): ${e.message}", e)
            return if (runAttemptCount < MAX_RETRIES) {
                AppTelemetry.recordMorningBriefRetry(applicationContext, "Weather exception: ${e.message}")
                Result.retry()
            } else {
                AppTelemetry.recordMorningBriefFailure(applicationContext, "Weather exception after $MAX_RETRIES attempts: ${e.message}")
                Result.failure()
            }
        }

        return when (weatherResult) {
            is AppResult.Error -> {
                val msg = "[${weatherResult.error::class.simpleName}] ${weatherResult.error.message}"
                Log.w(TAG, "Weather fetch error $msg (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
                if (runAttemptCount < MAX_RETRIES) {
                    AppTelemetry.recordMorningBriefRetry(applicationContext, msg)
                    Result.retry()
                } else {
                    AppTelemetry.recordMorningBriefFailure(applicationContext, "$msg after $MAX_RETRIES attempts")
                    Result.failure()
                }
            }
            is AppResult.Success -> {
                val weather = weatherResult.data
                val tempThreshold = if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold
                val humidityThreshold = if (prefs.isAutoModeEnabled) 75.0 else prefs.humidityThreshold.toDouble()
                val precipThreshold = if (prefs.isAutoModeEnabled) 0.2 else prefs.precipitationThreshold

                val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                val hasRisk = WeatherCalculations.hasFrostRisk(
                    temp = minTemp,
                    humidity = weather.current.humidity,
                    precip = weather.current.precipitation,
                    weatherCode = weather.current.weatherCode,
                    tempThreshold = tempThreshold,
                    humidityThreshold = humidityThreshold,
                    precipitationThreshold = precipThreshold,
                    sensitivity = prefs.sensitivity,
                    windSpeed = weather.current.windSpeed,
                    appMode = prefs.appMode
                )
                val frostProbability = WeatherCalculations.calculateFrostProbability(
                    temp = minTemp,
                    humidity = weather.current.humidity,
                    precip = weather.current.precipitation,
                    weatherCode = weather.current.weatherCode,
                    tempThreshold = tempThreshold,
                    humidityThreshold = humidityThreshold,
                    precipitationThreshold = precipThreshold,
                    sensitivity = prefs.sensitivity,
                    windSpeed = weather.current.windSpeed,
                    appMode = prefs.appMode
                )

                // Build and send morning brief notification
                val title = applicationContext.getString(R.string.notification_morning_brief_title)
                val message = if (hasRisk) {
                    applicationContext.getString(
                        R.string.notification_morning_brief_risk_message,
                        minTemp,
                        frostProbability
                    )
                } else {
                    applicationContext.getString(
                        R.string.notification_morning_brief_no_risk_message,
                        minTemp
                    )
                }

                NotificationHelper.createNotificationChannel(applicationContext)
                val notificationPosted = NotificationHelper.sendMorningBriefNotification(applicationContext, title, message)

                if (!notificationPosted) {
                    // Permission denied or NotificationManager unavailable — do not mark as sent
                    // so it can be retried when permission is granted.
                    AppTelemetry.recordMorningBriefFailure(applicationContext, "Notification not posted (permission denied or manager unavailable)")
                    Log.w(TAG, "Morning brief notification was not posted — not recording as sent")
                    return Result.success()
                }

                // Refresh both widgets
                try {
                    WidgetSyncHelper.updateAll(applicationContext)
                } catch (e: Exception) {
                    Log.w(TAG, "Widget update failed: ${e.message}")
                }

                // Record that today's notification was sent — only reached when notification was posted
                morningWakeLearningRepository.recordNotificationSent(epochDay)
                AppTelemetry.recordMorningBriefSuccess(applicationContext)
                Log.i(TAG, "Morning brief sent: hasRisk=$hasRisk minTemp=$minTemp epochDay=$epochDay")
                Result.success()
            }
        }
    }
}
