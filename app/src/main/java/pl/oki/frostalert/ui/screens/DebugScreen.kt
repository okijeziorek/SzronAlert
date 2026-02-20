package pl.oki.frostalert.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.worker.FrostCheckWorker
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = SettingsDataStore(context)
    val db = FrostDatabase.getDatabase(context)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Menu Debugowania") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DebugSection("Powiadomienia i WorkManager") {
                Button(
                    onClick = {
                        val testData = Data.Builder().putBoolean("IS_TEST", true).build()
                        val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>()
                            .setInputData(testData)
                            .build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                        Toast.makeText(context, "Zlecono testowe powiadomienie", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wyślij testowe powiadomienie")
                }

                Button(
                    onClick = {
                        val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>().build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                        Toast.makeText(context, "Uruchomiono pełny proces sprawdzania", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Uruchom proces sprawdzania (Real)")
                }
            }

            DebugSection("Baza Danych i Historia") {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val random = Random()
                            val now = System.currentTimeMillis()
                            for (i in 0 until 7) {
                                val temp = -5.0 + random.nextDouble() * 15.0
                                val record = TemperatureRecord(
                                    timestamp = now - (i * 24 * 60 * 60 * 1000),
                                    minTemp = temp,
                                    hasRisk = temp < 2.0
                                )
                                db.temperatureDao().insert(record)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Dodano 7 rekordów", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dodaj losowe dane do historii")
                }

                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.clearAllTables()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Historia wyczyszczona", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wyczyść całą historię")
                }
            }

            DebugSection("Ustawienia") {
                Button(
                    onClick = {
                        scope.launch {
                            dataStore.updateIgnoreUntil(0L)
                            Toast.makeText(context, "Zresetowano ignorowanie", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resetuj \"Ignoruj do rana\"")
                }
            }

            DebugSection("Informacje o Systemie") {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isIgnoring = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                } else true

                Text("Model: ${android.os.Build.MODEL}", fontSize = 12.sp)
                Text("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})", fontSize = 12.sp)
                Text("Optymalizacja baterii wyłączona: ${if (isIgnoring) "TAK" else "NIE"}", 
                    fontSize = 12.sp, 
                    color = if (isIgnoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun DebugSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
