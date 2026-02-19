package pl.oki.frostalert.data

import kotlinx.coroutines.flow.Flow

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

    suspend fun updateDarkThemeEnabled(isEnabled: Boolean) {
        settingsDataStore.updateDarkThemeEnabled(isEnabled)
    }
}
