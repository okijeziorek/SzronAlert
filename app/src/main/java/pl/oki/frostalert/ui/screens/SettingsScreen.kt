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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import pl.oki.frostalert.R
import pl.oki.frostalert.receiver.CarModeReceiver
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val purchaseError by viewModel.purchaseError.collectAsState()
    val locationLimitReached by viewModel.locationLimitReached.collectAsState()
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    purchaseError?.let { error ->
        LaunchedEffect(error) {
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearPurchaseError()
        }
    }

    if (locationLimitReached) {
        LaunchedEffect(locationLimitReached) {
            android.widget.Toast.makeText(context, context.getString(R.string.location_limit_reached), android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearLocationLimitWarning()
        }
    }

    if (userPreferences == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val prefs = userPreferences!!

        // Stan dla pól tekstowych lokalizacji
        var latText by remember(prefs.manualLatitude) { mutableStateOf(prefs.manualLatitude.toString()) }
        var lonText by remember(prefs.manualLongitude) { mutableStateOf(prefs.manualLongitude.toString()) }
        var nameText by remember(prefs.manualLocationName) { mutableStateOf(prefs.manualLocationName) }

        // Stan dla błędów validacji
        var latError by remember { mutableStateOf<String?>(null) }
        var lonError by remember { mutableStateOf<String?>(null) }

        if (!isPro) {
            val adLoader = remember(context) {
                AdLoader.Builder(context, context.getString(R.string.admob_native_unit_id))
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
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.settings_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
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
                        viewModel.launchPurchaseFlow(context as Activity)
                    }, modifier = Modifier.fillMaxWidth()) {
                        SingleLineText(text = stringResource(R.string.buy_pro), style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(Modifier.height(16.dp))
                    nativeAd?.let { ad ->
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.native_ad_layout, FrameLayout(ctx), false) as NativeAdView },
                            update = { adView -> populateNativeAdView(ad, adView) }
                        )
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
                        SingleLineText(text = stringResource(R.string.profile_car), style = MaterialTheme.typography.bodyMedium)
                    }
                    SegmentedButton(
                        selected = prefs.appMode == 1,
                        onClick = { viewModel.updateAppMode(1) },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        SingleLineText(text = stringResource(R.string.profile_garden), style = MaterialTheme.typography.bodyMedium)
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
                    SingleLineText(text = stringResource(R.string.manual_location_toggle), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = prefs.isManualLocationEnabled,
                        onCheckedChange = { viewModel.updateManualLocation(it, prefs.manualLatitude, prefs.manualLongitude, prefs.manualLocationName) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                var showGeofenceHistory by remember { mutableStateOf(false) }
                Button(onClick = { showGeofenceHistory = true }, modifier = Modifier.fillMaxWidth()) {
                    SingleLineText(text = stringResource(R.string.geofence_history_title), style = MaterialTheme.typography.bodyLarge)
                }

                if (showGeofenceHistory) {
                    Dialog(onDismissRequest = { showGeofenceHistory = false }) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            GeofenceHistoryScreen()
                        }
                    }
                }
                if (prefs.isManualLocationEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text(stringResource(R.string.location_name_label)) },
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
                                    latValue == null -> context.getString(R.string.location_input_error)
                                    latValue < -90.0 || latValue > 90.0 -> context.getString(R.string.location_lat_range_error)
                                    else -> null
                                }
                            },
                            label = { Text(stringResource(R.string.location_lat_label)) },
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
                                    lonValue == null -> context.getString(R.string.location_input_error)
                                    lonValue < -180.0 || lonValue > 180.0 -> context.getString(R.string.location_lon_range_error)
                                    else -> null
                                }
                            },
                            label = { Text(stringResource(R.string.location_lon_label)) },
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
                    ) { SingleLineText(text = stringResource(R.string.save_location), style = MaterialTheme.typography.bodyLarge) }

                    // Save as separate location button
                    if (latError == null && lonError == null) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                val lat = latText.toDoubleOrNull() ?: 52.2297
                                val lon = lonText.toDoubleOrNull() ?: 21.0122
                                viewModel.saveCurrentLocation(nameText, lat, lon)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) { SingleLineText(text = stringResource(R.string.location_save_as_separate), style = MaterialTheme.typography.bodyLarge) }
                    }
                } else {
                    Text(stringResource(R.string.location_gps_auto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                // Saved locations list
                val savedLocations by viewModel.savedLocations.collectAsState()
                if (savedLocations.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.location_saved_locations_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    savedLocations.forEach { location ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                SingleLineText(text = location.name, style = MaterialTheme.typography.bodyMedium)
                                SingleLineText(
                                    text = "${String.format(Locale.US, "%.4f", location.latitude)}, ${String.format(Locale.US, "%.4f", location.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                )
                            }
                            IconButton(onClick = { viewModel.deleteSavedLocation(location) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.location_delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                // Geofence radius setting
                Text(stringResource(R.string.geofence_radius_title), style = MaterialTheme.typography.titleMedium)
                val radiusKm = prefs.geofenceRadiusMeters / 1000.0
                var sliderRadius by remember { mutableStateOf(radiusKm.toFloat()) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(value = sliderRadius, onValueChange = {
                        sliderRadius = it
                    }, valueRange = 1f..100f, steps = 99)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SingleLineText(text = "${String.format(Locale.US, "%.0f", sliderRadius)} km", style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { viewModel.updateGeofenceRadius((sliderRadius * 1000.0)) }) {
                            SingleLineText(text = stringResource(R.string.geofence_radius_save), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { sliderRadius = 5f }) { SingleLineText(text = "5 km", style = MaterialTheme.typography.bodyMedium) }
                        Button(onClick = { sliderRadius = 10f }) { SingleLineText(text = "10 km", style = MaterialTheme.typography.bodyMedium) }
                        Button(onClick = { sliderRadius = 20f }) { SingleLineText(text = "20 km", style = MaterialTheme.typography.bodyMedium) }
                        Button(onClick = { sliderRadius = 50f }) { SingleLineText(text = "50 km", style = MaterialTheme.typography.bodyMedium) }
                    }
                }

                SectionTitle(stringResource(R.string.section_summer_settings))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleLineText(text = stringResource(R.string.storm_alert_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
                        SingleLineText(text = stringResource(R.string.watering_reminder_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
                    SingleLineText(text = stringResource(R.string.mata_option_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
                    SingleLineText(text = stringResource(R.string.use_fahrenheit_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = prefs.useFahrenheit, onCheckedChange = { viewModel.updateUseFahrenheit(it) })
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                SectionTitle(stringResource(R.string.section_appearance))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = prefs.theme == 0, onClick = { viewModel.updateTheme(0) }, shape = MaterialTheme.shapes.medium) { SingleLineText(text = stringResource(R.string.theme_light), style = MaterialTheme.typography.bodyMedium) }
                    SegmentedButton(selected = prefs.theme == 1, onClick = { viewModel.updateTheme(1) }, shape = MaterialTheme.shapes.medium) { SingleLineText(text = stringResource(R.string.theme_dark), style = MaterialTheme.typography.bodyMedium) }
                    SegmentedButton(selected = prefs.theme == 2, onClick = { viewModel.updateTheme(2) }, shape = MaterialTheme.shapes.medium) { SingleLineText(text = stringResource(R.string.theme_auto), style = MaterialTheme.typography.bodyMedium) }
                }

                Spacer(Modifier.height(24.dp))
                SectionTitle(stringResource(R.string.section_algorithm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleLineText(text = stringResource(R.string.auto_mode), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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

                SectionTitle(stringResource(R.string.section_geofencing))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.geofencing_label),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.geofencing_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = prefs.isGeofencingEnabled,
                        onCheckedChange = {
                            // Delegujemy logikę rejestracji do ViewModel / GeofenceRegistrar
                            viewModel.updateGeofencingEnabled(it)
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                SectionTitle(stringResource(R.string.section_notifications))
                SettingSlider(label = stringResource(R.string.alert_start), value = prefs.alertStartHour.toFloat(), onValueChange = { viewModel.updateAlertStartHour(it.toInt()) }, range = 0f..23f, steps = 23, format = "%.0f:00")
                SettingSlider(label = stringResource(R.string.alert_end), value = prefs.alertEndHour.toFloat(), onValueChange = { viewModel.updateAlertEndHour(it.toInt()) }, range = 0f..23f, steps = 23, format = "%.0f:00")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleLineText(text = stringResource(R.string.trend_change_notifications_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = prefs.isTrendChangeNotificationsEnabled,
                        onCheckedChange = { viewModel.updateTrendChangeNotificationsEnabled(it) }
                    )
                }

                // B2: Calendar sync toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.calendar_sync_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.calendar_sync_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = prefs.isCalendarSyncEnabled,
                        onCheckedChange = { viewModel.updateCalendarSyncEnabled(it) }
                    )
                }

                // B4: TTS toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.tts_enabled_label),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.tts_enabled_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = prefs.isTtsEnabled,
                        onCheckedChange = { viewModel.updateTtsEnabled(it) }
                    )
                }

                Spacer(Modifier.height(24.dp))
                SectionTitle(stringResource(R.string.car_mode_title))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.car_mode_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                SectionTitle(stringResource(R.string.section_calibration))
                val calibrationViewModel: CalibrationViewModel = hiltViewModel()
                val calibrationState by calibrationViewModel.uiState.collectAsState()

                if (calibrationState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    calibrationState.calibrationResult?.let { result ->
                        // Dokładność predykcji
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.calibration_accuracy),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "%.1f%%".format(Locale.US, result.accuracyPercentage),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    result.accuracyPercentage >= 80 -> MaterialTheme.colorScheme.primary
                                    result.accuracyPercentage >= 60 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Liczba feedbacków
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.calibration_feedback_count),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = result.totalFeedback.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Wyjaśnienie
                        Text(
                            text = calibrationViewModel.getCalibrationExplanation(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Przycisk zastosowania rekomendacji
                        if (calibrationState.shouldShowSuggestions) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { calibrationViewModel.applyCalibrationRecommendations() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SingleLineText(text = stringResource(R.string.calibration_apply_recommendations), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } ?: run {
                        // Brak danych
                        Text(
                            text = stringResource(R.string.calibration_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.calibration_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(R.string.section_advanced_tools))

                var showDashboardConfig by remember { mutableStateOf(false) }
                Button(onClick = { showDashboardConfig = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.dashboard_config_title),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showDashboardConfig) {
                    Dialog(onDismissRequest = { showDashboardConfig = false }) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            DashboardConfigScreen()
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                var showSmartHome by remember { mutableStateOf(false) }
                Button(onClick = { showSmartHome = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.smart_home_title),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showSmartHome) {
                    Dialog(onDismissRequest = { showSmartHome = false }) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            SmartHomeSettingsScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    SingleLineText(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SingleLineText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontWeight: FontWeight? = null
) {
    Text(
        text = text,
        style = style,
        modifier = modifier,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
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
private fun SettingSlider(label: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, steps: Int, format: String, enabled: Boolean = true) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SingleLineText(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            SingleLineText(
                text = String.format(Locale.US, format, value),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps, enabled = enabled)
    }
}
