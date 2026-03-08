package pl.oki.frostalert.ui.screens

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.billingclient.api.ProductDetails
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import pl.oki.frostalert.R
import pl.oki.frostalert.billing.BillingClientWrapper
import pl.oki.frostalert.receiver.CarModeReceiver
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val userPreferences by viewModel.userPreferences.collectAsState()
    val billingClient = remember { BillingClientWrapper(context) }
    val isPro by billingClient.isPro.collectAsState()
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Stan dla pól tekstowych lokalizacji
    var latText by remember(userPreferences.manualLatitude) { mutableStateOf(userPreferences.manualLatitude.toString()) }
    var lonText by remember(userPreferences.manualLongitude) { mutableStateOf(userPreferences.manualLongitude.toString()) }
    var nameText by remember(userPreferences.manualLocationName) { mutableStateOf(userPreferences.manualLocationName) }

    // Stan dla błędów validacji
    var latError by remember { mutableStateOf<String?>(null) }
    var lonError by remember { mutableStateOf<String?>(null) }

    if (!isPro) {
        val adLoader = remember(context) {
            AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110")
                .forNativeAd { ad: NativeAd ->
                    nativeAd?.destroy()
                    nativeAd = ad
                }
                .build()
        }
        LaunchedEffect(adLoader) { adLoader.loadAd(AdRequest.Builder().build()) }
        DisposableEffect(Unit) { onDispose { nativeAd?.destroy() } }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
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
                        productDetails?.let { billingClient.launchPurchaseFlow(context as Activity, it) }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.buy_pro))
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionTitle(stringResource(R.string.section_location))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.manual_location_toggle), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = userPreferences.isManualLocationEnabled,
                    onCheckedChange = { viewModel.updateManualLocation(it, userPreferences.manualLatitude, userPreferences.manualLongitude, userPreferences.manualLocationName) }
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
                        onValueChange = {
                            latText = it
                            val latValue = it.toDoubleOrNull()
                            latError = when {
                                latValue == null -> "Nieprawidłowa wartość"
                                latValue < -90.0 || latValue > 90.0 -> "Latitude: -90° do 90°"
                                else -> null
                            }
                        },
                        label = { Text("Lat") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = latError != null
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = {
                            lonText = it
                            val lonValue = it.toDoubleOrNull()
                            lonError = when {
                                lonValue == null -> "Nieprawidłowa wartość"
                                lonValue < -180.0 || lonValue > 180.0 -> "Longitude: -180° do 180°"
                                else -> null
                            }
                        },
                        label = { Text("Lon") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = lonError != null
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = latError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = lonError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull() ?: 52.2297
                        val lon = lonText.toDoubleOrNull() ?: 21.0122
                        viewModel.updateManualLocation(true, lat, lon, nameText)
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                    enabled = latError == null && lonError == null
                ) { Text(stringResource(R.string.save_location)) }
            } else {
                Text(stringResource(R.string.location_gps_auto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("Jednostki")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Używaj stopni Fahrenheita (°F)", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = userPreferences.useFahrenheit, onCheckedChange = { viewModel.updateUseFahrenheit(it) })
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.section_appearance))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = userPreferences.theme == 0, onClick = { viewModel.updateTheme(0) }, shape = MaterialTheme.shapes.medium) { Text(stringResource(R.string.theme_light)) }
                SegmentedButton(selected = userPreferences.theme == 1, onClick = { viewModel.updateTheme(1) }, shape = MaterialTheme.shapes.medium) { Text(stringResource(R.string.theme_dark)) }
                SegmentedButton(selected = userPreferences.theme == 2, onClick = { viewModel.updateTheme(2) }, shape = MaterialTheme.shapes.medium) { Text(stringResource(R.string.theme_auto)) }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.section_algorithm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.auto_mode), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = userPreferences.isAutoModeEnabled, onCheckedChange = { viewModel.updateAutoModeEnabled(it) })
            }
            
            SettingSlider(
                label = stringResource(R.string.sensitivity_label),
                value = userPreferences.sensitivity.toFloat(),
                onValueChange = { viewModel.updateSensitivity(it.toDouble()) },
                range = 0.5f..2.0f,
                steps = 15,
                format = "x %.1f"
            )
            Text(stringResource(R.string.sensitivity_desc), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 16.dp))

            SettingSlider(
                label = stringResource(R.string.temp_threshold),
                value = userPreferences.tempThreshold.toFloat(),
                onValueChange = { viewModel.updateTempThreshold(it.toDouble()) },
                range = -10f..10f,
                steps = 19,
                format = if (userPreferences.useFahrenheit) "%.1f °F" else "%.1f °C",
                enabled = !userPreferences.isAutoModeEnabled
            )
            SettingSlider(
                label = stringResource(R.string.humidity_threshold),
                value = userPreferences.humidityThreshold.toFloat(),
                onValueChange = { viewModel.updateHumidityThreshold(it.toInt()) },
                range = 0f..100f,
                steps = 100,
                format = "%.0f %%",
                enabled = !userPreferences.isAutoModeEnabled
            )
            SettingSlider(
                label = stringResource(R.string.precipitation_threshold),
                value = userPreferences.precipitationThreshold.toFloat(),
                onValueChange = { viewModel.updatePrecipitationThreshold(it.toDouble()) },
                range = 0f..1f,
                steps = 10,
                format = "%.1f mm",
                enabled = !userPreferences.isAutoModeEnabled
            )
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.section_notifications))
            SettingSlider(label = stringResource(R.string.alert_start), value = userPreferences.alertStartHour.toFloat(), onValueChange = { viewModel.updateAlertStartHour(it.toInt()) }, range = 0f..23f, steps = 23, format = "%.0f:00")
            SettingSlider(label = stringResource(R.string.alert_end), value = userPreferences.alertEndHour.toFloat(), onValueChange = { viewModel.updateAlertEndHour(it.toInt()) }, range = 0f..23f, steps = 23, format = "%.0f:00")

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.car_mode_title))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.car_mode_desc), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = userPreferences.isCarModeEnabled,
                    onCheckedChange = {
                        viewModel.updateCarModeEnabled(it)
                        val carModeReceiver = CarModeReceiver()
                        if (it) carModeReceiver.schedule(context, userPreferences.carModeHour) else carModeReceiver.cancel(context)
                    }
                )
            }
            SettingSlider(
                label = stringResource(R.string.car_mode_check_hour),
                value = userPreferences.carModeHour.toFloat(),
                onValueChange = {
                    viewModel.updateCarModeHour(it.toInt())
                    if (userPreferences.isCarModeEnabled) CarModeReceiver().schedule(context, it.toInt())
                },
                range = 0f..23f,
                steps = 23,
                format = "%.0f:00",
                enabled = userPreferences.isCarModeEnabled
            )

            if (!isPro) {
                Spacer(Modifier.height(24.dp))
                nativeAd?.let { ad ->
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.native_ad_layout, FrameLayout(ctx), false) as NativeAdView },
                        update = { adView -> populateNativeAdView(ad, adView) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
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
private fun SettingSlider(label: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, steps: Int, format: String, enabled: Boolean = true) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(String.format(Locale.US, format, value), style = MaterialTheme.typography.bodyLarge)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps, enabled = enabled)
    }
}
