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
        val scope = CoroutineScope(Dispatchers.IO)
        val dataStore = SettingsDataStore(context)

        when (intent.action) {
            "SNOOZE_15" -> snooze(context, 15)
            "IGNORE_TODAY" -> {
                scope.launch {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    dataStore.updateIgnoreUntil(calendar.timeInMillis)
                }
            }
        }

        // Close the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
    }

    private fun snooze(context: Context, minutes: Long) {
        val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
