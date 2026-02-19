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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.FrostCheckWorker
import pl.oki.frostalert.FrostDatabase
import pl.oki.frostalert.FrostWidgetProvider
import pl.oki.frostalert.data.OpenMeteoApi
import pl.oki.frostalert.data.SettingsDataStore
import pl.oki.frostalert.data.TemperatureRecord
import pl.oki.frostalert.updateAppWidget
import pl.oki.frostalert.utils.WeatherCalculations

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var weatherText by remember { mutableStateOf("Oczekiwanie na pozwolenie na lokalizację...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            weatherText = "Pobieranie danych..."
            fetchData(context) { weatherText = it }
        } else {
            weatherText = "Nie udzielono pozwolenia na dostęp do lokalizacji. Wejdź w ustawienia aplikacji, aby je przyznać."
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            weatherText = "Pobieranie danych..."
            fetchData(context) { weatherText = it }
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
        Text(text = weatherText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { 
            val testData = Data.Builder().putBoolean("IS_TEST", true).build()
            val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>()
                .setInputData(testData)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }) {
            Text("Testuj powiadomienie")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private fun fetchData(context: Context, onResult: (String) -> Unit) {
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

                        val weather = OpenMeteoApi.getWeather(location.latitude, location.longitude)
                        val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                        val hasRisk = WeatherCalculations.hasFrostRisk(minTemp, weather.current.humidity, weather.current.precipitation, weather.current.weatherCode, tempThreshold, humidityThreshold, precipitationThreshold)

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

                        onResult(WeatherCalculations.getWarningMessage(minTemp, weather.current.humidity, weather.current.precipitation, weather.current.weatherCode, tempThreshold, humidityThreshold, precipitationThreshold))
                    } catch (e: Exception) {
                        onResult("Błąd pobierania danych pogodowych: ${e.message}")
                    }
                }
            } else {
                onResult("Nie udało się pobrać lokalizacji. Sprawdź ustawienia GPS.")
            }
        }
    } catch (e: SecurityException) {
        onResult("Brak pozwolenia na dostęp do lokalizacji.")
    }
}