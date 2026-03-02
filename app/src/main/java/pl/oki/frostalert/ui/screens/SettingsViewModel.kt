package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.data.repository.SettingsRepository

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences(
                tempThreshold = 1.0,
                humidityThreshold = 75,
                precipitationThreshold = 0.2,
                alertStartHour = 19,
                alertEndHour = 8,
                ignoreUntil = 0L,
                carModeHour = 7,
                isAutoModeEnabled = true,
                isCarModeEnabled = true,
                theme = 2,
                isManualLocationEnabled = false,
                manualLatitude = 52.2297,
                manualLongitude = 21.0122,
                manualLocationName = "Warszawa",
                isOnboardingCompleted = false,
                useFahrenheit = false
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

    fun updateManualLocation(isEnabled: Boolean, lat: Double, lon: Double, name: String) {
        viewModelScope.launch {
            settingsRepository.updateManualLocation(isEnabled, lat, lon, name)
        }
    }

    fun setOnboardingCompleted(isCompleted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(isCompleted)
        }
    }

    fun updateUseFahrenheit(useFahrenheit: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateUseFahrenheit(useFahrenheit)
        }
    }
}
