package pl.oki.frostalert.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.oki.frostalert.data.remote.CurrentWeather
import pl.oki.frostalert.data.remote.HourlyForecast
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    // Inicjalizacja ViewModel
    val viewModel = remember { HomeViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    Scaffold { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BatteryOptimizationWarning()

                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        Box(Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is HomeUiState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 100.dp)) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.refreshData() }) {
                                Text("Spróbuj ponownie")
                            }
                        }
                    }
                    is HomeUiState.Success -> {
                        WeatherSuccessScreen(state)
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryOptimizationWarning() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    val isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    if (!isIgnoring) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryAlert, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Niezawodne alerty", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Wyłącz optymalizację baterii dla aplikacji.", fontSize = 11.sp)
                }
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("FIX")
                }
            }
        }
    }
}

@Composable
fun WeatherSuccessScreen(state: HomeUiState.Success) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FrostWarningCard(hasRisk = state.hasFrostRisk, warningMessage = state.warningMessage)
        CurrentWeatherCard(weather = state.weather.current)
        MinTemperatureCard(minTemp = state.minTemp)
        HourlyForecastSection(hourly = state.weather.hourly)
    }
}

@Composable
fun MinTemperatureCard(minTemp: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Icon(Icons.Default.Nightlight, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Najniższa w nocy", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.1f".format(Locale.US, minTemp),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "°C",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HourlyForecastSection(hourly: HourlyForecast) {
    Column {
        Text("Następne godziny", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        LazyRow(contentPadding = PaddingValues(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(hourly.time.take(24)) { index, timeStr ->
                HourlyItem(timeStr, hourly.temperature[index], hourly.weatherCode[index])
            }
        }
    }
}

@Composable
fun HourlyItem(timeStr: String, temp: Double, code: Int) {
    val displayTime = timeStr.substringAfter("T")
    Card(
        modifier = Modifier.width(70.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(displayTime, fontSize = 10.sp)
            Icon(getWeatherIcon(code), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text("%.0f°".format(temp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FrostWarningCard(hasRisk: Boolean, warningMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasRisk) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = warningMessage,
            modifier = Modifier.padding(16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = if (hasRisk) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun CurrentWeatherCard(weather: CurrentWeather) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            WeatherItem(getWeatherIcon(weather.weatherCode), "${weather.temperature}°", "Teraz")
            WeatherItem(Icons.Default.WaterDrop, "${weather.humidity}%", "Wilgoć")
            WeatherItem(Icons.Default.Cloud, "${weather.precipitation}mm", "Opady")
        }
    }
}

@Composable
fun WeatherItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun getWeatherIcon(code: Int): ImageVector = when (code) {
    0 -> Icons.Default.WbSunny
    1, 2, 3 -> Icons.Default.Cloud
    45, 48 -> Icons.Default.Cloud
    51, 53, 55, 61, 63, 65 -> Icons.Default.WaterDrop
    66, 67, 71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit
    80, 81, 82, 95, 96, 99 -> Icons.Default.Thunderstorm
    else -> Icons.Default.WbSunny
}
