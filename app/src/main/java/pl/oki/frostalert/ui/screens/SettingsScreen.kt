package pl.oki.frostalert.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.launch
import pl.oki.frostalert.BillingClientWrapper
import pl.oki.frostalert.CarModeReceiver
import pl.oki.frostalert.data.SettingsDataStore
import pl.oki.frostalert.data.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val dataStore = SettingsDataStore(context)
    val billingClient = remember { BillingClientWrapper(context) }
    val userPreferences by dataStore.userPreferencesFlow.collectAsState(
        initial = UserPreferences(
            tempThreshold = 2.0,
            humidityThreshold = 80,
            precipitationThreshold = 0.1,
            alertStartHour = 18,
            alertEndHour = 8,
            ignoreUntil = 0L,
            carModeHour = 7,
            isAutoModeEnabled = true,
            isCarModeEnabled = true,
            isDarkThemeEnabled = false
        )
    )
    val isPro by billingClient.isPro.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Ustawienia") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (!isPro) {
                Button(onClick = {
                    billingClient.queryProductDetails { productDetails: ProductDetails? ->
                        productDetails?.let {
                            billingClient.launchPurchaseFlow(context as Activity, it)
                        }
                    }
                }) {
                    Text("Kup Pro (9,99 zł) i usuń reklamy")
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tryb ciemny", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = userPreferences.isDarkThemeEnabled,
                    onCheckedChange = { scope.launch { dataStore.updateDarkThemeEnabled(it) } }
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tryb automatyczny", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = userPreferences.isAutoModeEnabled,
                    onCheckedChange = { scope.launch { dataStore.updateAutoModeEnabled(it) } }
                )
            }
            Spacer(Modifier.height(16.dp))
            SettingSlider(
                label = "Próg temperatury",
                value = userPreferences.tempThreshold.toFloat(),
                onValueChange = { scope.launch { dataStore.updateTempThreshold(it.toDouble()) } },
                range = -10f..10f,
                steps = 19,
                format = "%.1f °C",
                enabled = !userPreferences.isAutoModeEnabled
            )
            Spacer(Modifier.height(16.dp))
            SettingSlider(
                label = "Próg wilgotności",
                value = userPreferences.humidityThreshold.toFloat(),
                onValueChange = { scope.launch { dataStore.updateHumidityThreshold(it.toInt()) } },
                range = 0f..100f,
                steps = 100,
                format = "%.0f %%",
                enabled = !userPreferences.isAutoModeEnabled
            )
            Spacer(Modifier.height(16.dp))
            SettingSlider(
                label = "Próg opadów",
                value = userPreferences.precipitationThreshold.toFloat(),
                onValueChange = { scope.launch { dataStore.updatePrecipitationThreshold(it.toDouble()) } },
                range = 0f..1f,
                steps = 10,
                format = "%.1f mm",
                enabled = !userPreferences.isAutoModeEnabled
            )
            Spacer(Modifier.height(16.dp))
            SettingSlider(
                label = "Godzina rozpoczęcia alertów",
                value = userPreferences.alertStartHour.toFloat(),
                onValueChange = { scope.launch { dataStore.updateAlertStartHour(it.toInt()) } },
                range = 0f..23f,
                steps = 23,
                format = "%.0f:00"
            )
            Spacer(Modifier.height(16.dp))
            SettingSlider(
                label = "Godzina zakończenia alertów",
                value = userPreferences.alertEndHour.toFloat(),
                onValueChange = { scope.launch { dataStore.updateAlertEndHour(it.toInt()) } },
                range = 0f..23f,
                steps = 23,
                format = "%.0f:00"
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Włącz tryb samochód", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = userPreferences.isCarModeEnabled,
                    onCheckedChange = {
                        scope.launch {
                            dataStore.updateCarModeEnabled(it)
                            val carModeReceiver = CarModeReceiver()
                            if (it) {
                                carModeReceiver.schedule(context, userPreferences.carModeHour)
                            } else {
                                carModeReceiver.cancel(context)
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            SettingSlider(
                label = "Godzina przypomnienia (samochód)",
                value = userPreferences.carModeHour.toFloat(),
                onValueChange = {
                    scope.launch {
                        dataStore.updateCarModeHour(it.toInt())
                        if (userPreferences.isCarModeEnabled) {
                            val carModeReceiver = CarModeReceiver()
                            carModeReceiver.schedule(context, it.toInt())
                        }
                    }
                },
                range = 0f..23f,
                steps = 23,
                format = "%.0f:00",
                enabled = userPreferences.isCarModeEnabled
            )
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: String,
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(String.format(format, value), style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            enabled = enabled
        )
    }
}
