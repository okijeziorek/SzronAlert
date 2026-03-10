package pl.oki.frostalert.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.FrostDatabase
import pl.oki.frostalert.data.repository.HistoryRepository

data class HistoryUiState(
    val monthlyStats: List<pl.oki.frostalert.data.repository.MonthlyStat> = emptyList(),
    val absoluteMinTemp: Double? = null,
    val isLoading: Boolean = false
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HistoryRepository(FrostDatabase.getDatabase(application).temperatureDao())

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val stats = repository.getSeasonStats()
            val absMin = repository.getAbsoluteMinTemp()
            _uiState.value = HistoryUiState(monthlyStats = stats, absoluteMinTemp = absMin, isLoading = false)
        }
    }
}
