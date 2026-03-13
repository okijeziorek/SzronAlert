package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.CalibrationDao
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.utils.CalibrationEngine
import javax.inject.Inject

data class CalibrationUiState(
    val isLoading: Boolean = false,
    val calibrationResult: CalibrationEngine.CalibrationResult? = null,
    val error: String? = null,
    val shouldShowSuggestions: Boolean = false
)

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationDao: CalibrationDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    init {
        observeCalibrationData()
    }

    /**
     * Obserwuje zmiany w danych kalibracji i automatycznie aktualizuje stan
     */
    private fun observeCalibrationData() {
        viewModelScope.launch {
            calibrationDao.getRecentFeedback()
                .collect { feedbackList ->
                    try {
                        val result = CalibrationEngine.analyzeCalibration(feedbackList)

                        // Sprawdź czy warto pokazać sugestie
                        val prefs = settingsDataStore.userPreferencesFlow.first()
                        val shouldShowSuggestions = CalibrationEngine.shouldSuggestCalibration(
                            prefs.tempThreshold,
                            prefs.humidityThreshold,
                            prefs.sensitivity,
                            result
                        )

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            calibrationResult = result,
                            shouldShowSuggestions = shouldShowSuggestions,
                            error = null
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Błąd podczas analizy kalibracji: ${e.message}"
                        )
                    }
                }
        }
    }

    /**
     * Zastosowuje rekomendacje kalibracji do ustawień użytkownika
     */
    fun applyCalibrationRecommendations() {
        viewModelScope.launch {
            val result = _uiState.value.calibrationResult ?: return@launch

            try {
                settingsDataStore.updateTempThreshold(result.recommendedTempThreshold)
                settingsDataStore.updateHumidityThreshold(result.recommendedHumidityThreshold)
                settingsDataStore.updateSensitivity(result.recommendedSensitivity)

                // Stan zostanie automatycznie odświeżony przez observeCalibrationData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Błąd podczas stosowania rekomendacji: ${e.message}"
                )
            }
        }
    }

    /**
     * Czyści wszystkie dane kalibracji (dla celów testowych)
     */
    fun clearCalibrationData() {
        viewModelScope.launch {
            try {
                calibrationDao.clearAllFeedback()
                // Stan zostanie automatycznie odświeżony przez observeCalibrationData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Błąd podczas czyszczenia danych: ${e.message}"
                )
            }
        }
    }

    /**
     * Generuje wyjaśnienie dla użytkownika na temat wyników kalibracji
     */
    fun getCalibrationExplanation(): String {
        val result = _uiState.value.calibrationResult ?: return "Brak danych do analizy."
        return CalibrationEngine.generateCalibrationExplanation(result)
    }
}
