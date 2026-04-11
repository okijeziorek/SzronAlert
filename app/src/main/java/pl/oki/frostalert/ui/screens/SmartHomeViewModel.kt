package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.repository.SmartHomeRepository
import pl.oki.frostalert.utils.AppResult
import javax.inject.Inject

data class SmartHomeUiState(
    val isEnabled: Boolean = false,
    val webhookUrl: String = "",
    val iftttKey: String = "",
    val threshold: Int = 50,
    val testResult: String? = null,
    val isTesting: Boolean = false
)

@HiltViewModel
class SmartHomeViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val smartHomeRepository: SmartHomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartHomeUiState())
    val uiState: StateFlow<SmartHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.userPreferencesFlow.first().let { prefs ->
                _uiState.value = SmartHomeUiState(
                    isEnabled = prefs.isSmartHomeEnabled,
                    webhookUrl = prefs.smartHomeWebhookUrl,
                    iftttKey = prefs.smartHomeIftttKey,
                    threshold = prefs.smartHomeThreshold
                )
            }
        }
    }

    fun updateEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isEnabled = enabled)
    }

    fun updateWebhookUrl(url: String) {
        _uiState.value = _uiState.value.copy(webhookUrl = url)
    }

    fun updateIftttKey(key: String) {
        _uiState.value = _uiState.value.copy(iftttKey = key)
    }

    fun updateThreshold(threshold: Int) {
        _uiState.value = _uiState.value.copy(threshold = threshold)
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            settingsDataStore.updateSmartHomeEnabled(state.isEnabled)
            settingsDataStore.updateSmartHomeWebhookUrl(state.webhookUrl)
            settingsDataStore.updateSmartHomeIftttKey(state.iftttKey)
            settingsDataStore.updateSmartHomeThreshold(state.threshold)
        }
    }

    fun testWebhook() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)
            val state = _uiState.value
            val result = smartHomeRepository.sendWebhook(
                state.webhookUrl, 75, -2.0, "Test"
            )
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = when (result) {
                    is AppResult.Success -> "✅ Webhook wysłany pomyślnie"
                    is AppResult.Error -> "❌ ${result.error.message}"
                }
            )
        }
    }

    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(testResult = null)
    }
}
