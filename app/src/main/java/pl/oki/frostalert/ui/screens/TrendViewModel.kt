package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.remote.OpenMeteoApi
import pl.oki.frostalert.utils.AppResult
import pl.oki.frostalert.utils.TrendCalculations
import javax.inject.Inject

data class TrendUiState(
    val weeklyStats: TrendCalculations.WeeklyTrendStats? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class FutureTrendUiState(
    val futureWeeklyStats: TrendCalculations.WeeklyTrendStats? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TrendViewModel @Inject constructor(
    private val temperatureDao: TemperatureDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    // Stan dla przyszłego trendu
    private val _futureTrendState = MutableStateFlow(FutureTrendUiState())
    val futureTrendState: StateFlow<FutureTrendUiState> = _futureTrendState.asStateFlow()

    // Reaktywny strumień statystyk trendu
    val trendState: StateFlow<TrendUiState> = temperatureDao.getRecentRecords()
        .map { records ->
            try {
                val stats = TrendCalculations.calculateWeeklyTrend(records)
                TrendUiState(weeklyStats = stats, isLoading = false)
            } catch (e: Exception) {
                TrendUiState(
                    isLoading = false,
                    errorMessage = "Błąd wyliczania trendu: ${e.message}"
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrendUiState(isLoading = true)
        )

    /**
     * Ładuje przyszły trend na podstawie współrzędnych
     */
    fun loadFutureTrend(latitude: Double, longitude: Double) {
        _futureTrendState.value = FutureTrendUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val weatherResult = OpenMeteoApi.getWeather(latitude, longitude)
                when (weatherResult) {
                    is AppResult.Success -> {
                        val futureStats = TrendCalculations.calculateFutureWeeklyTrend(weatherResult.data)
                        _futureTrendState.value = FutureTrendUiState(futureWeeklyStats = futureStats)
                    }
                    is AppResult.Error -> {
                        _futureTrendState.value = FutureTrendUiState(
                            errorMessage = "Błąd pobierania prognozy: ${weatherResult.error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _futureTrendState.value = FutureTrendUiState(
                    errorMessage = "Błąd ładowania przyszłego trendu: ${e.message}"
                )
            }
        }
    }
}
