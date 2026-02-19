
package pl.oki.frostalert.ui.screens

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.FrostDatabase
import pl.oki.frostalert.FrostWidgetProvider
import pl.oki.frostalert.data.CurrentWeather
import pl.oki.frostalert.data.OpenMeteoApi
import pl.oki.frostalert.data.SettingsDataStore
import pl.oki.frostalert.data.TemperatureRecord
import pl.oki.frostalert.data.WeatherResponse
import pl.oki.frostalert.updateAppWidget
import pl.oki.frostalert.utils.WeatherCalculations

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val weather: WeatherResponse,
        val minTemp: Double,
        val hasFrostRisk: Boolean,
        val warningMessage: String
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf<HomeUiState?>(null) }
    var permissionMessage by remember { mutableStateOf("Oczekiwanie na pozwolenie na lokalizację...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            uiState = HomeUiState.Loading
            fetchData(context) { uiState = it }
        } else {
            permissionMessage = "Nie udzielono pozwolenia na dostęp do lokalizacji. Wejdź w ustawienia aplikacji, aby je przyznać."
            uiState = HomeUiState.Error(permissionMessage)
        }
    }

    LaunchedEffect(key1 = true) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            uiState = HomeUiState.Loading
            fetchData(context) { uiState = it }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = uiState) {
            null -> Text(text = permissionMessage, textAlign = TextAlign.Center)
            is HomeUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Pobieranie danych...", textAlign = TextAlign.Center)
            }
            is HomeUiState.Error -> Text(
                text = state.message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            is HomeUiState.Success -> {
                WeatherSuccessScreen(state)
            }
        }
    }
}

@Composable
fun WeatherSuccessScreen(state: HomeUiState.Success) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FrostWarningCard(hasRisk = state.hasFrostRisk, warningMessage = state.warningMessage)
        CurrentWeatherCard(weather = state.weather.current)
        MinTemperatureCard(minTemp = state.minTemp)
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
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning Icon",
                tint = if (hasRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = warningMessage,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (hasRisk) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun CurrentWeatherCard(weather: CurrentWeather) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherInfo(icon = getWeatherIcon(weather.weatherCode), value = "${weather.temperature}°C", label = "Temperatura")
            WeatherInfo(icon = Icons.Default.WaterDrop, value = "${weather.humidity}%", label = "Wilgotność")
            WeatherInfo(icon = Icons.Default.WaterDrop, value = "${weather.precipitation}mm", label = "Opady")
        }
    }
}

@Composable
fun MinTemperatureCard(minTemp: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Thermostat, contentDescription = "Min temperature icon", modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Prognozowana min. temp. w nocy: ", fontSize = 16.sp)
            Text(text = "%.1f°C".format(minTemp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WeatherInfo(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, fontSize = 14.sp)
    }
}

fun getWeatherIcon(weatherCode: Int): ImageVector {
    return when (weatherCode) {
        0 -> Icons.Default.WbSunny
        1, 2, 3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.Cloud
        51, 53, 55 -> Icons.Default.WaterDrop
        61, 63, 65 -> Icons.Default.AcUnit
        66, 67 -> Icons.Default.AcUnit
        71, 73, 75 -> Icons.Default.AcUnit
        77 -> Icons.Default.AcUnit
        80, 81, 82 -> Icons.Default.Bolt
        85, 86 -> Icons.Default.AcUnit
        95 -> Icons.Default.Bolt
        96, 99 -> Icons.Default.Bolt
        else -> Icons.Default.WbSunny
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private fun fetchData(context: Context, onResult: (HomeUiState) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val scope = CoroutineScope(Dispatchers.Main)

    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                scope.launch {
                    try {
                        val settingsDataStore = SettingsDataStore(context)
                        val userPreferences = settingsDataStore.userPreferencesFlow.first()

                        val tempThreshold = if (userPreferences.isAutoModeEnabled) 2.0 else userPreferences.tempThreshold
                        val humidityThreshold = if (userPreferences.isAutoModeEnabled) 80.0 else userPreferences.humidityThreshold.toDouble()
                        val precipitationThreshold = if (userPreferences.isAutoModeEnabled) 0.1 else userPreferences.precipitationThreshold

                        val weather: WeatherResponse = OpenMeteoApi.getWeather(location.latitude, location.longitude)
                        val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                        val hasRisk = WeatherCalculations.hasFrostRisk(
                            minTemp,
                            weather.current.humidity,
                            weather.current.precipitation,
                            weather.current.weatherCode,
                            tempThreshold,
                            humidityThreshold,
                            precipitationThreshold
                        )

                        val record = TemperatureRecord(
                            timestamp = System.currentTimeMillis(),
                            minTemp = minTemp,
                            hasRisk = hasRisk
                        )

                        val db = FrostDatabase.getDatabase(context)
                        db.temperatureDao().insert(record)

                        // Trigger widget update
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, FrostWidgetProvider::class.java))
                        for (appWidgetId in appWidgetIds) {
                            updateAppWidget(context, appWidgetManager, appWidgetId)
                        }

                        val warningMessage = WeatherCalculations.getWarningMessage(
                            minTemp,
                            weather.current.humidity,
                            weather.current.precipitation,
                            weather.current.weatherCode,
                            tempThreshold,
                            humidityThreshold,
                            precipitationThreshold
                        )
                        onResult(HomeUiState.Success(weather, minTemp, hasRisk, warningMessage))
                    } catch (e: Exception) {
                        onResult(HomeUiState.Error("Błąd pobierania danych pogodowych: ${e.message}"))
                    }
                }
            } else {
                onResult(HomeUiState.Error("Nie udało się pobrać lokalizacji. Sprawdź ustawienia GPS."))
            }
        }.addOnFailureListener { e ->
            onResult(HomeUiState.Error("Błąd lokalizacji: ${e.message}"))
        }
    } catch (e: SecurityException) {
        onResult(HomeUiState.Error("Brak pozwolenia na dostęp do lokalizacji."))
    }
}
