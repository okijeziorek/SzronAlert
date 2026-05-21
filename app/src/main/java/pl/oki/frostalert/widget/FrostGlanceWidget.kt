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
import androidx.glance.layout.width
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
import pl.oki.frostalert.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "FrostGlanceWidget"

class FrostGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var lastRecord: pl.oki.frostalert.data.local.TemperatureRecord? = null
        var useFahrenheit = false
        var isMataOptionEnabled = false

        try {
            val db = FrostDatabase.getDatabase(context)
            val records = try {
                db.temperatureDao().getRecentRecords().first()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read recent records: ${e.message}")
                emptyList<pl.oki.frostalert.data.local.TemperatureRecord>()
            }
            lastRecord = records.firstOrNull()

            try {
                val settings = SettingsDataStore(context).userPreferencesFlow.first()
                useFahrenheit = settings.useFahrenheit
                isMataOptionEnabled = settings.isMataOptionEnabled
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read settings for widget: ${e.message}")
            }

            Log.i(TAG, "provideGlance: recordsCount=${records.size}, lastRecordMin=${lastRecord?.minTemp}, lastHasRisk=${lastRecord?.hasRisk}")
        } catch (e: Exception) {
            Log.w(TAG, "Error preparing widget data: ${e.message}")
        }

        val riskHighLabel = context.getString(R.string.widget_risk_high_short)
        val riskLowLabel = context.getString(R.string.widget_risk_low_short)
        val noDataLabel = context.getString(R.string.widget_no_data)
        val tempLabelFormat = context.getString(R.string.widget_temp_label)
        val riskLabelFormat = context.getString(R.string.widget_risk_label)
        val refreshLabel = context.getString(R.string.widget_refresh_button)
        val mataLabel = context.getString(R.string.widget_action_mata)

        // Provide content directly (no try/catch around composable invocation)
        provideContent {
            GlanceTheme {
                WidgetContent(
                    lastRecord = lastRecord,
                    useFahrenheit = useFahrenheit,
                    isMataOptionEnabled = isMataOptionEnabled,
                    riskHighLabel = riskHighLabel,
                    riskLowLabel = riskLowLabel,
                    noDataLabel = noDataLabel,
                    tempLabelFormat = tempLabelFormat,
                    riskLabelFormat = riskLabelFormat,
                    refreshLabel = refreshLabel,
                    mataLabel = mataLabel
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(
        lastRecord: pl.oki.frostalert.data.local.TemperatureRecord?,
        useFahrenheit: Boolean,
        isMataOptionEnabled: Boolean,
        riskHighLabel: String,
        riskLowLabel: String,
        noDataLabel: String,
        tempLabelFormat: String,
        riskLabelFormat: String,
        refreshLabel: String,
        mataLabel: String
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = Color.White, night = Color.Black))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FrostAlert",
                style = TextStyle(color = ColorProvider(day = Color.Black, night = Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )

            if (lastRecord != null) {
                val date = try { SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(lastRecord.timestamp)) } catch (_: Exception) { "--" }
                val tempText = try { WeatherCalculations.formatTemperature(lastRecord.minTemp, useFahrenheit) } catch (_: Exception) { "--" }
                val riskText = if (lastRecord.frostProbability > 0) {
                    "${lastRecord.frostProbability}%"
                } else {
                    if (lastRecord.hasRisk) riskHighLabel else riskLowLabel
                }

                Text(
                    text = String.format(tempLabelFormat, tempText),
                    style = TextStyle(color = ColorProvider(day = Color.Black, night = Color.White), fontSize = 13.sp)
                )
                Text(
                    text = String.format(riskLabelFormat, riskText) + " ($date)",
                    style = TextStyle(color = ColorProvider(day = Color.Black, night = Color.White), fontSize = 12.sp)
                )
            } else {
                Text(
                    text = noDataLabel,
                    style = TextStyle(color = ColorProvider(day = Color.Black, night = Color.White), fontSize = 13.sp)
                )
            }

            Spacer(modifier = GlanceModifier.padding(top = 4.dp))

            Row {
                Button(
                    text = refreshLabel,
                    onClick = actionRunCallback<RefreshActionCallback>()
                )
                // Show "Applied mat" button only when frost risk detected and feature is enabled
                if (lastRecord?.hasRisk == true && isMataOptionEnabled) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Button(
                        text = mataLabel,
                        onClick = actionRunCallback<UsedMatActionCallback>()
                    )
                }
            }
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

/** Sets ignoreUntil to 08:00 next morning so the user is not disturbed after applying their mat. */
class UsedMatActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= Calendar.getInstance().timeInMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            SettingsDataStore(context).updateIgnoreUntil(cal.timeInMillis)
            Log.i(TAG, "UsedMatActionCallback: ignoreUntil set to ${cal.time}")
            WidgetSyncHelper.updateAll(context)
        } catch (e: Exception) {
            Log.w(TAG, "UsedMatActionCallback failed: ${e.message}")
        }
    }
}

class FrostGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FrostGlanceWidget()
}
