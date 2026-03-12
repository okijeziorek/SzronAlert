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
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.utils.SummerCalculations
import pl.oki.frostalert.worker.FrostCheckWorker
import java.util.Locale
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FrostDatabase.getDatabase(context)
    val userPrefs by settingsViewModel.userPreferences.collectAsState()

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

                // SEKCJA 3: WORKER & POWIADOMIENIA
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
