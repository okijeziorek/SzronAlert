package pl.oki.frostalert.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    val isDarkThemeEnabled: Boolean
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
        val IS_DARK_THEME_ENABLED = booleanPreferencesKey("is_dark_theme_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                tempThreshold = preferences[Keys.TEMP_THRESHOLD] ?: 2.0,
                humidityThreshold = preferences[Keys.HUMIDITY_THRESHOLD] ?: 80,
                precipitationThreshold = preferences[Keys.PRECIPITATION_THRESHOLD] ?: 0.1,
                alertStartHour = preferences[Keys.ALERT_START_HOUR] ?: 18,
                alertEndHour = preferences[Keys.ALERT_END_HOUR] ?: 8,
                ignoreUntil = preferences[Keys.IGNORE_UNTIL] ?: 0L,
                carModeHour = preferences[Keys.CAR_MODE_HOUR] ?: 7,
                isCarModeEnabled = preferences[Keys.IS_CAR_MODE_ENABLED] ?: false,
                isAutoModeEnabled = preferences[Keys.IS_AUTO_MODE_ENABLED] ?: true,
                isDarkThemeEnabled = preferences[Keys.IS_DARK_THEME_ENABLED] ?: false
            )
        }

    suspend fun updateAutoModeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[Keys.IS_AUTO_MODE_ENABLED] = isEnabled
        }
    }

    suspend fun updateCarModeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[Keys.IS_CAR_MODE_ENABLED] = isEnabled
        }
    }

    suspend fun updateTempThreshold(value: Double) {
        context.dataStore.edit { settings ->
            settings[Keys.TEMP_THRESHOLD] = value
        }
    }

    suspend fun updateHumidityThreshold(value: Int) {
        context.dataStore.edit { settings ->
            settings[Keys.HUMIDITY_THRESHOLD] = value
        }
    }

    suspend fun updatePrecipitationThreshold(value: Double) {
        context.dataStore.edit { settings ->
            settings[Keys.PRECIPITATION_THRESHOLD] = value
        }
    }

    suspend fun updateAlertStartHour(value: Int) {
        context.dataStore.edit { settings ->
            settings[Keys.ALERT_START_HOUR] = value
        }
    }

    suspend fun updateAlertEndHour(value: Int) {
        context.dataStore.edit { settings ->
            settings[Keys.ALERT_END_HOUR] = value
        }
    }

    suspend fun updateIgnoreUntil(timestamp: Long) {
        context.dataStore.edit { settings ->
            settings[Keys.IGNORE_UNTIL] = timestamp
        }
    }

    suspend fun updateCarModeHour(value: Int) {
        context.dataStore.edit { settings ->
            settings[Keys.CAR_MODE_HOUR] = value
        }
    }

    suspend fun updateDarkThemeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[Keys.IS_DARK_THEME_ENABLED] = isEnabled
        }
    }
}