package pl.oki.frostalert.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import pl.oki.frostalert.utils.AppTelemetry

private const val TAG = "WidgetSyncHelper"

/**
 * Ensures both widget paths (Glance + classic RemoteViews) are always updated together.
 * Calling this instead of updating each widget individually prevents stale states
 * caused by one path succeeding while the other fails.
 */
object WidgetSyncHelper {

    /**
     * Updates every installed widget — both Glance ([FrostGlanceWidget]) and classic
     * AppWidget ([FrostWidgetProvider] / RemoteViews).  Either path failing is logged but
     * does not prevent the other path from being updated.
     */
    suspend fun updateAll(context: Context) {
        var hasError = false

        // Glance widget (suspend call from Glance library)
        try {
            FrostGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            Log.w(TAG, "Glance widget update failed: ${e.message}")
            hasError = true
        }

        // Classic RemoteViews widget
        try {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, FrostWidgetProvider::class.java)
            )
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Classic widget update failed: ${e.message}")
            hasError = true
        }

        if (hasError) {
            AppTelemetry.recordWidgetError(context)
        } else {
            AppTelemetry.recordWidgetUpdate(context)
        }
    }
}
