package pl.oki.frostalert.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pl.oki.frostalert.R
import pl.oki.frostalert.utils.TrendCalculations
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.drawscope.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendSummaryCard(stats: TrendCalculations.WeeklyTrendStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                stats.frostRiskPercentage > 70 -> MaterialTheme.colorScheme.errorContainer
                stats.frostRiskPercentage > 40 -> MaterialTheme.colorScheme.primaryContainer
                stats.frostRiskPercentage > 0 -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nagłówek z trendami
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = TrendCalculations.getTrendDescription(stats),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = TrendCalculations.getTrendEmoji(stats.trend),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${stats.frostRiskPercentage.toInt()}%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Szczegóły
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrendStatItem(
                    icon = Icons.Default.Bedtime,
                    label = stringResource(R.string.trend_stat_nights_with_risk),
                    value = "${stats.nightsWithFrostRisk}/${stats.trendPoints.size}"
                )
                TrendStatItem(
                    icon = Icons.Default.Thermostat,
                    label = stringResource(R.string.trend_stat_avg_min),
                    value = "${"%.1f".format(stats.averageMinTemp)}°C"
                )
                TrendStatItem(
                    icon = Icons.Default.AcUnit,
                    label = stringResource(R.string.trend_stat_lowest),
                    value = "${"%.1f".format(stats.lowestTemp)}°C"
                )
            }
        }
    }
}

@Composable
fun TrendStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrendTemperatureChart(stats: TrendCalculations.WeeklyTrendStats) {
    if (stats.trendPoints.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.trend_chart_no_data), style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.trend_chart_temperature_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))

            val minTemp = stats.trendPoints.minOf { it.minTemp }
            val maxTemp = stats.trendPoints.maxOf { it.minTemp }
            val chartMin = minTemp - 1.5
            val chartMax = maxTemp + 1.5
            val chartRange = chartMax - chartMin

            // Oś Y z temperaturami
            Row(modifier = Modifier.fillMaxWidth()) {
                // Pobierz kolory przed Canvas
                val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                val backgroundColor = MaterialTheme.colorScheme.background
                val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                val errorColor = MaterialTheme.colorScheme.error
                val secondaryColor = MaterialTheme.colorScheme.secondary

                // Wykres
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    // Wszystkie elementy wykresu w jednym Canvas
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val topPadding = 12f
                        val bottomPadding = 12f
                        val leftPadding = 18f
                        val rightPadding = 18f
                        val plotHeight = size.height - topPadding - bottomPadding
                        val plotWidth = size.width - leftPadding - rightPadding

                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = topPadding + (i.toFloat() / gridLines) * plotHeight
                            drawLine(
                                color = outlineColor,
                                start = androidx.compose.ui.geometry.Offset(leftPadding, y),
                                end = androidx.compose.ui.geometry.Offset(size.width - rightPadding, y),
                                strokeWidth = 1f
                            )
                        }

                        val chartPoints = stats.trendPoints.mapIndexed { index, point ->
                            val divisor = maxOf(stats.trendPoints.size - 1, 1)
                            val x = leftPadding + (index.toFloat() / divisor) * plotWidth
                            val normalized = ((point.minTemp - chartMin) / chartRange).coerceIn(0.0, 1.0)
                            val y = topPadding + ((1 - normalized).toFloat() * plotHeight)
                            androidx.compose.ui.geometry.Offset(x, y)
                        }

                        val path = androidx.compose.ui.graphics.Path()
                        chartPoints.forEachIndexed { index, offset ->
                            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                        }

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )

                        stats.trendPoints.forEachIndexed { index, point ->
                            val offset = chartPoints[index]
                            drawCircle(
                                color = when {
                                    point.minTemp < 0 -> errorColor
                                    point.minTemp < 5 -> primaryColor
                                    else -> secondaryColor
                                },
                                radius = 6f,
                                center = offset
                            )
                            drawCircle(
                                color = backgroundColor,
                                radius = 8f,
                                center = offset,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Oś X – etykiety dni
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                stats.trendPoints.forEach { point ->
                    Text(
                        text = point.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Zakres temperatur
            Text(
                text = stringResource(
                    R.string.trend_chart_range,
                    String.format(Locale.US, "%.1f", minTemp),
                    String.format(Locale.US, "%.1f", maxTemp)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TrendNightsDetail(stats: TrendCalculations.WeeklyTrendStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.trend_night_details_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(stats.trendPoints) { index, point ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = point.dayLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = TrendCalculations.getDayOfWeekShort(point.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${"%.1f".format(point.minTemp)}°C",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    point.minTemp < 0 -> MaterialTheme.colorScheme.error
                                    point.minTemp < 5 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )

                            if (point.hasFrostRisk) {
                                Icon(
                                    Icons.Default.AcUnit,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    if (index < stats.trendPoints.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun FutureTrendSection(
    trendViewModel: TrendViewModel,
    futureTrendState: FutureTrendUiState,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userPrefs by settingsViewModel.userPreferences.collectAsState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.trend_future_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))

            if (userPrefs != null) {
                val prefs = userPrefs!!
                val lat = if (prefs.isManualLocationEnabled) prefs.manualLatitude else 52.2297 // domyślnie Warszawa
                val lon = if (prefs.isManualLocationEnabled) prefs.manualLongitude else 21.0122

                Button(
                    onClick = { trendViewModel.loadFutureTrend(lat, lon) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.trend_future_load_button), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(Modifier.height(12.dp))
            }

            when {
                futureTrendState.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                futureTrendState.errorMessage != null -> {
                    Text(
                        text = futureTrendState.errorMessage ?: stringResource(R.string.trend_unknown_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                futureTrendState.futureWeeklyStats != null -> {
                    val stats = futureTrendState.futureWeeklyStats!!
                    Text(
                        text = stringResource(R.string.trend_future_loaded, TrendCalculations.getTrendEmoji(stats.trend)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            exportTrendToCsv(
                                context = context,
                                stats = stats,
                                fileName = "frost_alert_future_trend.csv",
                                subject = context.getString(R.string.trend_export_future_subject),
                                chooserTitle = context.getString(R.string.trend_export_future_chooser)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.trend_export_future_button), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.trend_future_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun TrendScreen(
    trendViewModel: TrendViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val trendState by trendViewModel.trendState.collectAsState()
    val futureTrendState by trendViewModel.futureTrendState.collectAsState()
    val isPro by settingsViewModel.isPro.collectAsState()
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.trend_screen_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    Icon(
                            Icons.Default.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        when {
            trendState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            trendState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = trendState.errorMessage ?: stringResource(R.string.trend_unknown_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            trendState.weeklyStats != null -> {
                val stats = trendState.weeklyStats!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SEKCJA 1: STATYSTYKA OGÓLNA
                    TrendSummaryCard(stats)

                    if (!isPro) {
                        MonetizationBanner()
                    }

                    // SEKCJA 2: WYKRES TEMPERATURY
                    TrendTemperatureChart(stats)

                    // SEKCJA 3: SZCZEGÓŁOWE NOCE
                    TrendNightsDetail(stats)

                    // SEKCJA 4: PRZYSZŁY TREND
                    FutureTrendSection(trendViewModel, futureTrendState)

                    // SEKCJA 5: EKSPORT DANYCH
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.trend_export_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                exportTrendToCsv(
                                    context = ctx,
                                    stats = stats,
                                    fileName = "frost_alert_trend.csv",
                                    subject = ctx.getString(R.string.trend_export_subject),
                                    chooserTitle = ctx.getString(R.string.trend_export_chooser)
                                )
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.trend_export_button), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun exportTrendToCsv(
    context: Context,
    stats: TrendCalculations.WeeklyTrendStats,
    fileName: String,
    subject: String,
    chooserTitle: String
) {
    val header = context.getString(R.string.trend_export_csv_header) + "\n"
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val body = stats.trendPoints.joinToString("\n") { point ->
        val risk = if (point.hasFrostRisk) {
            context.getString(R.string.trend_export_risk_yes)
        } else {
            context.getString(R.string.trend_export_risk_no)
        }
        val date = sdf.format(Date(point.timestamp))
        "${point.dayLabel},$date,${String.format(Locale.US, "%.1f", point.minTemp)},$risk"
    }

    try {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, fileName)
        file.writeText(header + body)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.trend_export_error_with_reason, e.message ?: "?"), Toast.LENGTH_LONG).show()
    }
}
