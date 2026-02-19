package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
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
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Historia") })
        },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Statystyki sezonu", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Liczba ryzyk: $riskCount")
            Text("Średnia min temp: ${String.format(Locale.US, "%.1f", avgMinTemp)} °C")

            Spacer(Modifier.height(24.dp))

            if (records.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Historia temperatur", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        val maxRange = (records.maxOfOrNull { it.minTemp }?.toInt() ?: 0) + 1
                        val minRange = (records.minOfOrNull { it.minTemp }?.toInt() ?: 0) - 1

                        val yAxisData = AxisData.Builder()
                            .steps(4)
                            .backgroundColor(Color.Transparent)
                            .axisLabelColor(MaterialTheme.colorScheme.onSurface)
                            .axisLineColor(MaterialTheme.colorScheme.outline)
                            .labelData { i ->
                                val value = minRange + (i * (maxRange - minRange) / 4.0)
                                String.format(Locale.US, "%.1f°", value)
                            }
                            .build()

                        val xAxisData = AxisData.Builder()
                            .axisStepSize(30.dp)
                            .backgroundColor(Color.Transparent)
                            .axisLabelColor(MaterialTheme.colorScheme.onSurface)
                            .axisLineColor(MaterialTheme.colorScheme.outline)
                            .labelData { index ->
                                records.getOrNull(index)?.let {
                                    SimpleDateFormat("d/M", Locale.getDefault()).format(Date(it.timestamp))
                                } ?: ""
                            }
                            .build()

                        val barData = records.mapIndexed { index, record ->
                            val color = if (record.hasRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            BarData(
                                point = Point(index.toFloat(), record.minTemp.toFloat()),
                                label = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(record.timestamp)),
                                color = color,
                                gradientColorList = listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.1f))
                            )
                        }

                        val barChartData = BarChartData(
                            chartData = barData,
                            xAxisData = xAxisData,
                            yAxisData = yAxisData,
                            backgroundColor = Color.Transparent,
                            barStyle = BarStyle(
                                barWidth = 20.dp
                            )
                        )
                        BarChart(modifier = Modifier.fillMaxSize(), barChartData = barChartData)
                    }
                }
            } else {
                Text("Brak danych w historii")
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
