package pl.oki.frostalert.ui.screens

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.data.remote.WeatherResponse
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.data.repository.WeatherCacheRepository
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.CalibrationEngine
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.widget.WidgetSyncHelper
import pl.oki.frostalert.R
import javax.inject.Inject
import pl.oki.frostalert.data.local.CalibrationDao

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val weather: WeatherResponse,
        val minTemp: Double,
        val hasFrostRisk: Boolean,
        val frostProbability: Int,
        val warningMessage: String,
        val appMode: Int,
        val useFahrenheit: Boolean,
        val showCalibrationDialog: Boolean = false,
        val lastWeatherData: WeatherDataForCalibration? = null,
        /** True when the weather data comes from local cache (device is offline). */
        val isFromCache: Boolean = false,
        /** Epoch-ms timestamp of the cached response; null when data is live. */
        val cacheTimestamp: Long? = null
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

data class HistoricalComparisonData(
    val hadFrost: Boolean,
    val minTemp: Double,
    val tempDifference: Double // positive = warmer now
)

data class WeatherDataForCalibration(
    val temperature: Double,
    val humidity: Int,
    val weatherCode: Int,
    val locationLat: Double,
    val locationLon: Double,
    val predictedRisk: Boolean,
    val usedThreshold: Double,
    val usedHumidityThreshold: Int,
    val usedSensitivity: Double
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val locationRepository: LocationRepository,
    private val temperatureDao: TemperatureDao,
    private val calibrationDao: CalibrationDao,
    private val savedLocationDao: pl.oki.frostalert.data.local.SavedLocationDao,
    private val weatherCacheRepository: WeatherCacheRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        /** Minimum hours between calibration feedback prompts. */
        private const val MIN_HOURS_BETWEEN_FEEDBACK = 12
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _weatherData = MutableStateFlow<WeatherResponse?>(null)
    /** Non-null when displayed weather comes from local cache (offline mode). */
    private val _cacheTimestamp = MutableStateFlow<Long?>(null)

    private val _yearAgoData = MutableStateFlow<HistoricalComparisonData?>(null)
    val yearAgoData: StateFlow<HistoricalComparisonData?> = _yearAgoData.asStateFlow()
    
    // Saved locations for location selector
    val savedLocations: StateFlow<List<pl.oki.frostalert.data.local.SavedLocation>> =
        savedLocationDao.getAllLocations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Stan dialogu kalibracji
    private val _showCalibrationDialog = MutableStateFlow(false)
    val showCalibrationDialog: StateFlow<Boolean> = _showCalibrationDialog.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        settingsDataStore.userPreferencesFlow,
        _weatherData,
        _isRefreshing,
        _showCalibrationDialog,
        _cacheTimestamp
    ) { prefs, weather, refreshing, showDialog, cacheTs ->
        if (weather == null) {
            if (refreshing) HomeUiState.Loading else HomeUiState.Error(context.getString(R.string.home_pull_to_refresh))
        } else {
            val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
            val hasRisk = WeatherCalculations.hasFrostRisk(
                minTemp, weather.current.humidity, weather.current.precipitation, 
                weather.current.weatherCode, 
                if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                if (prefs.isAutoModeEnabled) 75.0 else prefs.humidityThreshold.toDouble(),
                if (prefs.isAutoModeEnabled) 0.2 else prefs.precipitationThreshold,
                sensitivity = prefs.sensitivity,
                windSpeed = weather.current.windSpeed,
                appMode = prefs.appMode
            )

            val warningMessage = WeatherCalculations.getWarningMessage(
                minTemp, weather.current.humidity, weather.current.precipitation, 
                weather.current.weatherCode, 
                if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                if (prefs.isAutoModeEnabled) 75.0 else prefs.humidityThreshold.toDouble(),
                if (prefs.isAutoModeEnabled) 0.2 else prefs.precipitationThreshold,
                sensitivity = prefs.sensitivity,
                windSpeed = weather.current.windSpeed,
                appMode = prefs.appMode,
                useFahrenheit = prefs.useFahrenheit
            )

            val frostProbability = WeatherCalculations.calculateFrostProbability(
                minTemp, weather.current.humidity, weather.current.precipitation,
                weather.current.weatherCode,
                if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                if (prefs.isAutoModeEnabled) 75.0 else prefs.humidityThreshold.toDouble(),
                if (prefs.isAutoModeEnabled) 0.2 else prefs.precipitationThreshold,
                sensitivity = prefs.sensitivity,
                windSpeed = weather.current.windSpeed,
                appMode = prefs.appMode
            )

            HomeUiState.Success(
                weather, minTemp, hasRisk, frostProbability, warningMessage, prefs.appMode, prefs.useFahrenheit,
                showCalibrationDialog = showDialog,
                isFromCache = cacheTs != null,
                cacheTimestamp = cacheTs
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState.Loading)

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                val prefs = settingsDataStore.userPreferencesFlow.first()
                
                // Determine effective location based on activeLocationId
                val location = if (prefs.activeLocationId > 0) {
                    val savedLoc = savedLocationDao.getById(prefs.activeLocationId)
                    if (savedLoc != null) {
                        android.location.Location("saved").apply {
                            latitude = savedLoc.latitude
                            longitude = savedLoc.longitude
                        }
                    } else {
                        locationRepository.getEffectiveLocation()
                    }
                } else {
                    locationRepository.getEffectiveLocation()
                }
                if (location == null) {
                    _isRefreshing.value = false
                    return@launch
                }

                val weatherResult = OpenMeteoApi.getWeather(location.latitude, location.longitude)
                
                if (weatherResult is AppResult.Success) {
                    val weather = weatherResult.data
                    _weatherData.value = weather
                    _cacheTimestamp.value = null // live data — no cache indicator

                    // Save fresh data to cache for offline use
                    try { weatherCacheRepository.saveWeatherData(weather) } catch (e: Exception) {
                        Log.w(TAG, "Failed to save weather to cache: ${e.message}")
                    }
                    
                    // Compute risk once for DB record; the UI recalculates reactively
                    // through the `uiState` combine whenever preferences or weather change.
                    val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                    val frostProbability = WeatherCalculations.calculateFrostProbability(
                        minTemp, weather.current.humidity, weather.current.precipitation,
                        weather.current.weatherCode,
                        if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                        if (prefs.isAutoModeEnabled) 75.0 else prefs.humidityThreshold.toDouble(),
                        if (prefs.isAutoModeEnabled) 0.2 else prefs.precipitationThreshold,
                        sensitivity = prefs.sensitivity,
                        windSpeed = weather.current.windSpeed,
                        appMode = prefs.appMode
                    )
                    val hasRisk = WeatherCalculations.hasFrostRisk(
                        minTemp, weather.current.humidity, weather.current.precipitation,
                        weather.current.weatherCode,
                        if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                        if (prefs.isAutoModeEnabled) 75.0 else prefs.humidityThreshold.toDouble(),
                        if (prefs.isAutoModeEnabled) 0.2 else prefs.precipitationThreshold,
                        sensitivity = prefs.sensitivity,
                        windSpeed = weather.current.windSpeed,
                        appMode = prefs.appMode
                    )
                    
                    temperatureDao.insert(TemperatureRecord(
                        timestamp = System.currentTimeMillis(),
                        minTemp = minTemp,
                        hasRisk = hasRisk,
                        frostProbability = frostProbability
                    ))
                    // Update both widgets atomically via WidgetSyncHelper
                    WidgetSyncHelper.updateAll(context)

                    // Load year-ago comparison data (±1 day window)
                    try {
                        val oneDayMs = 24 * 60 * 60 * 1000L
                        val yearAgoCal = java.util.Calendar.getInstance()
                        yearAgoCal.add(java.util.Calendar.YEAR, -1)
                        val yearAgoMs = yearAgoCal.timeInMillis
                        val yearAgoRecords = temperatureDao.getRecordsBetween(
                            yearAgoMs - oneDayMs,
                            yearAgoMs + oneDayMs
                        )
                        if (yearAgoRecords.isNotEmpty()) {
                            val bestRecord = yearAgoRecords.minBy { it.minTemp }
                            _yearAgoData.value = HistoricalComparisonData(
                                hadFrost = bestRecord.hasRisk,
                                minTemp = bestRecord.minTemp,
                                tempDifference = minTemp - bestRecord.minTemp
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load year-ago comparison: ${e.message}")
                    }

                    // KALIBRACJA: Sprawdź czy należy pokazać dialog feedbacku
                    val shouldAskForFeedback = CalibrationEngine.shouldAskForFeedback(
                        lastFeedbackTimestamp = prefs.lastFeedbackTimestamp,
                        predictedRisk = hasRisk,
                        minHoursBetweenFeedback = MIN_HOURS_BETWEEN_FEEDBACK
                    )

                    if (shouldAskForFeedback) {
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(1000)
                            _showCalibrationDialog.value = true
                        }
                    }
                } else {
                    // API failed — try to show cached data (offline mode)
                    Log.w(TAG, "API call failed, trying cache")
                    loadFromCache()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing weather data: ${e.message}", e)
                loadFromCache()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Loads the latest cached weather data and marks it as an offline source.
     * Called when the live API call fails or throws an exception.
     * Always updates so the freshest available cache is shown even if live data was
     * previously displayed.
     */
    private suspend fun loadFromCache() {
        runCatching {
            val cached = weatherCacheRepository.getAnyCachedWeather()
            if (cached != null) {
                val (cachedWeather, cacheTs) = cached
                // Always update: prefer cache with newer timestamp over stale live data
                val currentCacheTs = _cacheTimestamp.value
                if (currentCacheTs == null || cacheTs > currentCacheTs) {
                    _weatherData.value = cachedWeather
                    _cacheTimestamp.value = cacheTs
                    Log.i(TAG, "Loaded weather from cache (ts=$cacheTs)")
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to load from cache: ${e.message}")
        }
    }

    // KALIBRACJA ALGORYTMU
    fun showCalibrationDialog(weatherData: WeatherDataForCalibration) {
        _showCalibrationDialog.value = true
    }

    fun hideCalibrationDialog() {
        _showCalibrationDialog.value = false
    }

    fun switchLocation(locationId: Int) {
        viewModelScope.launch {
            settingsDataStore.updateActiveLocationId(locationId)
            refreshData()
        }
    }

    fun submitCalibrationFeedback(actualFrostOccurred: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = uiState.value
            if (currentState is HomeUiState.Success) {
                val weather = currentState.weather
                val prefs = settingsDataStore.userPreferencesFlow.first()
                val effectiveLocation = locationRepository.getEffectiveLocation()
                val feedbackLat = effectiveLocation?.latitude
                    ?: if (prefs.isManualLocationEnabled) prefs.manualLatitude else 0.0
                val feedbackLon = effectiveLocation?.longitude
                    ?: if (prefs.isManualLocationEnabled) prefs.manualLongitude else 0.0
                
                // Zapisz feedback do bazy danych
                val feedback = pl.oki.frostalert.data.local.CalibrationFeedback(
                    timestamp = System.currentTimeMillis(),
                    actualFrostOccurred = actualFrostOccurred,
                    predictedRisk = currentState.hasFrostRisk,
                    temperature = currentState.minTemp,
                    humidity = weather.current.humidity.toInt(),
                    weatherCode = weather.current.weatherCode,
                    locationLat = feedbackLat,
                    locationLon = feedbackLon,
                    appMode = prefs.appMode,
                    usedThreshold = if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                    usedHumidityThreshold = if (prefs.isAutoModeEnabled) 75 else prefs.humidityThreshold,
                    usedSensitivity = prefs.sensitivity
                )

                // Zapisz do bazy
                calibrationDao.insertFeedback(feedback)

                // Zaktualizuj timestamp ostatniego feedbacku
                settingsDataStore.updateLastFeedbackTimestamp(System.currentTimeMillis())

                // Ukryj dialog
                _showCalibrationDialog.value = false
            }
        }
    }
}
