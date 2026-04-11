package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.WeatherCalculations
import javax.inject.Inject

data class MapGridPoint(
    val latOffset: Int,
    val lonOffset: Int,
    val latitude: Double,
    val longitude: Double,
    val frostProbability: Int,
    val minTemp: Double
)

data class FrostMapUiState(
    val gridPoints: List<MapGridPoint> = emptyList(),
    val centerLat: Double = 0.0,
    val centerLon: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val radiusKm: Int = 50
)

@HiltViewModel
class FrostMapViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrostMapUiState())
    val uiState: StateFlow<FrostMapUiState> = _uiState.asStateFlow()

    fun loadMapData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val prefs = settingsDataStore.userPreferencesFlow.first()
                val centerLat = prefs.manualLatitude
                val centerLon = prefs.manualLongitude

                val result = OpenMeteoApi.getWeather(centerLat, centerLon)
                if (result is AppResult.Success) {
                    val weather = result.data
                    val baseMinTemp = WeatherCalculations.getNightMinTemp(weather.hourly)
                    val baseHumidity = weather.current.humidity
                    val basePrecip = weather.current.precipitation
                    val baseWeatherCode = weather.current.weatherCode
                    val baseWindSpeed = weather.current.windSpeed

                    val gridSize = 7
                    val halfGrid = gridSize / 2
                    // ~14 km lat step and ~14 km lon step at Polish mid-latitudes (~52°N)
                    val stepDegLat = 0.13
                    val stepDegLon = 0.20

                    val points = mutableListOf<MapGridPoint>()
                    for (row in -halfGrid..halfGrid) {
                        for (col in -halfGrid..halfGrid) {
                            val lat = centerLat + (row * stepDegLat)
                            val lon = centerLon + (col * stepDegLon)
                            val distanceFromCenter = kotlin.math.sqrt((row * row + col * col).toDouble())
                            // Synthetic spatial variation: approximates microclimate differences
                            // using position-based sine wave (±1.5°C) plus distance scaling (+0.15°C/cell)
                            val tempVariation = (kotlin.math.sin(lat * 10 + lon * 10) * 1.5) + (distanceFromCenter * 0.15)
                            val adjustedMinTemp = baseMinTemp + tempVariation

                            val probability = WeatherCalculations.calculateFrostProbability(
                                adjustedMinTemp, baseHumidity, basePrecip, baseWeatherCode,
                                if (prefs.isAutoModeEnabled) 1.0 else prefs.tempThreshold,
                                if (prefs.isAutoModeEnabled) 75.0 else prefs.humidityThreshold.toDouble(),
                                if (prefs.isAutoModeEnabled) 0.2 else prefs.precipitationThreshold,
                                windSpeed = baseWindSpeed,
                                appMode = prefs.appMode
                            )

                            points.add(MapGridPoint(row, col, lat, lon, probability, adjustedMinTemp))
                        }
                    }

                    _uiState.value = FrostMapUiState(
                        gridPoints = points,
                        centerLat = centerLat,
                        centerLon = centerLon,
                        isLoading = false,
                        radiusKm = 50
                    )
                } else if (result is AppResult.Error) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.error.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
}
