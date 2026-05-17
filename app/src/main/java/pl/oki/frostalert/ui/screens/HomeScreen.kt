package pl.oki.frostalert.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import pl.oki.frostalert.R
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.data.remote.DailyForecast
import pl.oki.frostalert.data.remote.HourlyForecast
import pl.oki.frostalert.ui.theme.RiskHigh
import pl.oki.frostalert.ui.theme.RiskModerate
import pl.oki.frostalert.ui.theme.RiskNone
import pl.oki.frostalert.ui.theme.RiskVeryHigh
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.utils.SummerCalculations
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val userPrefs by settingsViewModel.userPreferences.collectAsState()
    val scope = rememberCoroutineScope()
    val triggerReview by viewModel.triggerReview.collectAsState()
    val context = LocalContext.current

    // In-App Review flow
    if (triggerReview) {
        LaunchedEffect(Unit) {
            try {
                val manager = com.google.android.play.core.review.ReviewManagerFactory.create(context)
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val activity = context as? android.app.Activity
                        if (activity != null) {
                            manager.launchReviewFlow(activity, task.result)
                        }
                    }
                    viewModel.onReviewLaunched()
                }
            } catch (e: Exception) {
                viewModel.onReviewLaunched()
            }
        }
    }

    Scaffold { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BatteryOptimizationWarning()

                if (userPrefs == null) {
                    Box(Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val prefs = userPrefs!!
                    MutedAlertsBanner(prefs, settingsViewModel)

                    // Location selector chips
                    LocationSelectorRow(viewModel, prefs)

                    when (val state = uiState) {
                        is HomeUiState.Loading -> {
                            Box(Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is HomeUiState.Error -> {
                            ErrorState(state.message) { viewModel.refreshData() }
                        }
                        is HomeUiState.Success -> {
                            WeatherSuccessContent(state, viewModel, settingsViewModel, prefs, scope)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MutedAlertsBanner(prefs: UserPreferences, viewModel: SettingsViewModel) {
    val isMuted = remember(prefs.ignoreUntil) { prefs.ignoreUntil > System.currentTimeMillis() }
    AnimatedVisibility(visible = isMuted, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsOff, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.muted_banner_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.muted_banner_desc),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = { viewModel.updateIgnoreUntil(0L) }) {
                    Text(stringResource(R.string.restore_btn), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun WeatherSuccessContent(
    state: HomeUiState.Success,
    viewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    userPrefs: UserPreferences,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val isGarden = state.appMode == 1
    val isSummer = state.weather.current.temperature > 15.0
    val isPro by settingsViewModel.isPro.collectAsState()
    
    // Zapamiętujemy lokalnie, czy feedback został wysłany w tej sesji
    var feedbackSubmitted by remember { mutableStateOf(false) }
    
    val showFeedback = remember(userPrefs.lastFeedbackTimestamp, feedbackSubmitted) {
        val lastFeedback = userPrefs.lastFeedbackTimestamp
        val now = System.currentTimeMillis()
        // Pokaż feedback tylko raz na 12 godzin i jeśli nie wysłano go przed chwilą
        !feedbackSubmitted && (now - lastFeedback) > (12 * 60 * 60 * 1000)
    }
    
    // Konfiguracja kart dashboardu z preferencji
    val enabledCards = userPrefs.enabledDashboardCards
    val storedOrder = userPrefs.dashboardCardOrder.split(",").filter { it.isNotBlank() }
    // Normalizacja: dodaj brakujące włączone klucze na końcu
    val allKnownKeys = listOf("frost", "weather", "trend", "uv", "watering", "storm")
    val cardOrder = storedOrder.filter { it in allKnownKeys } +
        allKnownKeys.filter { it !in storedOrder }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // --- Offline cache banner ---
        if (state.isFromCache && state.cacheTimestamp != null) {
            OfflineCacheBanner(cacheTimestamp = state.cacheTimestamp)
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGarden) Icons.Default.Agriculture else Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = if (isGarden) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isGarden) stringResource(R.string.home_garden_title) else stringResource(R.string.home_car_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Renderuj karty w kolejności zgodnej z dashboardCardOrder, tylko jeśli są włączone
        val yearAgoData by viewModel.yearAgoData.collectAsState()

        cardOrder.forEach { cardKey ->
            if (cardKey !in enabledCards) return@forEach
            when (cardKey) {
                "frost" -> {
                    if (isSummer) {
                        SummerRiskCard(state, userPrefs, isGarden, enabledCards)
                    } else {
                        FrostWarningCard(
                            hasRisk = state.hasFrostRisk,
                            frostProbability = state.frostProbability,
                            warningMessage = state.warningMessage,
                            windSpeed = state.weather.current.windSpeed,
                            isGarden = isGarden
                        )
                        if (isGarden && state.hasFrostRisk) {
                            GardenAdviceCard(state.minTemp)
                        }
                    }
                }
                "weather" -> {
                    MinTemperatureCard(minTemp = state.minTemp, useFahrenheit = state.useFahrenheit, isGarden = isGarden)
                    HourlyForecastSection(hourly = state.weather.hourly, useFahrenheit = state.useFahrenheit)
                    // Daily 7-day forecast (C1)
                    state.weather.daily?.let { daily ->
                        Spacer(Modifier.height(4.dp))
                        DailyForecastSection(
                            daily = daily,
                            useFahrenheit = state.useFahrenheit,
                            userPrefs = userPrefs
                        )
                    }
                }
                "trend" -> {
                    HistoricalComparisonCard(data = yearAgoData, useFahrenheit = state.useFahrenheit)
                }
                "uv" -> {
                    if (state.weather.current.uvIndex >= 3.0) {
                        UvIndexCard(state.weather.current.uvIndex)
                    }
                }
                "watering" -> {
                    if (isGarden && isSummer) {
                        val needsWatering = state.weather.daily?.let {
                            SummerCalculations.needsWatering(it.precipitationSum.firstOrNull() ?: 0.0, 26.0)
                        } ?: false
                        if (needsWatering) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.watering_needed),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                "storm" -> {
                    if (isSummer && SummerCalculations.hasStormOrHailRisk(state.weather.current.weatherCode)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Thunderstorm, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.storm_alert_label),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // FEEDBACK z animacją zanikania
        AnimatedVisibility(
            visible = showFeedback,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FeedbackSection(onCorrection = { isPositive ->
                feedbackSubmitted = true // Ukrywamy natychmiast
                if (!isPositive) {
                    val adjustment = if (state.hasFrostRisk) -0.1 else 0.1
                    settingsViewModel.updateSensitivity((userPrefs.sensitivity + adjustment).coerceIn(0.5, 2.0))
                }
                settingsViewModel.updateLastFeedbackTimestamp(System.currentTimeMillis())
                // Odświeżamy dane po małym opóźnieniu, aby animacja ukrywania mogła się zakończyć
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    viewModel.refreshData()
                }
            })
        }
        
        if (feedbackSubmitted) {
            Text(
                text = stringResource(R.string.feedback_thanks),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        if (!isPro) {
            MonetizationBanner(adUnitResId = R.string.admob_banner_home_unit_id)
        }
    }

    // KALIBRACJA ALGORYTMU - Dialog zbierania feedbacku
    if (state.showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCalibrationDialog() },
            title = {
                Text(
                    text = stringResource(R.string.calibration_dialog_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.calibration_dialog_question),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.calibration_dialog_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.submitCalibrationFeedback(true)
                }) {
                    Text(stringResource(R.string.calibration_dialog_yes), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.submitCalibrationFeedback(false)
                }) {
                    Text(stringResource(R.string.calibration_dialog_no), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }
}

/** Banner shown when weather data is loaded from local cache (offline mode). */
@Composable
fun OfflineCacheBanner(cacheTimestamp: Long) {
    val timeStr = remember(cacheTimestamp) {
        // Create a fresh SimpleDateFormat inside remember(cacheTimestamp) so it is not
        // shared across threads; this block also re-runs whenever the timestamp changes
        // across recompositions, preventing stale values.
        SimpleDateFormat("HH:mm, d MMM", Locale.getDefault()).format(Date(cacheTimestamp))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.offline_cache_banner_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.offline_cache_banner_desc, timeStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Horizontal scrolling row of daily forecast cards (C1).
 * Shows up to 7 days with min/max temperature and frost-risk colour coding.
 */
@Composable
fun DailyForecastSection(
    daily: DailyForecast,
    useFahrenheit: Boolean,
    userPrefs: UserPreferences
) {
    Column {
        Text(
            text = stringResource(R.string.daily_forecast_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val days = daily.time.take(7)
            itemsIndexed(days, key = { _, d -> d }) { index, dateStr ->
                val minTemp = daily.temperatureMin.getOrNull(index) ?: return@itemsIndexed
                val maxTemp = daily.temperatureMax.getOrNull(index) ?: return@itemsIndexed
                val wCode = daily.weatherCode.getOrNull(index) ?: 0
                val hasRisk = minTemp <= userPrefs.tempThreshold
                val displayMin = if (useFahrenheit) WeatherCalculations.celsiusToFahrenheit(minTemp) else minTemp
                val displayMax = if (useFahrenheit) WeatherCalculations.celsiusToFahrenheit(maxTemp) else maxTemp
                val unit = if (useFahrenheit) "°F" else "°C"
                // Parse date string (yyyy-MM-dd)
                val dayLabel = remember(dateStr) {
                    runCatching {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsed = sdf.parse(dateStr)
                        parsed?.let { SimpleDateFormat("E", Locale.getDefault()).format(it) }
                            ?: dateStr.takeLast(2)
                    }.getOrDefault(dateStr.takeLast(2))
                }
                val bgColor = when {
                    hasRisk && minTemp < -2 -> RiskVeryHigh.copy(alpha = 0.18f)
                    hasRisk -> RiskHigh.copy(alpha = 0.15f)
                    minTemp < 4 -> RiskModerate.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .width(56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        getWeatherIcon(wCode),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (hasRisk) RiskHigh else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "%.0f$unit".format(Locale.US, displayMax),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "%.0f$unit".format(Locale.US, displayMin),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasRisk) RiskHigh else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (hasRisk) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasRisk) {
                        Spacer(Modifier.height(2.dp))
                        Icon(
                            Icons.Default.AcUnit,
                            contentDescription = stringResource(R.string.frost_risk_icon),
                            modifier = Modifier.size(12.dp),
                            tint = RiskHigh
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummerRiskCard(
    state: HomeUiState.Success,
    prefs: UserPreferences,
    isGarden: Boolean,
    enabledCards: Set<String> = emptySet()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val summerMsg = SummerCalculations.getSummerWarningMessage(
        context = context,
        currentTemp = state.weather.current.temperature,
        weatherCode = state.weather.current.weatherCode,
        uvIndex = state.weather.current.uvIndex,
        heatThreshold = prefs.heatThreshold,
        appMode = state.appMode
    )
    val hasHeat = SummerCalculations.hasHeatRisk(state.weather.current.temperature, prefs.heatThreshold)
    val hasStorm = SummerCalculations.hasStormOrHailRisk(state.weather.current.weatherCode)
    
    // Nie pokazuj podlewania w SummerRiskCard, jeśli karta "watering" jest włączona osobno
    val showWatering = "watering" !in enabledCards && isGarden && state.weather.daily != null
    val needsWatering = if (showWatering) {
        SummerCalculations.needsWatering(state.weather.daily.precipitationSum.firstOrNull() ?: 0.0, 26.0)
    } else false

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasStorm || hasHeat) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (hasStorm) Icons.Default.Thunderstorm else if (hasHeat) Icons.Default.WbSunny else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = summerMsg.ifEmpty { stringResource(R.string.summer_optimal) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (needsWatering) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.watering_needed),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun UvIndexCard(uvIndex: Double) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.uv_label, "%.1f".format(Locale.US, uvIndex), SummerCalculations.getUvDescription(context, uvIndex)),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = SummerCalculations.getUvAdvice(context, uvIndex),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GardenAdviceCard(minTemp: Double) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.garden_status_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = WeatherCalculations.getGardenTip(context, minTemp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FeedbackSection(onCorrection: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.feedback_question),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onCorrection(true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.feedback_yes), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = { onCorrection(false) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.feedback_no), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                text = stringResource(R.string.feedback_calibration),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FrostWarningCard(hasRisk: Boolean, frostProbability: Int, warningMessage: String, windSpeed: Double, isGarden: Boolean) {
    // Compute risk-level color for gradient
    val riskColor = when {
        frostProbability >= 75 -> RiskVeryHigh
        frostProbability >= 50 -> RiskHigh
        frostProbability >= 25 -> RiskModerate
        hasRisk -> RiskModerate
        else -> RiskNone
    }
    val targetGradientStart = if (hasRisk) riskColor.copy(alpha = 0.25f) else RiskNone.copy(alpha = 0.12f)
    val animatedGradientStart by androidx.compose.animation.animateColorAsState(
        targetValue = targetGradientStart,
        animationSpec = tween(durationMillis = 600),
        label = "risk_gradient_start"
    )
    val containerColor = if (hasRisk) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val icon = if (hasRisk) (if (isGarden) Icons.Default.Warning else Icons.Default.AcUnit) else Icons.Default.CheckCircle
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(animatedGradientStart, containerColor)
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (hasRisk) stringResource(R.string.frost_risk_icon) else stringResource(R.string.no_frost_risk_icon),
                    modifier = Modifier.size(64.dp),
                    tint = if (hasRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${frostProbability}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = if (hasRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when (WeatherCalculations.getFrostProbabilityLevel(frostProbability)) {
                        WeatherCalculations.FrostProbabilityLevel.VERY_HIGH -> stringResource(R.string.frost_probability_very_high)
                        WeatherCalculations.FrostProbabilityLevel.HIGH -> stringResource(R.string.frost_probability_high)
                        WeatherCalculations.FrostProbabilityLevel.MODERATE -> stringResource(R.string.frost_probability_moderate)
                        WeatherCalculations.FrostProbabilityLevel.LOW -> stringResource(R.string.frost_probability_low)
                        WeatherCalculations.FrostProbabilityLevel.MINIMAL -> stringResource(R.string.frost_probability_minimal)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                // Risk level bar
                val animatedProgress by animateFloatAsState(
                    targetValue = frostProbability / 100f,
                    animationSpec = tween(durationMillis = 800),
                    label = "risk_progress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(0.8f).height(8.dp).clip(CircleShape),
                    color = riskColor,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isGarden && hasRisk) warningMessage.replace("Wysokie ryzyko szronu!", stringResource(R.string.risk_garden)) else warningMessage,
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.ExtraBold, 
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (windSpeed > 10.0) {
                    Spacer(Modifier.height(12.dp))
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), shape = CircleShape) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Air, contentDescription = stringResource(R.string.wind_icon_description), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.wind_speed_format, "%.1f".format(Locale.US, windSpeed)),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinTemperatureCard(minTemp: Double, useFahrenheit: Boolean, isGarden: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isGarden) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isGarden) stringResource(R.string.night_min_garden) else stringResource(R.string.night_min_temp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val tempValue = if (useFahrenheit) WeatherCalculations.celsiusToFahrenheit(minTemp) else minTemp
                Text(
                    text = "%.1f%s".format(Locale.US, tempValue, if (useFahrenheit) "°F" else "°C"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(imageVector = if (isGarden) Icons.Default.NaturePeople else Icons.Default.Nightlight, contentDescription = null, modifier = Modifier.size(40.dp), tint = if (isGarden) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun HourlyForecastSection(hourly: HourlyForecast, useFahrenheit: Boolean) {
    Column {
        Text(
            text = stringResource(R.string.hourly_forecast),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(hourly.time.take(24)) { index, timeStr ->
                val temp = hourly.temperature[index] ?: return@itemsIndexed
                val displayTemp = if (useFahrenheit) WeatherCalculations.celsiusToFahrenheit(temp) else temp
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeStr.substringAfter("T"),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(getWeatherIcon(hourly.weatherCode[index] ?: 0), contentDescription = null, modifier = Modifier.size(24.dp).padding(vertical = 4.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "%.0f°".format(Locale.US, displayTemp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryOptimizationWarning() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    if (!isIgnoring) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryAlert, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.battery_warning_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.battery_warning_desc),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.battery_fix_btn), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(24.dp))
        Text(
            message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry_button), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun HistoricalComparisonCard(data: HistoricalComparisonData?, useFahrenheit: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_comparison_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                if (data == null) {
                    Text(
                        text = stringResource(R.string.history_comparison_no_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.history_comparison_min_temp,
                            WeatherCalculations.formatTemperature(data.minTemp, useFahrenheit)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (data.hadFrost) stringResource(R.string.history_comparison_frost_yes)
                               else stringResource(R.string.history_comparison_frost_no),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val absDiff = kotlin.math.abs(data.tempDifference)
                    val diffText = WeatherCalculations.formatTemperature(absDiff, useFahrenheit)
                    Text(
                        text = if (data.tempDifference >= 0) stringResource(R.string.history_comparison_warmer, diffText)
                               else stringResource(R.string.history_comparison_colder, diffText),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (data.tempDifference >= 0) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun getWeatherIcon(code: Int): ImageVector = when (code) {
    0 -> Icons.Default.WbSunny
    1, 2, 3 -> Icons.Default.Cloud
    45, 48 -> Icons.Default.WbCloudy
    in 51..67 -> Icons.Default.WaterDrop
    in 71..77 -> Icons.Default.AcUnit
    in 80..82 -> Icons.Default.BeachAccess
    95, 96, 99 -> Icons.Default.Thunderstorm
    else -> Icons.Default.Cloud
}

@Composable
private fun LocationSelectorRow(
    viewModel: HomeViewModel,
    prefs: UserPreferences
) {
    val savedLocations by viewModel.savedLocations.collectAsState()
    
    if (savedLocations.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = prefs.activeLocationId == 0,
                onClick = { viewModel.switchLocation(0) },
                label = {
                    Text(
                        text = if (prefs.isManualLocationEnabled) prefs.manualLocationName
                               else stringResource(R.string.location_gps_auto),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        items(savedLocations.size) { index ->
            val loc = savedLocations[index]
            FilterChip(
                selected = prefs.activeLocationId == loc.id,
                onClick = { viewModel.switchLocation(loc.id) },
                label = {
                    Text(
                        text = loc.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}
