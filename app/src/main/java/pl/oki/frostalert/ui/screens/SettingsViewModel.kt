package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.SettingsRepository
import pl.oki.frostalert.data.UserPreferences

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences(
                tempThreshold = 2.0,
                humidityThreshold = 80,
                precipitationThreshold = 0.1,
                alertStartHour = 18,
                alertEndHour = 8,
                ignoreUntil = 0L,
                carModeHour = 7,
                isAutoModeEnabled = true,
                isCarModeEnabled = true,
                theme = 2
            )
        )

    fun updateAutoModeEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoModeEnabled(isEnabled)
        }
    }

    fun updateTempThreshold(threshold: Double) {
        viewModelScope.launch {
            settingsRepository.updateTempThreshold(threshold)
        }
    }

    fun updateHumidityThreshold(threshold: Int) {
        viewModelScope.launch {
            settingsRepository.updateHumidityThreshold(threshold)
        }
    }

    fun updatePrecipitationThreshold(threshold: Double) {
        viewModelScope.launch {
            settingsRepository.updatePrecipitationThreshold(threshold)
        }
    }

    fun updateAlertStartHour(hour: Int) {
        viewModelScope.launch {
            settingsRepository.updateAlertStartHour(hour)
        }
    }

    fun updateAlertEndHour(hour: Int) {
        viewModelScope.launch {
            settingsRepository.updateAlertEndHour(hour)
        }
    }

    fun updateCarModeEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateCarModeEnabled(isEnabled)
        }
    }

    fun updateCarModeHour(hour: Int) {
        viewModelScope.launch {
            settingsRepository.updateCarModeHour(hour)
        }
    }

    fun updateTheme(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateTheme(value)
        }
    }
}
