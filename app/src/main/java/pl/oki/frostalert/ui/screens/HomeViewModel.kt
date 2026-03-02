package pl.oki.frostalert.ui.screens

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.data.remote.WeatherResponse
import pl.oki.frostalert.utils.WeatherCalculations
import pl.oki.frostalert.widget.FrostGlanceWidget
import kotlin.coroutines.resume

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
                val settingsDataStore = SettingsDataStore(context)
                val userPreferences = settingsDataStore.userPreferencesFlow.first()
                
                val lat: Double
                val lon: Double
                
                if (userPreferences.isManualLocationEnabled) {
                    lat = userPreferences.manualLatitude
                    lon = userPreferences.manualLongitude
                } else {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    val location = suspendCancellableCoroutine<Location?> { continuation ->
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { continuation.resume(it) }
                                .addOnFailureListener { continuation.resume(null) }
                        } catch (e: SecurityException) {
                            continuation.resume(null)
                        }
                    }
                    
                    if (location != null) {
                        lat = location.latitude
                        lon = location.longitude
                    } else {
                        _uiState.value = HomeUiState.Error("Włącz GPS lub ustaw miasto ręcznie.")
                        _isRefreshing.value = false
                        return@launch
                    }
                }

                val weather = OpenMeteoApi.getWeather(lat, lon)
                val minTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                
                val tempThreshold = if (userPreferences.isAutoModeEnabled) 1.0 else userPreferences.tempThreshold
                val humidityThreshold = if (userPreferences.isAutoModeEnabled) 75.0 else userPreferences.humidityThreshold.toDouble()
                val precipitationThreshold = if (userPreferences.isAutoModeEnabled) 0.2 else userPreferences.precipitationThreshold

                val hasRisk = WeatherCalculations.hasFrostRisk(
                    minTemp, weather.current.humidity, weather.current.precipitation, 
                    weather.current.weatherCode, tempThreshold, humidityThreshold, precipitationThreshold
                )

                // Zapis do bazy
                val db = FrostDatabase.getDatabase(context)
                db.temperatureDao().insert(TemperatureRecord(
                    timestamp = System.currentTimeMillis(),
                    minTemp = minTemp,
                    hasRisk = hasRisk
                ))

                // NAPRAWIONO: Prawidłowe odświeżanie widgetu Glance
                FrostGlanceWidget().updateAll(context)

                val warningMessage = WeatherCalculations.getWarningMessage(
                    minTemp, weather.current.humidity, weather.current.precipitation, 
                    weather.current.weatherCode, tempThreshold, humidityThreshold, precipitationThreshold,
                    useFahrenheit = userPreferences.useFahrenheit
                )

                _uiState.value = HomeUiState.Success(
                    weather, 
                    minTemp, 
                    hasRisk, 
                    warningMessage, 
                    userPreferences.useFahrenheit
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Błąd połączenia.")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
