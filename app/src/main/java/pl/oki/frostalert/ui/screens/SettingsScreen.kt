package pl.oki.frostalert.ui.screens

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.launch
import pl.oki.frostalert.BillingClientWrapper
import pl.oki.frostalert.CarModeReceiver
import pl.oki.frostalert.R
import pl.oki.frostalert.data.SettingsDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val dataStore = SettingsDataStore(context)
    val billingClient = remember { BillingClientWrapper(context) }
    val userPreferences by dataStore.userPreferencesFlow.collectAsState(initial = null)
    val isPro by billingClient.isPro.collectAsState()
    val scope = rememberCoroutineScope()
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(Unit) {
        if (!isPro) {
            val adLoader = AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110") // Test Ad ID
                .forNativeAd { ad ->
                    nativeAd = ad
                }
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ustawienia") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            userPreferences?.let { prefs ->
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
                    Text("Tryb automatyczny", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = prefs.isAutoModeEnabled,
                        onCheckedChange = { scope.launch { dataStore.updateAutoModeEnabled(it) } }
                    )
                }
                Spacer(Modifier.height(16.dp))
                SettingSlider(
                    label = "Próg temperatury",
                    value = prefs.tempThreshold.toFloat(),
                    onValueChange = { scope.launch { dataStore.updateTempThreshold(it.toDouble()) } },
                    range = -10f..10f,
                    steps = 19,
                    format = "%.1f °C",
                    enabled = !prefs.isAutoModeEnabled
                )
                Spacer(Modifier.height(16.dp))
                SettingSlider(
                    label = "Próg wilgotności",
                    value = prefs.humidityThreshold.toFloat(),
                    onValueChange = { scope.launch { dataStore.updateHumidityThreshold(it.toInt()) } },
                    range = 0f..100f,
                    steps = 100,
                    format = "%.0f %%",
                    enabled = !prefs.isAutoModeEnabled
                )
                Spacer(Modifier.height(16.dp))
                SettingSlider(
                    label = "Próg opadów",
                    value = prefs.precipitationThreshold.toFloat(),
                    onValueChange = { scope.launch { dataStore.updatePrecipitationThreshold(it.toDouble()) } },
                    range = 0f..1f,
                    steps = 10,
                    format = "%.1f mm",
                    enabled = !prefs.isAutoModeEnabled
                )
                Spacer(Modifier.height(16.dp))

                if (!isPro) {
                    nativeAd?.let {
                        AndroidView(
                            factory = { context ->
                                val adView = LayoutInflater.from(context).inflate(R.layout.native_ad_layout, null, false) as NativeAdView
                                adView.headlineView = adView.findViewById(R.id.ad_headline)
                                adView.bodyView = adView.findViewById(R.id.ad_body)
                                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                                (adView.headlineView as TextView).text = it.headline
                                (adView.bodyView as TextView).text = it.body
                                (adView.callToActionView as Button).text = it.callToAction
                                adView.setNativeAd(it)
                                adView
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                SettingSlider(
                    label = "Godzina rozpoczęcia alertów",
                    value = prefs.alertStartHour.toFloat(),
                    onValueChange = { scope.launch { dataStore.updateAlertStartHour(it.toInt()) } },
                    range = 0f..23f,
                    steps = 23,
                    format = "%.0f:00"
                )
                Spacer(Modifier.height(16.dp))
                SettingSlider(
                    label = "Godzina zakończenia alertów",
                    value = prefs.alertEndHour.toFloat(),
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
                        checked = prefs.isCarModeEnabled,
                        onCheckedChange = {
                            scope.launch {
                                dataStore.updateCarModeEnabled(it)
                                val carModeReceiver = CarModeReceiver()
                                if (it) {
                                    carModeReceiver.schedule(context, prefs.carModeHour)
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
                    value = prefs.carModeHour.toFloat(),
                    onValueChange = { 
                        scope.launch { 
                            dataStore.updateCarModeHour(it.toInt()) 
                            if (prefs.isCarModeEnabled) {
                                val carModeReceiver = CarModeReceiver()
                                carModeReceiver.schedule(context, it.toInt())
                            }
                        }
                    },
                    range = 0f..23f,
                    steps = 23,
                    format = "%.0f:00"
                )
            }
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