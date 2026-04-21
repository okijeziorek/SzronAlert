package pl.oki.frostalert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.repository.MorningWakeLearningRepository
import pl.oki.frostalert.worker.MorningBriefWorker
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Listens for [Intent.ACTION_USER_PRESENT] (screen unlocked after keyguard) and triggers
 * the morning brief worker on the first unlock of each day, subject to:
 *  - feature toggle ([UserPreferences.isMorningBriefEnabled])
 *  - morning-window check (learned from history or default 06:00–10:00)
 *  - hard anti-spam (one unique work per epoch day)
 *
 * Registration: AndroidManifest with ACTION_USER_PRESENT intent-filter. This broadcast is
 * exempt from Android 8+ implicit-broadcast restrictions (see exemptions list).
 */
class MorningUserPresentReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MorningUserPresent"
        const val WORK_NAME_PREFIX = "morning_brief_"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsDataStore = SettingsDataStore(context)
                val prefs = settingsDataStore.userPreferencesFlow.first()

                if (!prefs.isMorningBriefEnabled) {
                    Log.d(TAG, "Morning brief disabled — skipping")
                    return@launch
                }

                val now = Calendar.getInstance()
                val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val epochDay = LocalDate.now().toEpochDay()

                val repo = MorningWakeLearningRepository(settingsDataStore)

                // Record first unlock of the day (also updates learned window)
                repo.recordFirstUnlockOfDay(epochDay, minuteOfDay)

                // Anti-spam: skip if brief already sent today
                if (!repo.shouldScheduleBriefToday(epochDay)) {
                    Log.d(TAG, "Morning brief already sent today (epoch day $epochDay) — skipping")
                    return@launch
                }

                // Only enqueue during the morning window
                if (!repo.isWithinMorningWindow(minuteOfDay)) {
                    Log.d(TAG, "Not in morning window (minute=$minuteOfDay, window=${prefs.morningWindowStartMinute}–${prefs.morningWindowEndMinute}) — skipping")
                    return@launch
                }

                // Schedule MorningBriefWorker with the configured delay
                val delayMinutes = prefs.morningBriefDelayMinutes.toLong().coerceAtLeast(0L)
                val workRequest = OneTimeWorkRequestBuilder<MorningBriefWorker>()
                    .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

                val workName = "$WORK_NAME_PREFIX$epochDay"
                WorkManager.getInstance(context).enqueueUniqueWork(
                    workName,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )

                repo.recordScheduled(System.currentTimeMillis())
                Log.i(TAG, "Scheduled MorningBriefWorker: name=$workName delay=${delayMinutes}min window=${prefs.morningWindowStartMinute}–${prefs.morningWindowEndMinute}")
            } catch (e: Exception) {
                Log.e(TAG, "Error in MorningUserPresentReceiver: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
