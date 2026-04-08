package pl.oki.frostalert.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.worker.FrostCheckWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dataStore = SettingsDataStore(context)

        // Dismiss the notification immediately so the user gets instant feedback.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        when (intent.action) {
            ACTION_IGNORE_TODAY -> {
                // goAsync() keeps the receiver alive until pendingResult.finish(),
                // preventing the coroutine scope from being GC'd prematurely.
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        dataStore.updateIgnoreUntil(calendar.timeInMillis)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_MATA_APPLIED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val calendar = Calendar.getInstance()
                        if (calendar.get(Calendar.HOUR_OF_DAY) >= 20) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        calendar.set(Calendar.HOUR_OF_DAY, 8)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        dataStore.updateIgnoreUntil(calendar.timeInMillis)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE_2H -> {
                val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>()
                    .setInitialDelay(2, TimeUnit.HOURS)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    companion object {
        const val ACTION_IGNORE_TODAY = "ACTION_IGNORE_TODAY"
        const val ACTION_MATA_APPLIED = "ACTION_MATA_APPLIED"
        const val ACTION_SNOOZE_2H = "ACTION_SNOOZE_2H"
        private const val NOTIFICATION_ID = 1001
    }
}
