package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.utils.TrendCalculations
import javax.inject.Inject

data class TrendUiState(
    val weeklyStats: TrendCalculations.WeeklyTrendStats? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TrendViewModel @Inject constructor(
    private val temperatureDao: TemperatureDao
) : ViewModel() {

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
}

