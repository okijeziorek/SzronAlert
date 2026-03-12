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
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userPreferences by viewModel.userPreferences.collectAsState()
    val billingClient = remember { BillingClientWrapper(context) }
    val isProActual by billingClient.isPro.collectAsState()
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    if (userPreferences == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val prefs = userPreferences!!
        val isPro = isProActual || prefs.isProForced

        // Stan dla pól tekstowych lokalizacji
        var latText by remember(prefs.manualLatitude) { mutableStateOf(prefs.manualLatitude.toString()) }
        var lonText by remember(prefs.manualLongitude) { mutableStateOf(prefs.manualLongitude.toString()) }
        var nameText by remember(prefs.manualLocationName) { mutableStateOf(prefs.manualLocationName) }

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

                SectionTitle(stringResource(R.string.section_app_profile))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = prefs.appMode == 0,
                        onClick = { viewModel.updateAppMode(0) },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(R.string.profile_car))
                    }
                    SegmentedButton(
                        selected = prefs.appMode == 1,
                        onClick = { viewModel.updateAppMode(1) },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(R.string.profile_garden))
                    }
                }
                Text(
                    text = if (prefs.appMode == 0) stringResource(R.string.profile_car_desc) else stringResource(R.string.profile_garden_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )

                SectionTitle(stringResource(R.string.section_location))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.manual_location_toggle), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = prefs.isManualLocationEnabled,
                        onCheckedChange = { viewModel.updateManualLocation(it, prefs.manualLatitude, prefs.manualLongitude, prefs.manualLocationName) }
                    )
                }
                
                if (prefs.isManualLocationEnabled) {
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
                                    latValue == null -> "Błąd"
                                    latValue < -90.0 || latValue > 90.0 -> "Lat: -90 do 90"
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
                                    lonValue == null -> "Błąd"
                                    lonValue < -180.0 || lonValue > 180.0 -> "Lon: -180 do 180"
                                    else -> null
                                }
                            },
                            label = { Text("Lon") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = lonError != null
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

                SectionTitle(stringResource(R.string.section_summer_settings))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.storm_alert_label), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = prefs.isStormAlertEnabled,
                        onCheckedChange = { viewModel.updateStormAlertEnabled(it) }
                    )
                }
                
                if (prefs.appMode == 1) { // Tylko w trybie Garden
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.watering_reminder_label), style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = prefs.isWateringReminderEnabled,
                            onCheckedChange = { viewModel.updateWateringReminderEnabled(it) }
                        )
                    }
                }

                SettingSlider(
                    label = stringResource(R.string.heat_threshold_label),
                    value = prefs.heatThreshold.toFloat(),
                    onValueChange = { viewModel.updateHeatThreshold(it.toDouble()) },
                    range = 20f..40f,
                    steps = 20,
                    format = if (prefs.useFahrenheit) "%.0f °F" else "%.0f °C"
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                SectionTitle(stringResource(R.string.section_frost_options))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.mata_option_label), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = prefs.isMataOptionEnabled,
                        onCheckedChange = { viewModel.updateMataOptionEnabled(it) }
                    )
                }
                Text(stringResource(R.string.mata_option_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                SectionTitle(stringResource(R.string.section_units))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.use_fahrenheit_label), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = prefs.useFahrenheit, onCheckedChange = { viewModel.updateUseFahrenheit(it) })
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                SectionTitle(stringResource(R.string.section_appearance))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = prefs.theme == 0, onClick = { viewModel.updateTheme(0) }, shape = MaterialTheme.shapes.medium) { Text(stringResource(R.string.theme_light)) }
                    SegmentedButton(selected = prefs.theme == 1, onClick = { viewModel.updateTheme(1) }, shape = MaterialTheme.shapes.medium) { Text(stringResource(R.string.theme_dark)) }
                    SegmentedButton(selected = prefs.theme == 2, onClick = { viewModel.updateTheme(2) }, shape = MaterialTheme.shapes.medium) { Text(stringResource(R.string.theme_auto)) }
                }

                Spacer(Modifier.height(24.dp))
                SectionTitle(stringResource(R.string.section_algorithm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.auto_mode), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = prefs.isAutoModeEnabled, onCheckedChange = { viewModel.updateAutoModeEnabled(it) })
                }
                
                SettingSlider(
                    label = stringResource(R.string.sensitivity_label),
                    value = prefs.sensitivity.toFloat(),
                    onValueChange = { viewModel.updateSensitivity(it.toDouble()) },
                    range = 0.5f..2.0f,
                    steps = 15,
                    format = "x %.1f"
                )
                Text(stringResource(R.string.sensitivity_desc), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 16.dp))

                SettingSlider(
                    label = stringResource(R.string.temp_threshold),
                    value = prefs.tempThreshold.toFloat(),
                    onValueChange = { viewModel.updateTempThreshold(it.toDouble()) },
                    range = -10f..10f,
                    steps = 19,
                    format = if (prefs.useFahrenheit) "%.1f °F" else "%.1f °C",
                    enabled = !prefs.isAutoModeEnabled
                )
                
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                SectionTitle(stringResource(R.string.section_notifications))
                SettingSlider(label = stringResource(R.string.alert_start), value = prefs.alertStartHour.toFloat(), onValueChange = { viewModel.updateAlertStartHour(it.toInt()) }, range = 0f..23f, steps = 23, format = "%.0f:00")
                SettingSlider(label = stringResource(R.string.alert_end), value = prefs.alertEndHour.toFloat(), onValueChange = { viewModel.updateAlertEndHour(it.toInt()) }, range = 0f..23f, steps = 23, format = "%.0f:00")

                Spacer(Modifier.height(24.dp))
                SectionTitle(stringResource(R.string.car_mode_title))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.car_mode_desc), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = prefs.isCarModeEnabled,
                        onCheckedChange = {
                            viewModel.updateCarModeEnabled(it)
                            val carModeReceiver = CarModeReceiver()
                            if (it) carModeReceiver.schedule(context, prefs.carModeHour) else carModeReceiver.cancel(context)
                        }
                    )
                }
                SettingSlider(
                    label = stringResource(R.string.car_mode_check_hour),
                    value = prefs.carModeHour.toFloat(),
                    onValueChange = {
                        viewModel.updateCarModeHour(it.toInt())
                        if (prefs.isCarModeEnabled) CarModeReceiver().schedule(context, it.toInt())
                    },
                    range = 0f..23f,
                    steps = 23,
                    format = "%.0f:00",
                    enabled = prefs.isCarModeEnabled
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
