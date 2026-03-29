package pl.oki.frostalert.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.worker.FrostCheckWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "FrostWidgetProvider"

// Application-level scope so widget updates are not tied to a single component lifecycle.
private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    widgetScope.launch {
        try {
            val db = FrostDatabase.getDatabase(context)
            val records = try {
                db.temperatureDao().getRecentRecords().first()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read recent records for widget $appWidgetId: ${e.message}")
                emptyList()
            }
            val lastRecord = records.firstOrNull()

            val useFahrenheit = try {
                SettingsDataStore(context).userPreferencesFlow.first().useFahrenheit
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read useFahrenheit for widget $appWidgetId: ${e.message}")
                false
            }

            Log.i(TAG, "updateAppWidget: id=$appWidgetId records=${records.size} lastTemp=${lastRecord?.minTemp} useFahrenheit=$useFahrenheit")

            if (lastRecord != null) {
                val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
                val date = sdf.format(Date(lastRecord.timestamp))
                val riskText = if (lastRecord.hasRisk) "Wysokie" else "Niskie"
                val tempText = WeatherCalculations.formatTemperature(lastRecord.minTemp, useFahrenheit)

                views.setTextViewText(R.id.widget_title, "FrostAlert • $date")
                views.setTextViewText(R.id.widget_temp, "Min: $tempText")
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
                Log.w(TAG, "Failed to push RemoteViews for widget $appWidgetId: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget $appWidgetId: ${e.message}", e)
        }
    }
}
