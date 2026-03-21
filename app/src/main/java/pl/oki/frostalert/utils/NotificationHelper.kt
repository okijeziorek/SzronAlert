package pl.oki.frostalert.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.receiver.NotificationActionReceiver
import pl.oki.frostalert.ui.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "frost_alert_channel"
    private const val CHANNEL_NAME = "Frost Alerts"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Alerts about frost and ice on windshields"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Sends a frost alert notification.
     *
     * @param isMataOptionEnabled whether to show the "applied mat" action button.
     *        Pass this from already-loaded preferences to avoid an extra DataStore read.
     */
    fun sendNotification(context: Context, title: String, message: String, isMataOptionEnabled: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_frost)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Przycisk "Ignoruj dziś"
        val ignoreIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_IGNORE_TODAY"
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            context, 1, ignoreIntent, PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, context.getString(R.string.action_ignore), ignorePendingIntent)

        // Przycisk "Snooze" (Przypomnij za 2h)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE_2H"
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, 3, snoozeIntent, PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, context.getString(R.string.action_snooze), snoozePendingIntent)

        // Przycisk "Zastosowałem matę" - jeśli włączony w ustawieniach
        if (isMataOptionEnabled) {
            val mataIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_MATA_APPLIED"
            }
            val mataPendingIntent = PendingIntent.getBroadcast(
                context, 2, mataIntent, PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, context.getString(R.string.action_mata), mataPendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * Overload that reads preferences from DataStore when the caller does not already have them.
     * Note: uses runBlocking — call only from a background thread or within a Worker.
     */
    fun sendNotification(context: Context, title: String, message: String) {
        val settingsDataStore = SettingsDataStore(context)
        val prefs = runBlocking { settingsDataStore.userPreferencesFlow.first() }
        sendNotification(context, title, message, isMataOptionEnabled = prefs.isMataOptionEnabled)
    }
}
