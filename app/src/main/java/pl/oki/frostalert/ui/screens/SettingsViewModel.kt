package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.data.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val geofenceRegistrar: pl.oki.frostalert.geofence.GeofenceRegistrarContract,
    @javax.inject.Named("app_context") private val appContext: android.content.Context
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences?> = settingsRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateLastFeedbackTimestamp(timestamp: Long) {
        viewModelScope.launch {
            settingsRepository.updateLastFeedbackTimestamp(timestamp)
        }
    }

    fun updateIgnoreUntil(timestamp: Long) {
        viewModelScope.launch {
            settingsRepository.updateIgnoreUntil(timestamp)
        }
    }

    fun updateIsProForced(isForced: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateIsProForced(isForced)
        }
    }

    fun updateAutoModeEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoModeEnabled(isEnabled)
        }
    }

    fun updateMataOptionEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMataOptionEnabled(isEnabled)
        }
    }

    fun updateAppMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.updateAppMode(mode)
        }
    }

    fun updateHeatThreshold(threshold: Double) {
        viewModelScope.launch {
            settingsRepository.updateHeatThreshold(threshold)
        }
    }

    fun updateStormAlertEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateStormAlertEnabled(isEnabled)
        }
    }

    fun updateWateringReminderEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWateringReminderEnabled(isEnabled)
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

    fun updateSensitivity(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateSensitivity(value)
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
            // Jeśli włączone geofencing, przerejestruj geofence dla nowej lokalizacji
            try {
                val locationRepo = pl.oki.frostalert.data.repository.LocationRepository(appContext, pl.oki.frostalert.data.local.SettingsDataStore(appContext))
                geofenceRegistrar.registerForCurrentLocation(locationRepo)
            } catch (_: Exception) {}
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

    fun updateGeofencingEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateGeofencingEnabled(isEnabled)
            if (isEnabled) {
                try {
                    val locationRepo = pl.oki.frostalert.data.repository.LocationRepository(appContext, pl.oki.frostalert.data.local.SettingsDataStore(appContext))
                    geofenceRegistrar.registerForCurrentLocation(locationRepo)
                } catch (_: Exception) {}
            } else {
                geofenceRegistrar.unregister()
            }
        }
    }

    fun updateGeofenceRadius(radiusMeters: Double) {
        viewModelScope.launch {
            settingsRepository.updateGeofenceRadius(radiusMeters)
        }
    }

    fun updateTrendChangeNotificationsEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTrendChangeNotificationsEnabled(isEnabled)
        }
    }

    fun updateLastTrend(trend: pl.oki.frostalert.utils.TrendCalculations.TrendDirection?) {
        viewModelScope.launch {
            settingsRepository.updateLastTrend(trend)
        }
    }
}
