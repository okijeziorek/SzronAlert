package pl.oki.frostalert.ui.screens

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.data.remote.WeatherResponse
import pl.oki.frostalert.data.repository.LocationRepository
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.widget.FrostGlanceWidget

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val weather: WeatherResponse,
        val minTemp: Double,
        val hasFrostRisk: Boolean,
        val warningMessage: String,
        val appMode: Int,
        val useFahrenheit: Boolean
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsDataStore = SettingsDataStore(context)
    private val locationRepository = LocationRepository(context, settingsDataStore)
    private val db = FrostDatabase.getDatabase(context)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Reaktywne połączenie ustawień i danych pogodowych
    private val _weatherData = MutableStateFlow<WeatherResponse?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        settingsDataStore.userPreferencesFlow,
        _weatherData,
        _isRefreshing
    ) { prefs, weather, refreshing ->
        if (weather == null) {
            if (refreshing) HomeUiState.Loading else HomeUiState.Error("Pociągnij, aby odświeżyć dane.")
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

            HomeUiState.Success(
                weather, minTemp, hasRisk, warningMessage, prefs.appMode, prefs.useFahrenheit
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
                val location = locationRepository.getEffectiveLocation()
                if (location == null) {
                    // Błąd obsłużony przez combine -> state
                    _isRefreshing.value = false
                    return@launch
                }

                val weatherResult = OpenMeteoApi.getWeather(location.latitude, location.longitude)
                
                if (weatherResult is AppResult.Success) {
                    _weatherData.value = weatherResult.data
                    
                    // Zapis do bazy (tylko przy fizycznym odświeżeniu)
                    val prefs = settingsDataStore.userPreferencesFlow.first()
                    val minTemp = WeatherCalculations.getNightMinTemp(weatherResult.data.hourly)
                    val hasRisk = WeatherCalculations.hasFrostRisk(
                        minTemp, weatherResult.data.current.humidity, weatherResult.data.current.precipitation, 
                        weatherResult.data.current.weatherCode, 1.0, 75.0, 0.2, 
                        sensitivity = prefs.sensitivity, appMode = prefs.appMode
                    )
                    
                    db.temperatureDao().insert(TemperatureRecord(
                        timestamp = System.currentTimeMillis(),
                        minTemp = minTemp,
                        hasRisk = hasRisk
                    ))
                    FrostGlanceWidget().updateAll(context)
                }
            } catch (e: Exception) {
                // Obsłużone przez stan
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
