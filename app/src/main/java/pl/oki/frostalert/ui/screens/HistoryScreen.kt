package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch
import pl.oki.frostalert.billing.BillingClientWrapper
import pl.oki.frostalert.data.local.FrostDatabase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = FrostDatabase.getDatabase(context)
    val dao = db.temperatureDao()
    val records by dao.getRecentRecords().collectAsState(initial = emptyList())
    
    val scope = rememberCoroutineScope()
    val billingClient = remember { BillingClientWrapper(context) }
    val isPro by billingClient.isPro.collectAsState()
    var riskCount by remember { mutableStateOf(0) }
    var avgMinTemp by remember { mutableStateOf(0.0) }

    LaunchedEffect(records) {
        scope.launch {
            riskCount = dao.getRiskCount()
            avgMinTemp = dao.getAverageMinTemp() ?: 0.0
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Historia Szronu") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    riskCount = dao.getRiskCount()
                    avgMinTemp = dao.getAverageMinTemp() ?: 0.0
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Statystyki Ostatnich Dni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(Modifier.weight(1f), "Dni z ryzykiem", riskCount.toString(), MaterialTheme.colorScheme.error)
                StatCard(Modifier.weight(1f), "Średnia temp.", String.format(Locale.US, "%.1f°C", avgMinTemp), MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(32.dp))

            if (records.isEmpty()) {
                Text("Brak danych do wyświetlenia", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    text = "Trend temperatury (od lewej: najnowsze)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(24.dp)), // Wymuszone przycinanie zawartości
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    val points = records.mapIndexed { index, record ->
                        Point(index.toFloat(), record.minTemp.toFloat())
                    }

                    val yMin = (records.minOfOrNull { it.minTemp } ?: -5.0).let { if (it > 0) -2.0 else it - 2.0 }.toFloat()
                    val yMax = (maxOf(records.maxOfOrNull { it.minTemp } ?: 5.0, 2.0) + 2.0).toFloat()

                    val xAxisData = AxisData.Builder()
                        .axisStepSize(80.dp) // Większy krok dla czytelności
                        .steps(points.size - 1)
                        .labelData { index -> 
                            records.getOrNull(index)?.let {
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
                            String.format("%.0f°", value)
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
                                    IntersectionPoint(color = MaterialTheme.colorScheme.primary, radius = 3.dp),
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
                        bottomPadding = 40.dp, // Miejsce na daty
                        containerPaddingEnd = 24.dp 
                    )

                    // Box z clipToBounds zapobiega wychodzeniu linii poza obszar karty
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                        LineChart(
                            modifier = Modifier.fillMaxSize(),
                            lineChartData = lineChartData
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            if (!isPro) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    factory = { context ->
                        AdView(context).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = "ca-app-pub-3940256099942544/6300978111"
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}
