package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.BarChart
import co.yml.charts.ui.barchart.models.BarChartData
import co.yml.charts.ui.barchart.models.BarData
import co.yml.charts.ui.barchart.models.BarStyle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch
import pl.oki.frostalert.BillingClientWrapper
import pl.oki.frostalert.FrostDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        topBar = { CenterAlignedTopAppBar(title = { Text("Historia") }) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Statystyki sezonu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard("Liczba ryzyk", riskCount.toString())
                StatCard("Średnia min. temp", String.format(Locale.US, "%.1f °C", avgMinTemp))
            }

            Spacer(Modifier.height(24.dp))

            if (records.isEmpty()) {
                Text(
                    "Brak danych w historii",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Minimalne temperatury w nocy",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(12.dp))

                        val yMin = (records.minOfOrNull { it.minTemp } ?: -5.0) - 2.0
                        val yMax = (records.maxOfOrNull { it.minTemp } ?: 5.0) + 2.0
                        val range = yMax - yMin

                        val yAxisData = AxisData.Builder()
                            .axisOffset(10.dp)
                            .steps(6)
                            .labelAndAxisLinePadding(12.dp)
                            .axisLabelColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            .axisLineColor(Color.Transparent)
                            .backgroundColor(Color.Transparent)
                            .labelData { i ->
                                val value = yMin + (i * range / 6.0)
                                if (value >= -0.1 && value <= 0.1) "0°" else String.format("%.0f°", value)
                            }
                            .build()

                        val xAxisData = AxisData.Builder()
                            .axisOffset(10.dp)
                            .labelAndAxisLinePadding(14.dp)
                            .axisStepSize(38.dp)
                            .axisLabelColor(MaterialTheme.colorScheme.onSurfaceVariant)
                            .axisLineColor(Color.Transparent)
                            .labelData { index ->
                                records.getOrNull(index)?.let {
                                    SimpleDateFormat("d MMM", Locale("pl", "PL")).format(Date(it.timestamp))
                                } ?: ""
                            }
                            .build()

                        val primary = MaterialTheme.colorScheme.primary
                        val error = MaterialTheme.colorScheme.error

                        val barData = records.mapIndexed { index, record ->
                            val baseColor = if (record.hasRisk) error else primary
                            val gradientColors = listOf(
                                baseColor.copy(alpha = 0.95f),
                                baseColor.copy(alpha = 0.65f),
                                baseColor.copy(alpha = 0.25f)
                            )

                            BarData(
                                point = Point(index.toFloat(), record.minTemp.toFloat()),
                                label = "",
                                color = baseColor,
                                gradientColorList = gradientColors,
                                description = record.minTemp.toString() + "°"
                            )
                        }

                        BarChart(
                            modifier = Modifier
                                .fillMaxWidth(),
                            barChartData = BarChartData(
                                chartData = barData,
                                xAxisData = xAxisData,
                                yAxisData = yAxisData,
                                backgroundColor = Color.Transparent,
                                paddingEnd = 16.dp,
                                paddingTop = 8.dp,
                                barStyle = BarStyle(
                                    isGradientEnabled = true,
                                    selectionHighlightData = null
                                ),
                                showYAxis = true,
                                showXAxis = true,
                                horizontalExtraSpace = 12.dp
                            )
                        )

                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(primary, "Bezpieczna noc")
                            Spacer(Modifier.width(24.dp))
                            LegendItem(error, "Ryzyko przymrozku")
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (!isPro) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        AdView(context).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Ad ID
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
