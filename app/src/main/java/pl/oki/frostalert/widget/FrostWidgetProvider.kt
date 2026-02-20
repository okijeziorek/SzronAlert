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
        val db = FrostDatabase.getDatabase(context)
        val lastRecord = db.temperatureDao().getRecentRecords().first().firstOrNull()

        if (lastRecord != null) {
            val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
            val date = sdf.format(Date(lastRecord.timestamp))
            val riskText = if (lastRecord.hasRisk) "Wysokie" else "Niskie"

            views.setTextViewText(R.id.widget_title, "Szron Alert ($date)")
            views.setTextViewText(R.id.widget_temp, "Noc min: ${String.format(Locale.US, "%.1f", lastRecord.minTemp)}°C")
            views.setTextViewText(R.id.widget_risk, "Ryzyko: $riskText")
        } else {
            views.setTextViewText(R.id.widget_title, "Brak danych")
            views.setTextViewText(R.id.widget_temp, "")
            views.setTextViewText(R.id.widget_risk, "")
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
