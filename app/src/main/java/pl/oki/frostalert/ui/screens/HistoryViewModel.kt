package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.TemperatureDao
import pl.oki.frostalert.data.local.TemperatureRecord
import pl.oki.frostalert.data.repository.HistoryRepository
import pl.oki.frostalert.data.repository.MonthlyStat
import pl.oki.frostalert.utils.AppResult
import javax.inject.Inject

data class HistoryUiState(
    val monthlyStats: List<MonthlyStat> = emptyList(),
    val absoluteMinTemp: Double? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository,
    private val temperatureDao: TemperatureDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // Udostępniamy rekordy jako reaktywny strumień, aby UI nie musiało dotykać bazy
    val recentRecords: StateFlow<List<TemperatureRecord>> = temperatureDao.getRecentRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val statsResult = repository.getSeasonStats()
            val minTempResult = repository.getAbsoluteMinTemp()

            if (statsResult is AppResult.Success && minTempResult is AppResult.Success) {
                _uiState.value = HistoryUiState(
                    monthlyStats = statsResult.data,
                    absoluteMinTemp = minTempResult.data,
                    isLoading = false
                )
            } else {
                val error = (statsResult as? AppResult.Error)?.error?.message 
                    ?: (minTempResult as? AppResult.Error)?.error?.message 
                    ?: "Wystąpił nieoczekiwany błąd danych."
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error
                )
            }
        }
    }
}
