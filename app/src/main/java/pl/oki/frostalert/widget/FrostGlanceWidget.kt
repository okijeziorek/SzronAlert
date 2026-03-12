package pl.oki.frostalert.widget

import android.content.Context
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

class FrostGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = FrostDatabase.getDatabase(context)
        val lastRecord = db.temperatureDao().getRecentRecords().first().firstOrNull()
        val settings = SettingsDataStore(context).userPreferencesFlow.first()

        provideContent {
            GlanceTheme {
                WidgetContent(lastRecord, settings.useFahrenheit)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(lastRecord: pl.oki.frostalert.data.local.TemperatureRecord?, useFahrenheit: Boolean) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lastRecord != null) {
                val sdf = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
                val date = sdf.format(Date(lastRecord.timestamp))
                val riskText = if (lastRecord.hasRisk) "Wysokie" else "Niskie"
                val riskColor = if (lastRecord.hasRisk) GlanceTheme.colors.error else GlanceTheme.colors.primary

                Text(
                    text = "Szron Alert ($date)",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(GlanceModifier.padding(vertical = 4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Noc min: ",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    )
                    Text(
                        text = WeatherCalculations.formatTemperature(lastRecord.minTemp, useFahrenheit),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Text(
                    text = "Ryzyko: $riskText",
                    style = TextStyle(color = riskColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            } else {
                Text(
                    text = "Brak danych. Kliknij odśwież.",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp)
                )
            }

            Spacer(GlanceModifier.padding(vertical = 4.dp))
            
            Button(
                text = "Odśwież",
                onClick = actionRunCallback<RefreshActionCallback>()
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
        // Uruchamiamy WorkManager, aby pobrać świeże dane
        val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "manual_widget_refresh",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        // Widget zaktualizuje się sam, gdy Worker skończy pracę i wywoła updateAll
    }
}

class FrostGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FrostGlanceWidget()
}
