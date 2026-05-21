package pl.oki.frostalert.data.local

import android.content.Context
import android.util.Log
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
    val sensitivity: Double,
    val alertStartHour: Int,
    val alertEndHour: Int,
    val ignoreUntil: Long,
    val carModeHour: Int,
    val isCarModeEnabled: Boolean,
    val isAutoModeEnabled: Boolean,
    val isMataOptionEnabled: Boolean,
    val appMode: Int,
    val lastFeedbackTimestamp: Long,
    val isProForced: Boolean,
    
    // NOWE POLA DLA TRYBU LETNIEGO
    val heatThreshold: Double, // Próg upału (np. 30°C)
    val isStormAlertEnabled: Boolean, // Czy ostrzegać o burzach/gradzie
    val isWateringReminderEnabled: Boolean, // Czy przypominać o podlewaniu (Garden)
    
    // NOWE POLE DLA GEOFENCING
    val isGeofencingEnabled: Boolean, // Czy włączyć powiadomienia geoprzestrzennych
    
    // NOWE POLE DLA POWIADOMIEŃ O ZMIANIE TRENDU
    val isTrendChangeNotificationsEnabled: Boolean, // Czy włączyć powiadomienia o zmianie trendu
    val lastTrend: pl.oki.frostalert.utils.TrendCalculations.TrendDirection?, // Ostatni potwierdzony trend
    val pendingTrend: pl.oki.frostalert.utils.TrendCalculations.TrendDirection?, // Kandydat na zmianę trendu (debouncing)
    
    val theme: Int,
    val isManualLocationEnabled: Boolean,
    val manualLatitude: Double,
    val manualLongitude: Double,
    val manualLocationName: String,
    val isOnboardingCompleted: Boolean,
    val useFahrenheit: Boolean,
    val appLanguage: String = "system", // Language code (e.g., "pl", "en", "de", "fr", "system")
    val geofenceRadiusMeters: Double,
    val activeLocationId: Int,
    val isTtsEnabled: Boolean = false,
    val isCalendarSyncEnabled: Boolean = false,
    val enabledDashboardCards: Set<String> = setOf("frost", "weather", "trend"),
    val dashboardCardOrder: String = "frost,weather,trend,uv,watering,storm",
    val isSmartHomeEnabled: Boolean = false,
    val smartHomeWebhookUrl: String = "",
    val smartHomeThreshold: Int = 50,
    val smartHomeIftttKey: String = "",
    // Morning Brief feature
    val isMorningBriefEnabled: Boolean = true,
    val morningLearningDays: Int = 14,
    val morningBriefDelayMinutes: Int = 5,
    val morningWakeHistoryJson: String = "[]",
    val morningMedianWakeMinute: Int = -1,
    val morningWindowStartMinute: Int = 360,
    val morningWindowEndMinute: Int = 600,
    val morningLastNotificationEpochDay: Long = -1L,
    val morningLastUnlockEpochDay: Long = -1L,
    val morningLastScheduledAtMs: Long = 0L,
    // In-App Review
    val frostAlertSentCount: Int = 0,
    val reviewShownTimestamp: Long = 0L
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val TEMP_THRESHOLD = doublePreferencesKey("temp_threshold")
        val HUMIDITY_THRESHOLD = intPreferencesKey("humidity_threshold")
        val PRECIPITATION_THRESHOLD = doublePreferencesKey("precipitation_threshold")
        val SENSITIVITY = doublePreferencesKey("sensitivity")
        val ALERT_START_HOUR = intPreferencesKey("alert_start_hour")
        val ALERT_END_HOUR = intPreferencesKey("alert_end_hour")
        val IGNORE_UNTIL = longPreferencesKey("ignore_until")
        val CAR_MODE_HOUR = intPreferencesKey("car_mode_hour")
        val IS_CAR_MODE_ENABLED = booleanPreferencesKey("is_car_mode_enabled")
        val IS_AUTO_MODE_ENABLED = booleanPreferencesKey("is_auto_mode_enabled")
        val IS_MATA_OPTION_ENABLED = booleanPreferencesKey("is_mata_option_enabled")
        val APP_MODE = intPreferencesKey("app_mode")
        val LAST_FEEDBACK_TIMESTAMP = longPreferencesKey("last_feedback_timestamp")
        val IS_PRO_FORCED = booleanPreferencesKey("is_pro_forced")
        
        // KLUCZE LETNIE
        val HEAT_THRESHOLD = doublePreferencesKey("heat_threshold")
        val IS_STORM_ALERT_ENABLED = booleanPreferencesKey("is_storm_alert_enabled")
        val IS_WATERING_REMINDER_ENABLED = booleanPreferencesKey("is_watering_reminder_enabled")
        
        // KLUCZ DO GEOFENCING
        val IS_GEOFENCING_ENABLED = booleanPreferencesKey("is_geofencing_enabled")
        val GEOFENCE_RADIUS = doublePreferencesKey("geofence_radius")
        
        // KLUCZ DO POWIADOMIEŃ O ZMIANIE TRENDU
        val IS_TREND_CHANGE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("is_trend_change_notifications_enabled")
        val LAST_TREND = stringPreferencesKey("last_trend") // Zakładam, że TrendDirection można zapisać jako String
        val PENDING_TREND = stringPreferencesKey("pending_trend") // Kandydat na zmianę trendu (debouncing)
        
        val THEME = intPreferencesKey("theme")
        val IS_MANUAL_LOCATION_ENABLED = booleanPreferencesKey("is_manual_location_enabled")
        val MANUAL_LATITUDE = doublePreferencesKey("manual_latitude")
        val MANUAL_LONGITUDE = doublePreferencesKey("manual_longitude")
        val MANUAL_LOCATION_NAME = stringPreferencesKey("manual_location_name")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ACTIVE_LOCATION_ID = intPreferencesKey("active_location_id")
        val IS_TTS_ENABLED = booleanPreferencesKey("is_tts_enabled")
        val IS_CALENDAR_SYNC_ENABLED = booleanPreferencesKey("is_calendar_sync_enabled")
        val ENABLED_DASHBOARD_CARDS = stringSetPreferencesKey("enabled_dashboard_cards")
        val DASHBOARD_CARD_ORDER = stringPreferencesKey("dashboard_card_order")
        val IS_SMART_HOME_ENABLED = booleanPreferencesKey("is_smart_home_enabled")
        val SMART_HOME_WEBHOOK_URL = stringPreferencesKey("smart_home_webhook_url")
        val SMART_HOME_THRESHOLD = intPreferencesKey("smart_home_threshold")
        val SMART_HOME_IFTTT_KEY = stringPreferencesKey("smart_home_ifttt_key")
        // Morning Brief feature
        val IS_MORNING_BRIEF_ENABLED = booleanPreferencesKey("is_morning_brief_enabled")
        val MORNING_LEARNING_DAYS = intPreferencesKey("morning_learning_days")
        val MORNING_BRIEF_DELAY_MINUTES = intPreferencesKey("morning_brief_delay_minutes")
        val MORNING_WAKE_HISTORY_JSON = stringPreferencesKey("morning_wake_history_json")
        val MORNING_MEDIAN_WAKE_MINUTE = intPreferencesKey("morning_median_wake_minute")
        val MORNING_WINDOW_START_MINUTE = intPreferencesKey("morning_window_start_minute")
        val MORNING_WINDOW_END_MINUTE = intPreferencesKey("morning_window_end_minute")
        val MORNING_LAST_NOTIFICATION_EPOCH_DAY = longPreferencesKey("morning_last_notification_epoch_day")
        val MORNING_LAST_UNLOCK_EPOCH_DAY = longPreferencesKey("morning_last_unlock_epoch_day")
        val MORNING_LAST_SCHEDULED_AT_MS = longPreferencesKey("morning_last_scheduled_at_ms")
        // In-App Review tracking
        val FROST_ALERT_SENT_COUNT = intPreferencesKey("frost_alert_sent_count")
        val REVIEW_SHOWN_TIMESTAMP = longPreferencesKey("review_shown_timestamp")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                tempThreshold = preferences[Keys.TEMP_THRESHOLD] ?: 1.0,
                humidityThreshold = preferences[Keys.HUMIDITY_THRESHOLD] ?: 75,
                precipitationThreshold = preferences[Keys.PRECIPITATION_THRESHOLD] ?: 0.2,
                sensitivity = preferences[Keys.SENSITIVITY] ?: 1.0,
                alertStartHour = preferences[Keys.ALERT_START_HOUR] ?: 19,
                alertEndHour = preferences[Keys.ALERT_END_HOUR] ?: 8,
                ignoreUntil = preferences[Keys.IGNORE_UNTIL] ?: 0L,
                carModeHour = preferences[Keys.CAR_MODE_HOUR] ?: 7,
                isCarModeEnabled = preferences[Keys.IS_CAR_MODE_ENABLED] ?: true,
                isAutoModeEnabled = preferences[Keys.IS_AUTO_MODE_ENABLED] ?: true,
                isMataOptionEnabled = preferences[Keys.IS_MATA_OPTION_ENABLED] ?: false,
                appMode = preferences[Keys.APP_MODE] ?: 0,
                lastFeedbackTimestamp = preferences[Keys.LAST_FEEDBACK_TIMESTAMP] ?: 0L,
                isProForced = preferences[Keys.IS_PRO_FORCED] ?: false,
                
                // DOMYŚLNE WARTOŚCI LETNIE
                heatThreshold = preferences[Keys.HEAT_THRESHOLD] ?: 30.0,
                isStormAlertEnabled = preferences[Keys.IS_STORM_ALERT_ENABLED] ?: true,
                isWateringReminderEnabled = preferences[Keys.IS_WATERING_REMINDER_ENABLED] ?: true,
                
                // DOMYŚLNE WARTOŚCI DLA GEOFENCING
                isGeofencingEnabled = preferences[Keys.IS_GEOFENCING_ENABLED] ?: false,
                
                // DOMYŚLNE WARTOŚCI DLA POWIADOMIEŃ O ZMIANIE TRENDU
                isTrendChangeNotificationsEnabled = preferences[Keys.IS_TREND_CHANGE_NOTIFICATIONS_ENABLED] ?: true,
                lastTrend = preferences[Keys.LAST_TREND]?.let { name ->
                    try { pl.oki.frostalert.utils.TrendCalculations.TrendDirection.valueOf(name) }
                    catch (e: IllegalArgumentException) {
                        Log.d("SettingsDataStore", "Nie udało się sparsować lastTrend: '$name'")
                        null
                    }
                },
                pendingTrend = preferences[Keys.PENDING_TREND]?.let { name ->
                    try { pl.oki.frostalert.utils.TrendCalculations.TrendDirection.valueOf(name) }
                    catch (e: IllegalArgumentException) {
                        Log.d("SettingsDataStore", "Nie udało się sparsować pendingTrend: '$name'")
                        null
                    }
                },
                
                theme = preferences[Keys.THEME] ?: 2,
                isManualLocationEnabled = preferences[Keys.IS_MANUAL_LOCATION_ENABLED] ?: false,
                manualLatitude = preferences[Keys.MANUAL_LATITUDE] ?: 52.2297,
                manualLongitude = preferences[Keys.MANUAL_LONGITUDE] ?: 21.0122,
                manualLocationName = preferences[Keys.MANUAL_LOCATION_NAME] ?: "Warszawa",
                isOnboardingCompleted = preferences[Keys.IS_ONBOARDING_COMPLETED] ?: false,
                useFahrenheit = preferences[Keys.USE_FAHRENHEIT] ?: false,
                appLanguage = preferences[Keys.APP_LANGUAGE] ?: "system",
                geofenceRadiusMeters = preferences[Keys.GEOFENCE_RADIUS] ?: 20000.0,
                activeLocationId = preferences[Keys.ACTIVE_LOCATION_ID] ?: 0,
                isTtsEnabled = preferences[Keys.IS_TTS_ENABLED] ?: false,
                isCalendarSyncEnabled = preferences[Keys.IS_CALENDAR_SYNC_ENABLED] ?: false,
                enabledDashboardCards = preferences[Keys.ENABLED_DASHBOARD_CARDS] ?: setOf("frost", "weather", "trend"),
                dashboardCardOrder = preferences[Keys.DASHBOARD_CARD_ORDER] ?: "frost,weather,trend,uv,watering,storm",
                isSmartHomeEnabled = preferences[Keys.IS_SMART_HOME_ENABLED] ?: false,
                smartHomeWebhookUrl = preferences[Keys.SMART_HOME_WEBHOOK_URL] ?: "",
                smartHomeThreshold = preferences[Keys.SMART_HOME_THRESHOLD] ?: 50,
                smartHomeIftttKey = preferences[Keys.SMART_HOME_IFTTT_KEY] ?: "",
                isMorningBriefEnabled = preferences[Keys.IS_MORNING_BRIEF_ENABLED] ?: true,
                morningLearningDays = preferences[Keys.MORNING_LEARNING_DAYS] ?: 14,
                morningBriefDelayMinutes = preferences[Keys.MORNING_BRIEF_DELAY_MINUTES] ?: 5,
                morningWakeHistoryJson = preferences[Keys.MORNING_WAKE_HISTORY_JSON] ?: "[]",
                morningMedianWakeMinute = preferences[Keys.MORNING_MEDIAN_WAKE_MINUTE] ?: -1,
                morningWindowStartMinute = preferences[Keys.MORNING_WINDOW_START_MINUTE] ?: 360,
                morningWindowEndMinute = preferences[Keys.MORNING_WINDOW_END_MINUTE] ?: 600,
                morningLastNotificationEpochDay = preferences[Keys.MORNING_LAST_NOTIFICATION_EPOCH_DAY] ?: -1L,
                morningLastUnlockEpochDay = preferences[Keys.MORNING_LAST_UNLOCK_EPOCH_DAY] ?: -1L,
                morningLastScheduledAtMs = preferences[Keys.MORNING_LAST_SCHEDULED_AT_MS] ?: 0L,
                frostAlertSentCount = preferences[Keys.FROST_ALERT_SENT_COUNT] ?: 0,
                reviewShownTimestamp = preferences[Keys.REVIEW_SHOWN_TIMESTAMP] ?: 0L
            )
        }

    suspend fun updateHeatThreshold(value: Double) {
        context.dataStore.edit { it[Keys.HEAT_THRESHOLD] = value.coerceIn(20.0, 50.0) }
    }

    suspend fun updateStormAlertEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_STORM_ALERT_ENABLED] = isEnabled }
    }

    suspend fun updateWateringReminderEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_WATERING_REMINDER_ENABLED] = isEnabled }
    }

    suspend fun updateIsProForced(isForced: Boolean) {
        context.dataStore.edit { it[Keys.IS_PRO_FORCED] = isForced }
    }

    suspend fun updateLastFeedbackTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_FEEDBACK_TIMESTAMP] = timestamp }
    }

    suspend fun updateAutoModeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_AUTO_MODE_ENABLED] = isEnabled }
    }

    suspend fun updateMataOptionEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_MATA_OPTION_ENABLED] = isEnabled }
    }

    suspend fun updateAppMode(mode: Int) {
        context.dataStore.edit { it[Keys.APP_MODE] = mode.coerceIn(0, 1) }
    }

    suspend fun updateCarModeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_CAR_MODE_ENABLED] = isEnabled }
    }

    suspend fun updateTempThreshold(value: Double) {
        context.dataStore.edit { it[Keys.TEMP_THRESHOLD] = value.coerceIn(-30.0, 15.0) }
    }

    suspend fun updateHumidityThreshold(value: Int) {
        context.dataStore.edit { it[Keys.HUMIDITY_THRESHOLD] = value.coerceIn(0, 100) }
    }

    suspend fun updatePrecipitationThreshold(value: Double) {
        context.dataStore.edit { it[Keys.PRECIPITATION_THRESHOLD] = value.coerceIn(0.0, 50.0) }
    }

    suspend fun updateSensitivity(value: Double) {
        context.dataStore.edit { it[Keys.SENSITIVITY] = value.coerceIn(0.1, 3.0) }
    }

    suspend fun updateAlertStartHour(value: Int) {
        context.dataStore.edit { it[Keys.ALERT_START_HOUR] = value.coerceIn(0, 23) }
    }

    suspend fun updateAlertEndHour(value: Int) {
        context.dataStore.edit { it[Keys.ALERT_END_HOUR] = value.coerceIn(0, 23) }
    }

    suspend fun updateIgnoreUntil(timestamp: Long) {
        context.dataStore.edit { it[Keys.IGNORE_UNTIL] = timestamp }
    }

    suspend fun updateCarModeHour(value: Int) {
        context.dataStore.edit { it[Keys.CAR_MODE_HOUR] = value.coerceIn(0, 23) }
    }

    suspend fun updateTheme(value: Int) {
        context.dataStore.edit { it[Keys.THEME] = value.coerceIn(0, 2) }
    }

    suspend fun updateManualLocation(isEnabled: Boolean, lat: Double, lon: Double, name: String) {
        context.dataStore.edit { settings ->
            settings[Keys.IS_MANUAL_LOCATION_ENABLED] = isEnabled
            settings[Keys.MANUAL_LATITUDE] = lat.coerceIn(-90.0, 90.0)
            settings[Keys.MANUAL_LONGITUDE] = lon.coerceIn(-180.0, 180.0)
            settings[Keys.MANUAL_LOCATION_NAME] = name
        }
    }

    suspend fun setOnboardingCompleted(isCompleted: Boolean) {
        context.dataStore.edit { it[Keys.IS_ONBOARDING_COMPLETED] = isCompleted }
    }

    suspend fun updateUseFahrenheit(useFahrenheit: Boolean) {
        context.dataStore.edit { it[Keys.USE_FAHRENHEIT] = useFahrenheit }
    }

    suspend fun updateGeofencingEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_GEOFENCING_ENABLED] = isEnabled }
    }

    suspend fun updateGeofenceRadius(radiusMeters: Double) {
        context.dataStore.edit { it[Keys.GEOFENCE_RADIUS] = radiusMeters.coerceIn(1000.0, 100_000.0) }
    }

    suspend fun updateActiveLocationId(id: Int) {
        context.dataStore.edit { it[Keys.ACTIVE_LOCATION_ID] = id }
    }

    suspend fun updateTrendChangeNotificationsEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_TREND_CHANGE_NOTIFICATIONS_ENABLED] = isEnabled }
    }

    suspend fun updateLastTrend(trend: pl.oki.frostalert.utils.TrendCalculations.TrendDirection?) {
        context.dataStore.edit { 
            if (trend != null) {
                it[Keys.LAST_TREND] = trend.name
            } else {
                it.remove(Keys.LAST_TREND)
            }
        }
    }

    suspend fun updatePendingTrend(trend: pl.oki.frostalert.utils.TrendCalculations.TrendDirection?) {
        context.dataStore.edit {
            if (trend != null) {
                it[Keys.PENDING_TREND] = trend.name
            } else {
                it.remove(Keys.PENDING_TREND)
            }
        }
    }

    suspend fun updateTtsEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_TTS_ENABLED] = isEnabled }
    }

    suspend fun updateCalendarSyncEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_CALENDAR_SYNC_ENABLED] = isEnabled }
    }

    suspend fun updateEnabledDashboardCards(cards: Set<String>) {
        context.dataStore.edit { it[Keys.ENABLED_DASHBOARD_CARDS] = cards }
    }

    suspend fun updateDashboardCardOrder(order: String) {
        context.dataStore.edit { it[Keys.DASHBOARD_CARD_ORDER] = order }
    }

    suspend fun updateSmartHomeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_SMART_HOME_ENABLED] = isEnabled }
    }

    suspend fun updateSmartHomeWebhookUrl(url: String) {
        context.dataStore.edit { it[Keys.SMART_HOME_WEBHOOK_URL] = url }
    }

    suspend fun updateSmartHomeThreshold(threshold: Int) {
        context.dataStore.edit { it[Keys.SMART_HOME_THRESHOLD] = threshold.coerceIn(0, 100) }
    }

    suspend fun updateSmartHomeIftttKey(key: String) {
        context.dataStore.edit { it[Keys.SMART_HOME_IFTTT_KEY] = key }
    }

    // ── Morning Brief ─────────────────────────────────────────────────────────

    suspend fun updateMorningBriefEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_MORNING_BRIEF_ENABLED] = isEnabled }
    }

    suspend fun updateMorningLearningDays(days: Int) {
        context.dataStore.edit { it[Keys.MORNING_LEARNING_DAYS] = days.coerceIn(3, 30) }
    }

    suspend fun updateMorningBriefDelayMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.MORNING_BRIEF_DELAY_MINUTES] = minutes.coerceIn(0, 30) }
    }

    suspend fun updateMorningWakeHistoryJson(json: String) {
        context.dataStore.edit { it[Keys.MORNING_WAKE_HISTORY_JSON] = json }
    }

    suspend fun updateMorningMedianWakeMinute(minute: Int) {
        context.dataStore.edit { it[Keys.MORNING_MEDIAN_WAKE_MINUTE] = minute.coerceIn(0, 1439) }
    }

    suspend fun updateMorningWindowStartMinute(minute: Int) {
        context.dataStore.edit { it[Keys.MORNING_WINDOW_START_MINUTE] = minute.coerceIn(0, 1439) }
    }

    suspend fun updateMorningWindowEndMinute(minute: Int) {
        context.dataStore.edit { it[Keys.MORNING_WINDOW_END_MINUTE] = minute.coerceIn(0, 1439) }
    }

    suspend fun updateMorningLastNotificationEpochDay(epochDay: Long) {
        context.dataStore.edit { it[Keys.MORNING_LAST_NOTIFICATION_EPOCH_DAY] = epochDay }
    }

    suspend fun updateMorningLastUnlockEpochDay(epochDay: Long) {
        context.dataStore.edit { it[Keys.MORNING_LAST_UNLOCK_EPOCH_DAY] = epochDay }
    }

    suspend fun updateMorningLastScheduledAtMs(ms: Long) {
        context.dataStore.edit { it[Keys.MORNING_LAST_SCHEDULED_AT_MS] = ms }
    }

    suspend fun updateAppLanguage(languageCode: String) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = languageCode }
    }

    suspend fun incrementFrostAlertSentCount() {
        context.dataStore.edit { prefs ->
            prefs[Keys.FROST_ALERT_SENT_COUNT] = (prefs[Keys.FROST_ALERT_SENT_COUNT] ?: 0) + 1
        }
    }

    suspend fun updateReviewShownTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.REVIEW_SHOWN_TIMESTAMP] = timestamp }
    }
}
