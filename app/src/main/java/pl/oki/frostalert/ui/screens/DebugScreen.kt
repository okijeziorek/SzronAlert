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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.worker.FrostCheckWorker
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FrostDatabase.getDatabase(context)
    val userPrefs by settingsViewModel.userPreferences.collectAsState()

    // Stan dla symulatora
    var simTemp by remember { mutableStateOf(0.0f) }
    var simHumidity by remember { mutableStateOf(80.0f) }
    var simWind by remember { mutableStateOf(5.0f) }

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
                // SEKCJA 1: SYMULATOR ALGORYTMU
                DebugSection("🧪 Symulator Algorytmu", Icons.Default.Science) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DebugSlider("Temp: ${"%.1f".format(simTemp)}°C", simTemp, -10f..15f) { simTemp = it }
                        DebugSlider("Wilgotność: ${simHumidity.toInt()}%", simHumidity, 0f..100f) { simHumidity = it }
                        DebugSlider("Wiatr: ${simWind.toInt()} km/h", simWind, 0f..30f) { simWind = it }
                        
                        val hasRisk = WeatherCalculations.hasFrostRisk(
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

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasRisk) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = if (hasRisk) "WYNIK: RYZYKO SZRONU" else "WYNIK: BEZPIECZNIE",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
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
                    Text(
                        "Odblokowuje eksport CSV i usuwa reklamy bez płacenia.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                            }
                            Toast.makeText(context, "Dodano 14 dni historii", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generuj 2 tygodnie historii")
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
