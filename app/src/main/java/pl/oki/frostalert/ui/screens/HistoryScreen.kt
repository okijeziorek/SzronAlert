package pl.oki.frostalert.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.TemperatureRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by historyViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val isPro by settingsViewModel.isPro.collectAsState()

    LaunchedEffect(Unit) {
            historyViewModel.refreshStats()
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.history_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        ) { padding ->
            if (uiState.errorMessage != null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                val records by historyViewModel.recentRecords.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.stats_season),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (records.isNotEmpty()) {
                            TextButton(onClick = { 
                                if (isPro) {
                                    scope.launch {
                                        exportToCsv(context, records)
                                    }
                                } else {
                                    settingsViewModel.launchPurchaseFlow(context as Activity)
                                }
                            }) {
                                if (!isPro) {
                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(Modifier.width(4.dp))
                                }
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.export_csv),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(Modifier.weight(1f), stringResource(R.string.stat_risk_days), uiState.monthlyStats.sumOf { it.riskDays }.toString(), MaterialTheme.colorScheme.error, Icons.Default.AcUnit)
                        StatCard(Modifier.weight(1f), stringResource(R.string.stat_avg_temp), String.format(Locale.US, "%.1f°C", uiState.absoluteMinTemp ?: 0.0), MaterialTheme.colorScheme.primary, Icons.Default.Thermostat)
                    }

                    Spacer(Modifier.height(24.dp))

                    if (uiState.monthlyStats.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.monthly_report_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        uiState.monthlyStats.forEach { stat ->
                            MonthlyStatItem(stat)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (!isPro) {
                        Spacer(Modifier.height(20.dp))
                        MonetizationBanner()
                    }

                    Spacer(Modifier.height(32.dp))

                    if (records.size < 2) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (records.isEmpty()) stringResource(R.string.history_empty) else stringResource(R.string.history_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.trend_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth().height(350.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            // Najnowsze dane po lewej: odwracamy listę tak, by index 0 było najnowszym
                            val displayRecords = records.reversed()
                            val points = displayRecords.mapIndexed { index, record ->
                                Point(index.toFloat(), record.minTemp.toFloat())
                            }

                            val minVal = displayRecords.minOf { it.minTemp }
                            val maxVal = displayRecords.maxOf { it.minTemp }
                            val yMin = (if (minVal > 0) -2.0 else minVal - 2.0).toFloat()
                            val yMax = (if (maxVal < 2) 4.0 else maxVal + 2.0).toFloat()

                            val xAxisData = AxisData.Builder()
                                .axisStepSize(100.dp)
                                .steps(points.size - 1)
                                .labelData { index -> 
                                    displayRecords.getOrNull(index)?.let {
                                        SimpleDateFormat("d/MM", Locale.getDefault()).format(Date(it.timestamp))
                                    } ?: ""
                                }
                                .labelAndAxisLinePadding(12.dp)
                                .axisLineColor(MaterialTheme.colorScheme.outlineVariant)
                                .axisLabelColor(MaterialTheme.colorScheme.onSurfaceVariant)
                                .build()

                            val yAxisData = AxisData.Builder()
                                .steps(5)
                                .labelAndAxisLinePadding(16.dp)
                                .labelData { i ->
                                    val value = yMin + (i * (yMax - yMin) / 5)
                                    String.format(Locale.US, "%.0f°", value)
                                }
                                .axisLineColor(MaterialTheme.colorScheme.outlineVariant)
                                .axisLabelColor(MaterialTheme.colorScheme.onSurfaceVariant)
                                .build()

                            val lineChartData = LineChartData(
                                linePlotData = LinePlotData(
                                    lines = listOf(
                                        Line(
                                            dataPoints = points,
                                            LineStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                width = 4f,
                                                lineType = LineType.SmoothCurve(isDotted = false)
                                            ),
                                            IntersectionPoint(color = MaterialTheme.colorScheme.primary, radius = 4.dp),
                                            SelectionHighlightPoint(color = MaterialTheme.colorScheme.error),
                                            null,
                                            SelectionHighlightPopUp()
                                        )
                                    )
                                ),
                                xAxisData = xAxisData,
                                yAxisData = yAxisData,
                                backgroundColor = Color.Transparent,
                                paddingTop = 20.dp,
                                bottomPadding = 40.dp,
                                containerPaddingEnd = 40.dp,
                                isZoomAllowed = true
                            )

                            LineChart(
                                modifier = Modifier.fillMaxSize(),
                                lineChartData = lineChartData
                            )
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
}

@Composable
fun MonthlyStatItem(stat: pl.oki.frostalert.data.repository.MonthlyStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stat.monthName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.avg_min_label, String.format(Locale.US, "%.1f°C", stat.averageMinTemp)), 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.days_count_label, stat.riskDays),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun exportToCsv(context: Context, records: List<TemperatureRecord>) {
    val csvHeader = "Data,Min Temp (C),Ryzyko Szronu\n"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    val csvBody = records.joinToString("\n") { 
        "${sdf.format(Date(it.timestamp))},${String.format(Locale.US, "%.1f", it.minTemp)},${if (it.hasRisk) "TAK" else "NIE"}"
    }

    try {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        
        val file = File(exportDir, "frost_alert_history.csv")
        file.writeText(csvHeader + csvBody)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Historia FrostAlert")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Udostępnij historię"))
    } catch (e: Exception) {
        Toast.makeText(context, "Błąd eksportu: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, title: String, value: String, color: Color, icon: ImageVector? = null) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = color
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
