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

                // Compact the widget content so small widget sizes still show the important info
                val compact = "Szron: ${String.format(Locale.US, "%.1f", lastRecord.minTemp)}°C • $riskText (${date})"
                views.setTextViewText(R.id.widget_title, compact)
                // Hide the secondary fields to avoid large empty areas on small widgets
                views.setViewVisibility(R.id.widget_temp, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_risk, android.view.View.GONE)
            } else {
                views.setTextViewText(R.id.widget_title, "Brak danych\nKliknij odśwież")
                views.setViewVisibility(R.id.widget_temp, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_risk, android.view.View.GONE)
            }

            // Ensure widget update runs on main thread to avoid potential platform limitations
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    android.util.Log.w("FrostWidgetProvider", "Failed to post updateAppWidget for $appWidgetId: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("FrostWidgetProvider", "Error updating widget $appWidgetId: ${e.message}")
        }
    }
}
