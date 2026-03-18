package pl.oki.frostalert.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.utils.SummerCalculations
import pl.oki.frostalert.utils.TrendCalculations
import pl.oki.frostalert.worker.FrostCheckWorker
import java.util.Locale
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    trendViewModel: TrendViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FrostDatabase.getDatabase(context)
    val userPrefs by settingsViewModel.userPreferences.collectAsState()
    val trendState by trendViewModel.trendState.collectAsState()
    val settingsDataStore = SettingsDataStore(context)

    // Stan dla symulatora
    var simTemp by remember { mutableStateOf(0.0f) }
    var simHumidity by remember { mutableStateOf(80.0f) }
    var simWind by remember { mutableStateOf(5.0f) }
    var simUv by remember { mutableStateOf(1.0f) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Laboratorium Dewelopera") }) }
    ) { padding ->
        if (userPrefs == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val prefs = userPrefs!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // SEKCJA 1: SYMULATOR ALGORYTMU (ZIMA + LATO)
                DebugSection("🧪 Symulator Warunków", Icons.Default.Science) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DebugSlider("Temp: ${"%.1f".format(Locale.US, simTemp)}°C", simTemp, -10f..40f) { simTemp = it }
                        DebugSlider("Wilgotność: ${simHumidity.toInt()}%", simHumidity, 0f..100f) { simHumidity = it }
                        DebugSlider("Wiatr: ${simWind.toInt()} km/h", simWind, 0f..40f) { simWind = it }
                        DebugSlider("Indeks UV: ${"%.1f".format(Locale.US, simUv)}", simUv, 0f..12f) { simUv = it }
                        
                        val hasFrostRisk = WeatherCalculations.hasFrostRisk(
                            temp = simTemp.toDouble(),
                            humidity = simHumidity.toDouble(),
                            precip = 0.0,
                            weatherCode = 0,
                            tempThreshold = prefs.tempThreshold,
                            humidityThreshold = prefs.humidityThreshold.toDouble(),
                            precipitationThreshold = prefs.precipitationThreshold,
                            sensitivity = prefs.sensitivity,
                            windSpeed = simWind.toDouble(),
                            appMode = prefs.appMode
                        )

                        val summerMsg = SummerCalculations.getSummerWarningMessage(
                            currentTemp = simTemp.toDouble(),
                            weatherCode = 0,
                            uvIndex = simUv.toDouble(),
                            heatThreshold = prefs.heatThreshold,
                            appMode = prefs.appMode
                        )

                        // Wynik Zima
                        if (simTemp < 15) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (hasFrostRisk) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = if (hasFrostRisk) "WYNIK ZIMA: RYZYKO SZRONU" else "WYNIK ZIMA: BEZPIECZNIE",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                                )
                            }
                        }

                        // Wynik Lato
                        if (simTemp >= 15 || simUv >= 3) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (summerMsg.isNotEmpty()) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = if (summerMsg.isEmpty()) "WYNIK LATO: OPTYMALNIE" else "LATO: $summerMsg",
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // SEKCJA 2: TRYB PRO & REKLAMY
                DebugSection("💰 Funkcje Biznesowe", Icons.Default.BugReport) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Wymuś status PRO (Debug)", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = prefs.isProForced,
                            onCheckedChange = { settingsViewModel.updateIsProForced(it) }
                        )
                    }
                }

                // SEKCJA 3: TREND 7-DNIOWY
                DebugSection("📈 Trend 7-dniowy", Icons.Default.TrendingUp) {
                    if (trendState.weeklyStats != null) {
                        val stats = trendState.weeklyStats!!
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ryzyko szronu: ${stats.frostRiskPercentage.toInt()}%", fontWeight = FontWeight.Bold)
                            Text("Noce z ryzykiem: ${stats.nightsWithFrostRisk}/7")
                            Text("Średnia min: ${"%.1f".format(stats.averageMinTemp)}°C")
                            Text("Najniższa: ${"%.1f".format(stats.lowestTemp)}°C")
                            Text("Trend: ${TrendCalculations.getTrendEmoji(stats.trend)}")
                            Button(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false
                            ) {
                                Text("Otwórz ekran Trendu (4. tab)")
                            }
                        }
                    } else {
                        Text("Ładowanie trendu...")
                    }
                }

                // SEKCJA 4: WORKER & POWIADOMIENIA
                DebugSection("🔔 System & Tło", Icons.Default.Info) {
                    Button(
                        onClick = {
                            val testData = Data.Builder().putBoolean("IS_TEST", true).build()
                            val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>()
                                .setInputData(testData)
                                .build()
                            WorkManager.getInstance(context).enqueue(workRequest)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Testowe powiadomienie")
                    }

                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val random = Random()
                                val now = System.currentTimeMillis()
                                for (i in 0 until 14) {
                                    val temp = -8.0 + random.nextDouble() * 15.0
                                    db.temperatureDao().insert(TemperatureRecord(
                                        timestamp = now - (i * 24 * 60 * 60 * 1000L),
                                        minTemp = temp,
                                        hasRisk = temp < 1.0
                                    ))
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Dodano 14 dni historii", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generuj historię")
                    }

                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) { db.clearAllTables() }
                            Toast.makeText(context, "Baza wyczyszczona", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Wyczyść wszystko")
                    }
                }

                // SEKCJA 5: KALIBRACJA ALGORYTMU
                DebugSection("🎯 Kalibracja Algorytmu", Icons.Default.Science) {
                    val calibrationViewModel: CalibrationViewModel = hiltViewModel()
                    val calibrationState by calibrationViewModel.uiState.collectAsState()

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (calibrationState.calibrationResult != null) {
                            val result = calibrationState.calibrationResult!!
                            Text("Feedbacków: ${result.totalFeedback}", fontWeight = FontWeight.Bold)
                            Text("Dokładność: ${result.accuracyPercentage.toInt()}%", color = MaterialTheme.colorScheme.primary)
                            Text("Poziom ufności: ${result.confidenceLevel}", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text("Brak danych kalibracji", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    db.calibrationDao().clearAllFeedback()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Kalibracja wyczyszczona", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Wyczyść feedback")
                        }
                    }
                }

                // SEKCJA 6: STATYSTYKI BAZY DANYCH
                DebugSection("💾 Baza Danych", Icons.Default.Info) {
                    var dbStats by remember { mutableStateOf<Map<String, Int>?>(null) }

                    LaunchedEffect(Unit) {
                        scope.launch(Dispatchers.IO) {
                            val tempCount = db.temperatureDao().getAllRecords().size
                            val feedbackCount = db.calibrationDao().getTotalFeedbackCount()
                            val riskCount = db.temperatureDao().getRiskCount()
                            withContext(Dispatchers.Main) {
                                dbStats = mapOf(
                                    "Rekordów temp" to tempCount,
                                    "Feedbacków" to feedbackCount,
                                    "Ryzyk szronu" to riskCount
                                )
                            }
                        }
                    }

                    if (dbStats != null) {
                        dbStats!!.forEach { (label, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text(count.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text("Ładowanie statystyk...", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                db.temperatureDao().insert(TemperatureRecord(
                                    timestamp = System.currentTimeMillis(),
                                    minTemp = simTemp.toDouble(),
                                    hasRisk = WeatherCalculations.hasFrostRisk(
                                        temp = simTemp.toDouble(),
                                        humidity = simHumidity.toDouble(),
                                        precip = 0.0,
                                        weatherCode = 0,
                                        tempThreshold = prefs.tempThreshold,
                                        humidityThreshold = prefs.humidityThreshold.toDouble(),
                                        precipitationThreshold = prefs.precipitationThreshold,
                                        sensitivity = prefs.sensitivity,
                                        windSpeed = simWind.toDouble(),
                                        appMode = prefs.appMode
                                    )
                                ))
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Dodano rekord testowy", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dodaj rekord z symulatora")
                    }
                }

                // SEKCJA 7: POWIADOMIENIA I AKCJE
                DebugSection("🔔 Test Powiadomień", Icons.Default.Notifications) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val testData = Data.Builder().putBoolean("IS_TEST", true).build()
                                val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>()
                                    .setInputData(testData)
                                    .build()
                                WorkManager.getInstance(context).enqueue(workRequest)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Wyślij testowe powiadomienie")
                        }

                        Button(
                            onClick = {
                                // Symuluj akcję "Ignoruj dziś"
                                settingsViewModel.updateIgnoreUntil(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                                Toast.makeText(context, "Ustawiono 'Ignoruj dziś'", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Test: Ignoruj dziś")
                        }

                        Button(
                            onClick = {
                                // Symuluj akcję "Przypomnij za 2h"
                                val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>()
                                    .setInitialDelay(2, java.util.concurrent.TimeUnit.HOURS)
                                    .build()
                                WorkManager.getInstance(context).enqueue(workRequest)
                                Toast.makeText(context, "Zaplanowano przypomnienie za 2h", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Test: Przypomnij za 2h")
                        }
                    }
                }

                // SEKCJA 8: TRYB APLIKACJI
                DebugSection("🔄 Tryb Aplikacji", Icons.Default.SwapHoriz) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Aktualny tryb: ${if (prefs.appMode == 0) "Samochód" else "Ogród"}", fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { settingsViewModel.updateAppMode(0) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (prefs.appMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text("🚗 Samochód")
                            }

                            Button(
                                onClick = { settingsViewModel.updateAppMode(1) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (prefs.appMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text("🌱 Ogród")
                            }
                        }
                    }
                }

                // SEKCJA 9: TEST GEOFENCING
                DebugSection("🗺️ Test Geofencing", Icons.Default.Info) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Symuluj sprawdzenie geofencing (wymaga włączonej opcji w ustawieniach)", style = MaterialTheme.typography.bodySmall)

                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    // Symuluj lokalizację (np. Warszawa)
                                    val mockLocation = android.location.Location("mock").apply {
                                        latitude = 52.2297
                                        longitude = 21.0122
                                    }
                                    val locationRepo = pl.oki.frostalert.data.repository.LocationRepository(context, settingsDataStore)
                                    val result = locationRepo.checkGeofencingRisk(mockLocation, prefs)
                                    withContext(Dispatchers.Main) {
                                        val msg = when (result) {
                                            is pl.oki.frostalert.data.repository.GeofencingResult.NoRisk -> "Brak wyższego ryzyka w okolicy"
                                            is pl.oki.frostalert.data.repository.GeofencingResult.HigherRiskNearby -> "Wyższe ryzyko w kierunku ${result.direction}: +${(result.riskIncrease * 100).toInt()}%"
                                            is pl.oki.frostalert.data.repository.GeofencingResult.Error -> "Błąd: ${result.message}"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Geofencing (Warszawa)")
                        }

                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    // Symuluj inną lokalizację (np. Kraków)
                                    val mockLocation = android.location.Location("mock").apply {
                                        latitude = 50.0647
                                        longitude = 19.9450
                                    }
                                    val locationRepo = pl.oki.frostalert.data.repository.LocationRepository(context, settingsDataStore)
                                    val result = locationRepo.checkGeofencingRisk(mockLocation, prefs)
                                    withContext(Dispatchers.Main) {
                                        val msg = when (result) {
                                            is pl.oki.frostalert.data.repository.GeofencingResult.NoRisk -> "Brak wyższego ryzyka w okolicy"
                                            is pl.oki.frostalert.data.repository.GeofencingResult.HigherRiskNearby -> "Wyższe ryzyko w kierunku ${result.direction}: +${(result.riskIncrease * 100).toInt()}%"
                                            is pl.oki.frostalert.data.repository.GeofencingResult.Error -> "Błąd: ${result.message}"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Geofencing (Kraków)")
                        }

                                        Spacer(Modifier.height(8.dp))
                                        var showGeofenceHistory by remember { mutableStateOf(false) }
                                        Button(
                                            onClick = { showGeofenceHistory = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Otwórz historię Geofence")
                                        }

                                        if (showGeofenceHistory) {
                                            Dialog(onDismissRequest = { showGeofenceHistory = false }) {
                                                Surface(modifier = Modifier.fillMaxSize()) {
                                                    GeofenceHistoryScreen()
                                                }
                                            }
                                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebugSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
fun DebugSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}
