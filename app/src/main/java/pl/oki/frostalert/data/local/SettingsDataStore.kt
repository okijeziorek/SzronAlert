package pl.oki.frostalert.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserPreferences(
    val tempThreshold: Double,
    val humidityThreshold: Int,
    val precipitationThreshold: Double,
    val alertStartHour: Int,
    val alertEndHour: Int,
    val ignoreUntil: Long,
    val carModeHour: Int,
    val isCarModeEnabled: Boolean,
    val isAutoModeEnabled: Boolean,
    val theme: Int,
    val isManualLocationEnabled: Boolean,
    val manualLatitude: Double,
    val manualLongitude: Double,
    val manualLocationName: String,
    val isOnboardingCompleted: Boolean,
    val useFahrenheit: Boolean
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val TEMP_THRESHOLD = doublePreferencesKey("temp_threshold")
        val HUMIDITY_THRESHOLD = intPreferencesKey("humidity_threshold")
        val PRECIPITATION_THRESHOLD = doublePreferencesKey("precipitation_threshold")
        val ALERT_START_HOUR = intPreferencesKey("alert_start_hour")
        val ALERT_END_HOUR = intPreferencesKey("alert_end_hour")
        val IGNORE_UNTIL = longPreferencesKey("ignore_until")
        val CAR_MODE_HOUR = intPreferencesKey("car_mode_hour")
        val IS_CAR_MODE_ENABLED = booleanPreferencesKey("is_car_mode_enabled")
        val IS_AUTO_MODE_ENABLED = booleanPreferencesKey("is_auto_mode_enabled")
        val THEME = intPreferencesKey("theme")
        val IS_MANUAL_LOCATION_ENABLED = booleanPreferencesKey("is_manual_location_enabled")
        val MANUAL_LATITUDE = doublePreferencesKey("manual_latitude")
        val MANUAL_LONGITUDE = doublePreferencesKey("manual_longitude")
        val MANUAL_LOCATION_NAME = stringPreferencesKey("manual_location_name")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                tempThreshold = preferences[Keys.TEMP_THRESHOLD] ?: 1.0,
                humidityThreshold = preferences[Keys.HUMIDITY_THRESHOLD] ?: 75,
                precipitationThreshold = preferences[Keys.PRECIPITATION_THRESHOLD] ?: 0.2,
                alertStartHour = preferences[Keys.ALERT_START_HOUR] ?: 19,
                alertEndHour = preferences[Keys.ALERT_END_HOUR] ?: 8,
                ignoreUntil = preferences[Keys.IGNORE_UNTIL] ?: 0L,
                carModeHour = preferences[Keys.CAR_MODE_HOUR] ?: 7,
                isCarModeEnabled = preferences[Keys.IS_CAR_MODE_ENABLED] ?: true,
                isAutoModeEnabled = preferences[Keys.IS_AUTO_MODE_ENABLED] ?: true,
                theme = preferences[Keys.THEME] ?: 2,
                isManualLocationEnabled = preferences[Keys.IS_MANUAL_LOCATION_ENABLED] ?: false,
                manualLatitude = preferences[Keys.MANUAL_LATITUDE] ?: 52.2297,
                manualLongitude = preferences[Keys.MANUAL_LONGITUDE] ?: 21.0122,
                manualLocationName = preferences[Keys.MANUAL_LOCATION_NAME] ?: "Warszawa",
                isOnboardingCompleted = preferences[Keys.IS_ONBOARDING_COMPLETED] ?: false,
                useFahrenheit = preferences[Keys.USE_FAHRENHEIT] ?: false
            )
        }

    suspend fun updateAutoModeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_AUTO_MODE_ENABLED] = isEnabled }
    }

    suspend fun updateCarModeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_CAR_MODE_ENABLED] = isEnabled }
    }

    suspend fun updateTempThreshold(value: Double) {
        context.dataStore.edit { it[Keys.TEMP_THRESHOLD] = value }
    }

    suspend fun updateHumidityThreshold(value: Int) {
        context.dataStore.edit { it[Keys.HUMIDITY_THRESHOLD] = value }
    }

    suspend fun updatePrecipitationThreshold(value: Double) {
        context.dataStore.edit { it[Keys.PRECIPITATION_THRESHOLD] = value }
    }

    suspend fun updateAlertStartHour(value: Int) {
        context.dataStore.edit { it[Keys.ALERT_START_HOUR] = value }
    }

    suspend fun updateAlertEndHour(value: Int) {
        context.dataStore.edit { it[Keys.ALERT_END_HOUR] = value }
    }

    suspend fun updateIgnoreUntil(timestamp: Long) {
        context.dataStore.edit { it[Keys.IGNORE_UNTIL] = timestamp }
    }

    suspend fun updateCarModeHour(value: Int) {
        context.dataStore.edit { it[Keys.CAR_MODE_HOUR] = value }
    }

    suspend fun updateTheme(value: Int) {
        context.dataStore.edit { it[Keys.THEME] = value }
    }

    suspend fun updateManualLocation(isEnabled: Boolean, lat: Double, lon: Double, name: String) {
        context.dataStore.edit { settings ->
            settings[Keys.IS_MANUAL_LOCATION_ENABLED] = isEnabled
            settings[Keys.MANUAL_LATITUDE] = lat
            settings[Keys.MANUAL_LONGITUDE] = lon
            settings[Keys.MANUAL_LOCATION_NAME] = name
        }
    }

    suspend fun setOnboardingCompleted(isCompleted: Boolean) {
        context.dataStore.edit { it[Keys.IS_ONBOARDING_COMPLETED] = isCompleted }
    }

    suspend fun updateUseFahrenheit(useFahrenheit: Boolean) {
        context.dataStore.edit { it[Keys.USE_FAHRENHEIT] = useFahrenheit }
    }
}
