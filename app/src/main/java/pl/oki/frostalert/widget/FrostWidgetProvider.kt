package pl.oki.frostalert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.worker.FrostCheckWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FrostWidgetProvider : AppWidgetProvider() {

    companion object {
        const val REFRESH_ACTION = "pl.oki.frostalert.action.WIDGET_REFRESH"

        /** Updates every installed classic (RemoteViews) widget instance. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FrostWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == REFRESH_ACTION) {
            val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "manual_widget_refresh",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.frost_widget_layout)
    val scope = CoroutineScope(Dispatchers.IO)

    // Tapping anywhere on the widget triggers a manual refresh via FrostCheckWorker
    val refreshIntent = Intent(context, FrostWidgetProvider::class.java).apply {
        action = FrostWidgetProvider.REFRESH_ACTION
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, appWidgetId, refreshIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

    scope.launch {
        try {
            val db = FrostDatabase.getDatabase(context)
            val records = try { db.temperatureDao().getRecentRecords().first() } catch (e: Exception) {
                // log and fallback
                android.util.Log.w("FrostWidgetProvider", "Failed to read recent records: ${e.message}")
                emptyList()
            }
            val lastRecord = records.firstOrNull()

            android.util.Log.i("FrostWidgetProvider", "updateAppWidget: appWidgetId=$appWidgetId, recordsCount=${records.size}, last=${lastRecord?.minTemp}")

            if (lastRecord != null) {
                val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
                val date = sdf.format(Date(lastRecord.timestamp))
                val riskText = if (lastRecord.hasRisk) "Wysokie" else "Niskie"

                views.setTextViewText(R.id.widget_title, "FrostAlert • $date")
                views.setTextViewText(R.id.widget_temp, "Min: ${String.format(Locale.US, "%.1f", lastRecord.minTemp)}°C")
                views.setTextViewText(R.id.widget_risk, "Ryzyko: $riskText")
                views.setViewVisibility(R.id.widget_temp, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_risk, android.view.View.VISIBLE)
            } else {
                views.setTextViewText(R.id.widget_title, "FrostAlert")
                views.setTextViewText(R.id.widget_temp, "Brak danych")
                views.setTextViewText(R.id.widget_risk, "Dotknij, aby odświeżyć")
                views.setViewVisibility(R.id.widget_temp, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_risk, android.view.View.VISIBLE)
            }

            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.w("FrostWidgetProvider", "Failed to updateAppWidget for $appWidgetId: ${e.message}")
            }
        } catch (e: Exception) {
            android.util.Log.w("FrostWidgetProvider", "Error updating widget $appWidgetId: ${e.message}")
        }
    }
}
