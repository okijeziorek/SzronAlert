package pl.oki.frostalert.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.oki.frostalert.R
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

data class ExtendedTrendUiState(
    val extendedStats: TrendCalculations.ExtendedTrendStats? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TrendViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val temperatureDao: TemperatureDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    // Stan dla przyszłego trendu
    private val _futureTrendState = MutableStateFlow(FutureTrendUiState())
    val futureTrendState: StateFlow<FutureTrendUiState> = _futureTrendState.asStateFlow()

    // Stan dla rozszerzonego trendu 14-dniowego
    private val _extendedTrendState = MutableStateFlow(ExtendedTrendUiState())
    val extendedTrendState: StateFlow<ExtendedTrendUiState> = _extendedTrendState.asStateFlow()

    // Reaktywny strumień statystyk trendu
    val trendState: StateFlow<TrendUiState> = temperatureDao.getRecentRecords()
        .map { records ->
            try {
                val stats = TrendCalculations.calculateWeeklyTrend(records, context = context)
                TrendUiState(weeklyStats = stats, isLoading = false)
            } catch (e: Exception) {
                TrendUiState(
                    isLoading = false,
                    errorMessage = context.getString(R.string.error_trend_calculation, e.message)
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
                        val futureStats = TrendCalculations.calculateFutureWeeklyTrend(weatherResult.data, context = context)
                        _futureTrendState.value = FutureTrendUiState(futureWeeklyStats = futureStats)
                    }
                    is AppResult.Error -> {
                        _futureTrendState.value = FutureTrendUiState(
                            errorMessage = context.getString(R.string.error_trend_forecast, weatherResult.error.message)
                        )
                    }
                }
            } catch (e: Exception) {
                _futureTrendState.value = FutureTrendUiState(
                    errorMessage = context.getString(R.string.error_trend_future, e.message)
                )
            }
        }
    }

    /**
     * Ładuje rozszerzony trend 14-dniowy na podstawie współrzędnych
     */
    fun loadExtendedForecast(latitude: Double, longitude: Double) {
        _extendedTrendState.value = ExtendedTrendUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val weatherResult = OpenMeteoApi.getWeather(latitude, longitude)
                when (weatherResult) {
                    is AppResult.Success -> {
                        val stats = TrendCalculations.calculateExtendedTrend(weatherResult.data, context = context)
                        _extendedTrendState.value = ExtendedTrendUiState(extendedStats = stats)
                    }
                    is AppResult.Error -> {
                        _extendedTrendState.value = ExtendedTrendUiState(
                            errorMessage = context.getString(R.string.error_trend_forecast, weatherResult.error.message)
                        )
                    }
                }
            } catch (e: Exception) {
                _extendedTrendState.value = ExtendedTrendUiState(
                    errorMessage = context.getString(R.string.error_trend_14day, e.message)
                )
            }
        }
    }
}
