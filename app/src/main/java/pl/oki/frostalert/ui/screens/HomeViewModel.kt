package pl.oki.frostalert.ui.screens

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
        val useFahrenheit: Boolean
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsDataStore = SettingsDataStore(context)
    private val locationRepository = LocationRepository(context, settingsDataStore)
    private val db = FrostDatabase.getDatabase(context)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                val userPreferences = settingsDataStore.userPreferencesFlow.first()
                val location = locationRepository.getEffectiveLocation()
                
                if (location == null) {
                    _uiState.value = HomeUiState.Error("Nie udało się pobrać lokalizacji. Sprawdź uprawnienia GPS.")
                    return@launch
                }

                val weatherResult = OpenMeteoApi.getWeather(location.latitude, location.longitude)
                
                when (weatherResult) {
                    is AppResult.Error -> {
                        _uiState.value = HomeUiState.Error(weatherResult.error.message)
                    }
                    is AppResult.Success -> {
                        val weather = weatherResult.data
                        val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                        
                        val tempThreshold = if (userPreferences.isAutoModeEnabled) 1.0 else userPreferences.tempThreshold
                        val humidityThreshold = if (userPreferences.isAutoModeEnabled) 75.0 else userPreferences.humidityThreshold.toDouble()
                        val precipitationThreshold = if (userPreferences.isAutoModeEnabled) 0.2 else userPreferences.precipitationThreshold
                        val sensitivity = userPreferences.sensitivity

                        val hasRisk = WeatherCalculations.hasFrostRisk(
                            minTemp, weather.current.humidity, weather.current.precipitation, 
                            weather.current.weatherCode, tempThreshold, humidityThreshold, precipitationThreshold,
                            sensitivity = sensitivity,
                            windSpeed = weather.current.windSpeed
                        )

                        db.temperatureDao().insert(TemperatureRecord(
                            timestamp = System.currentTimeMillis(),
                            minTemp = minTemp,
                            hasRisk = hasRisk
                        ))

                        FrostGlanceWidget().updateAll(context)

                        val warningMessage = WeatherCalculations.getWarningMessage(
                            minTemp, weather.current.humidity, weather.current.precipitation, 
                            weather.current.weatherCode, tempThreshold, humidityThreshold, precipitationThreshold,
                            sensitivity = sensitivity,
                            windSpeed = weather.current.windSpeed,
                            useFahrenheit = userPreferences.useFahrenheit
                        )

                        _uiState.value = HomeUiState.Success(
                            weather, minTemp, hasRisk, warningMessage, userPreferences.useFahrenheit
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Błąd połączenia z serwerem pogodowym.")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
