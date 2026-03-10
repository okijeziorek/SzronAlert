package pl.oki.frostalert.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.data.remote.HourlyForecast
import pl.oki.frostalert.utils.WeatherCalculations
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val userPrefs by settingsViewModel.userPreferences.collectAsState()
    val scope = rememberCoroutineScope()

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

                if (userPrefs == null) {
                    Box(Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (val state = uiState) {
                        is HomeUiState.Loading -> {
                            Box(Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is HomeUiState.Error -> {
                            ErrorState(state.message) { viewModel.refreshData() }
                        }
                        is HomeUiState.Success -> {
                            WeatherSuccessContent(state, viewModel, settingsViewModel, userPrefs!!, scope)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherSuccessContent(
    state: HomeUiState.Success,
    viewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    userPrefs: UserPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val isGarden = state.appMode == 1
    val showFeedback = remember(userPrefs.lastFeedbackTimestamp) {
        val lastFeedback = userPrefs.lastFeedbackTimestamp
        val now = System.currentTimeMillis()
        (now - lastFeedback) > (12 * 60 * 60 * 1000)
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGarden) Icons.Default.Agriculture else Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = if (isGarden) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isGarden) stringResource(R.string.home_garden_title) else stringResource(R.string.home_car_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        FrostWarningCard(
            hasRisk = state.hasFrostRisk,
            warningMessage = state.warningMessage,
            windSpeed = state.weather.current.windSpeed,
            isGarden = isGarden
        )

        if (isGarden && state.hasFrostRisk) {
            GardenAdviceCard(state.minTemp)
        }

        if (showFeedback) {
            FeedbackSection(onCorrection = { isPositive ->
                if (!isPositive) {
                    val adjustment = if (state.hasFrostRisk) -0.1 else 0.1
                    settingsViewModel.updateSensitivity((userPrefs.sensitivity + adjustment).coerceIn(0.5, 2.0))
                }
                settingsViewModel.updateLastFeedbackTimestamp(System.currentTimeMillis())
                viewModel.refreshData()
            })
        }

        MinTemperatureCard(minTemp = state.minTemp, useFahrenheit = state.useFahrenheit, isGarden = isGarden)
        HourlyForecastSection(hourly = state.weather.hourly, useFahrenheit = state.useFahrenheit)
    }
}

@Composable
fun GardenAdviceCard(minTemp: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Eco, 
                contentDescription = null, 
                modifier = Modifier.size(32.dp), 
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(R.string.garden_status_label), 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    WeatherCalculations.getGardenTip(minTemp), 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun FeedbackSection(onCorrection: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.feedback_question), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onCorrection(true) }) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.feedback_yes))
                }
                OutlinedButton(onClick = { onCorrection(false) }) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.feedback_no))
                }
            }
            Text(stringResource(R.string.feedback_calibration), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FrostWarningCard(hasRisk: Boolean, warningMessage: String, windSpeed: Double, isGarden: Boolean) {
    val containerColor = when {
        isGarden && hasRisk -> MaterialTheme.colorScheme.errorContainer
        isGarden -> MaterialTheme.colorScheme.tertiaryContainer
        hasRisk -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    
    val icon = if (hasRisk) (if (isGarden) Icons.Default.Warning else Icons.Default.AcUnit) else Icons.Default.CheckCircle
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp), 
                tint = if (hasRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isGarden && hasRisk) warningMessage.replace("Wysokie ryzyko szronu!", stringResource(R.string.risk_garden)) else warningMessage,
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.ExtraBold, 
                textAlign = TextAlign.Center,
                color = if (hasRisk) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
            
            if (windSpeed > 10.0) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), 
                    shape = CircleShape
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Air, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.wind_speed_format, "%.1f".format(Locale.US, windSpeed)), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun MinTemperatureCard(minTemp: Double, useFahrenheit: Boolean, isGarden: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGarden) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isGarden) stringResource(R.string.night_min_garden) else stringResource(R.string.night_min_temp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val tempValue = if (useFahrenheit) WeatherCalculations.celsiusToFahrenheit(minTemp) else minTemp
                Text(
                    text = "%.1f%s".format(Locale.US, tempValue, if (useFahrenheit) "°F" else "°C"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = if (isGarden) Icons.Default.NaturePeople else Icons.Default.Nightlight, 
                contentDescription = null, 
                modifier = Modifier.size(40.dp), 
                tint = if (isGarden) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun HourlyForecastSection(hourly: HourlyForecast, useFahrenheit: Boolean) {
    Column {
        Text(stringResource(R.string.hourly_forecast), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(hourly.time.take(24)) { index, timeStr ->
                val temp = hourly.temperature[index]
                val displayTemp = if (useFahrenheit) WeatherCalculations.celsiusToFahrenheit(temp) else temp
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(timeStr.substringAfter("T"), style = MaterialTheme.typography.labelSmall)
                    Icon(getWeatherIcon(hourly.weatherCode[index]), contentDescription = null, modifier = Modifier.size(24.dp).padding(vertical = 4.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("%.0f°".format(Locale.US, displayTemp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BatteryOptimizationWarning() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    val isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) powerManager.isIgnoringBatteryOptimizations(context.packageName) else true

    if (!isIgnoring) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryAlert, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.battery_warning_title), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(stringResource(R.string.battery_warning_desc), fontSize = 11.sp)
                }
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:${context.packageName}") }
                    context.startActivity(intent)
                }) { Text(stringResource(R.string.battery_fix_btn)) }
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry_button)) }
    }
}

fun getWeatherIcon(code: Int): ImageVector = when (code) {
    0 -> Icons.Default.WbSunny
    1, 2, 3 -> Icons.Default.Cloud
    45, 48 -> Icons.Default.WbCloudy
    in 51..67 -> Icons.Default.WaterDrop
    in 71..77 -> Icons.Default.AcUnit
    in 80..82 -> Icons.Default.BeachAccess
    else -> Icons.Default.Cloud
}
