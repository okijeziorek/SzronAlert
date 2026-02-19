package pl.oki.frostalert.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import pl.oki.frostalert.FrostCheckWorker
import pl.oki.frostalert.data.SettingsDataStore

@Composable
fun DebugScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = SettingsDataStore(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            scope.launch {
                dataStore.updateIgnoreUntil(0L)
            }
        }) {
            Text("Resetuj \"Ignoruj dziś\"")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val testData = Data.Builder().putBoolean("IS_TEST", true).build()
            val workRequest = OneTimeWorkRequestBuilder<FrostCheckWorker>()
                .setInputData(testData)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }) {
            Text("Wywołaj alert")
        }
    }
}
