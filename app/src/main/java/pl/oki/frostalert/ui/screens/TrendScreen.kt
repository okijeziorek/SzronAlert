package pl.oki.frostalert.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pl.oki.frostalert.R
import pl.oki.frostalert.utils.TrendCalculations
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendScreen(
    trendViewModel: TrendViewModel = hiltViewModel()
) {
    val trendState by trendViewModel.trendState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trend_screen_title)) },
                navigationIcon = {
                    Icon(
                        Icons.Default.TrendingUp,
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
                            text = trendState.errorMessage ?: "Nieznany błąd",
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

                    // SEKCJA 2: WYKRES TEMPERATURY
                    TrendTemperatureChart(stats)

                    // SEKCJA 3: SZCZEGÓŁOWE NOCE
                    TrendNightsDetail(stats)

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

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
                Column {
                    Text(
                        text = TrendCalculations.getTrendDescription(stats),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = TrendCalculations.getTrendEmoji(stats.trend),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "${stats.frostRiskPercentage.toInt()}%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Szczegóły
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrendStatItem(
                    icon = Icons.Default.Bedtime,
                    label = "Noce z ryzykiem",
                    value = "${stats.nightsWithFrostRisk}/7"
                )
                TrendStatItem(
                    icon = Icons.Default.Thermostat,
                    label = "Średnia min",
                    value = "${"%.1f".format(stats.averageMinTemp)}°C"
                )
                TrendStatItem(
                    icon = Icons.Default.AcUnit,
                    label = "Najniższa",
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
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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
                Text("Brak danych do wyświetlenia", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Temperatura nocna (7 dni)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Znajdź zakres temperatur dla lepszej wizualizacji
            val minTemp = stats.trendPoints.minOf { it.minTemp }
            val maxTemp = stats.trendPoints.maxOf { it.minTemp }
            val tempRange = maxOf(maxTemp - minTemp, 5.0) // Minimum 5°C zakresu dla czytelności

            // Dodaj padding dla wartości ekstremalnych
            val chartMin = minTemp - 2.0
            val chartMax = maxTemp + 2.0
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
                        // Linie siatki poziomej
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = (i.toFloat() / gridLines) * 180f
                            drawLine(
                                color = outlineColor,
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Punkty temperatury
                        stats.trendPoints.forEachIndexed { index, point ->
                            val x = (index.toFloat() / maxOf(stats.trendPoints.size - 1, 1)) * 300f + 20f
                            val y = 160f - ((point.minTemp - chartMin) / chartRange * 140f).toFloat()

                            // Punkt
                            drawCircle(
                                color = when {
                                    point.minTemp < 0 -> errorColor
                                    point.minTemp < 5 -> primaryColor
                                    else -> secondaryColor
                                },
                                radius = 6f,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )

                            // Obrys punktu
                            drawCircle(
                                color = backgroundColor,
                                radius = 8f,
                                center = androidx.compose.ui.geometry.Offset(x, y),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                            )
                        }

                        // Linia łącząca punkty
                        val path = androidx.compose.ui.graphics.Path()
                        stats.trendPoints.forEachIndexed { index, point ->
                            val x = (index.toFloat() / maxOf(stats.trendPoints.size - 1, 1)) * 300f + 20f
                            val y = 160f - ((point.minTemp - chartMin) / chartRange * 140f).toFloat()

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Zakres temperatur
            Text(
                text = "Zakres: ${"%.1f".format(minTemp)}°C - ${"%.1f".format(maxTemp)}°C",
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
                "Szczegóły nocy",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
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
                                fontWeight = FontWeight.Bold
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
                                Badge(
                                    modifier = Modifier.size(24.dp),
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Icon(
                                        Icons.Default.AcUnit,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
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
                        Divider()
                    }
                }
            }
        }
    }
}
