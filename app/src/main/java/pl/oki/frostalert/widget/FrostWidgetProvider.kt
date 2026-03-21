package pl.oki.frostalert.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.FrostDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FrostWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.frost_widget_layout)
    val scope = CoroutineScope(Dispatchers.IO)

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
                views.setTextViewText(R.id.widget_risk, "Uruchom odświeżenie")
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
