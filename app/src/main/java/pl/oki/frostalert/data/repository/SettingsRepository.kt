package pl.oki.frostalert.data.repository

import kotlinx.coroutines.flow.Flow
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.UserPreferences

class SettingsRepository(private val settingsDataStore: SettingsDataStore) {

    val userPreferencesFlow: Flow<UserPreferences> = settingsDataStore.userPreferencesFlow

    suspend fun updateAutoModeEnabled(isEnabled: Boolean) {
        settingsDataStore.updateAutoModeEnabled(isEnabled)
    }

    suspend fun updateTempThreshold(threshold: Double) {
        settingsDataStore.updateTempThreshold(threshold)
    }

    suspend fun updateHumidityThreshold(threshold: Int) {
        settingsDataStore.updateHumidityThreshold(threshold)
    }

    suspend fun updatePrecipitationThreshold(threshold: Double) {
        settingsDataStore.updatePrecipitationThreshold(threshold)
    }

    suspend fun updateAlertStartHour(hour: Int) {
        settingsDataStore.updateAlertStartHour(hour)
    }

    suspend fun updateAlertEndHour(hour: Int) {
        settingsDataStore.updateAlertEndHour(hour)
    }

    suspend fun updateCarModeEnabled(isEnabled: Boolean) {
        settingsDataStore.updateCarModeEnabled(isEnabled)
    }

    suspend fun updateCarModeHour(hour: Int) {
        settingsDataStore.updateCarModeHour(hour)
    }

    suspend fun updateTheme(value: Int) {
        settingsDataStore.updateTheme(value)
    }

    suspend fun updateManualLocation(isEnabled: Boolean, lat: Double, lon: Double, name: String) {
        settingsDataStore.updateManualLocation(isEnabled, lat, lon, name)
    }

    suspend fun setOnboardingCompleted(isCompleted: Boolean) {
        settingsDataStore.setOnboardingCompleted(isCompleted)
    }

    suspend fun updateUseFahrenheit(useFahrenheit: Boolean) {
        settingsDataStore.updateUseFahrenheit(useFahrenheit)
    }
}
