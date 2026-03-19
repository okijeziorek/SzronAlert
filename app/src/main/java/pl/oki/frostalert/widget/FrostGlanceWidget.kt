package pl.oki.frostalert.widget

import android.content.Context
import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.worker.FrostCheckWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "FrostGlanceWidget"

class FrostGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var lastRecord: pl.oki.frostalert.data.local.TemperatureRecord? = null
        var recordsCount = 0
        var useFahrenheit = false

        try {
            val db = FrostDatabase.getDatabase(context)
            val records = try {
                db.temperatureDao().getRecentRecords().first()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read recent records: ${e.message}")
                emptyList()
            }
            lastRecord = records.firstOrNull()
            recordsCount = records.size

            try {
                val settings = SettingsDataStore(context).userPreferencesFlow.first()
                useFahrenheit = settings.useFahrenheit
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read settings for widget: ${e.message}")
            }

            // Diagnostic log
            Log.i(TAG, "provideGlance: recordsCount=$recordsCount, lastRecordMin=${lastRecord?.minTemp}, lastHasRisk=${lastRecord?.hasRisk}, useFahrenheit=$useFahrenheit")
        } catch (e: Exception) {
            Log.w(TAG, "Error preparing widget data: ${e.message}")
        }

        // Provide content directly (no try/catch around composable invocation)
        provideContent {
            GlanceTheme {
                WidgetContent(lastRecord, useFahrenheit, recordsCount)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(lastRecord: pl.oki.frostalert.data.local.TemperatureRecord?, useFahrenheit: Boolean, recordsCount: Int) {
        // Compact single-line widget content to improve rendering across launchers and sizes
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = Color.White, night = Color.Black))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val contentText = if (lastRecord != null) {
                val date = try { SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(lastRecord.timestamp)) } catch (_: Exception) { "--" }
                val tempText = try { WeatherCalculations.formatTemperature(lastRecord.minTemp, useFahrenheit) } catch (_: Exception) { "--" }
                val riskText = if (lastRecord.hasRisk) "Wysokie" else "Niskie"
                "Szron: $tempText • $riskText • $date"
            } else {
                "Brak danych ($recordsCount)"
            }

            Text(
                text = contentText,
                style = TextStyle(color = ColorProvider(day = Color.Black, night = Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            Log.i(TAG, "RefreshActionCallback: enqueueing FrostCheckWorker")
            val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "manual_widget_refresh",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enqueue widget refresh worker: ${e.message}")
        }
    }
}

class FrostGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FrostGlanceWidget()
}
