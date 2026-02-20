package pl.oki.frostalert.ui.screens

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.billingclient.api.ProductDetails
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.launch
import pl.oki.frostalert.R
import pl.oki.frostalert.billing.BillingClientWrapper
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.receiver.CarModeReceiver

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
            theme = 2,
            isManualLocationEnabled = false,
            manualLatitude = 52.2297,
            manualLongitude = 21.0122,
            manualLocationName = "Warszawa"
        )
    )
    val isPro by billingClient.isPro.collectAsState()
    val scope = rememberCoroutineScope()
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Stan dla pól tekstowych lokalizacji
    var latText by remember(userPreferences.manualLatitude) { mutableStateOf(userPreferences.manualLatitude.toString()) }
    var lonText by remember(userPreferences.manualLongitude) { mutableStateOf(userPreferences.manualLongitude.toString()) }
    var nameText by remember(userPreferences.manualLocationName) { mutableStateOf(userPreferences.manualLocationName) }

    if (!isPro) {
        val adLoader = remember(context) {
            AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110") // Test Ad ID
                .forNativeAd { ad: NativeAd ->
                    nativeAd?.destroy()
                    nativeAd = ad
                }
                .build()
        }

        LaunchedEffect(adLoader) {
            adLoader.loadAd(AdRequest.Builder().build())
        }

        DisposableEffect(Unit) {
            onDispose {
                nativeAd?.destroy()
            }
        }
    }

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
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Kup Pro (9,99 zł) i usuń reklamy")
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionTitle("Lokalizacja")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ustaw lokalizację ręcznie", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = userPreferences.isManualLocationEnabled,
                    onCheckedChange = { scope.launch { dataStore.updateManualLocation(it, userPreferences.manualLatitude, userPreferences.manualLongitude, userPreferences.manualLocationName) } }
                )
            }
            
            if (userPreferences.isManualLocationEnabled) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nazwa miejscowości") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Szerokość (Lat)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Długość (Lon)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull() ?: 52.2297
                        val lon = lonText.toDoubleOrNull() ?: 21.0122
                        scope.launch {
                            dataStore.updateManualLocation(true, lat, lon, nameText)
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Zapisz lokalizację")
                }
            } else {
                Text(
                    "Używam GPS (automatycznie)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("Wygląd")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SegmentedButton(
                    selected = userPreferences.theme == 0,
                    onClick = { scope.launch { dataStore.updateTheme(0) } },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Jasny")
                }
                SegmentedButton(
                    selected = userPreferences.theme == 1,
                    onClick = { scope.launch { dataStore.updateTheme(1) } },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Ciemny")
                }
                SegmentedButton(
                    selected = userPreferences.theme == 2,
                    onClick = { scope.launch { dataStore.updateTheme(2) } },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Auto")
                }
            }
            Spacer(Modifier.height(24.dp))

            SectionTitle("Algorytm Szronu")
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
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("Powiadomienia")
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
            Spacer(Modifier.height(24.dp))

            SectionTitle("Tryb Samochód (Lód na szybach)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Włącz przypomnienia", style = MaterialTheme.typography.bodyLarge)
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
                label = "Godzina porannego sprawdzenia",
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

            if (!isPro) {
                Spacer(Modifier.height(24.dp))
                val ad = nativeAd
                if (ad != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            LayoutInflater.from(ctx).inflate(R.layout.native_ad_layout, FrameLayout(ctx), false) as NativeAdView
                        },
                        update = { adView ->
                            populateNativeAdView(ad, adView)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

    (adView.headlineView as? TextView)?.text = nativeAd.headline
    adView.headlineView?.visibility = if (nativeAd.headline == null) View.INVISIBLE else View.VISIBLE

    (adView.bodyView as? TextView)?.text = nativeAd.body
    adView.bodyView?.visibility = if (nativeAd.body == null) View.INVISIBLE else View.VISIBLE

    (adView.callToActionView as? Button)?.text = nativeAd.callToAction
    adView.callToActionView?.visibility = if (nativeAd.callToAction == null) View.INVISIBLE else View.VISIBLE

    adView.setNativeAd(nativeAd)
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
