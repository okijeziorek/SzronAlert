package pl.oki.frostalert.data.repository

import kotlinx.coroutines.flow.Flow
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.local.UserPreferences

interface SettingsRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun updateAutoModeEnabled(isEnabled: Boolean)
    suspend fun updateMataOptionEnabled(isEnabled: Boolean)
    suspend fun updateAppMode(mode: Int)
    suspend fun updateLastFeedbackTimestamp(timestamp: Long)
    suspend fun updateIgnoreUntil(timestamp: Long)
    suspend fun updateIsProForced(isForced: Boolean)
    suspend fun updateHeatThreshold(threshold: Double)
    suspend fun updateStormAlertEnabled(isEnabled: Boolean)
    suspend fun updateWateringReminderEnabled(isEnabled: Boolean)
    suspend fun updateTempThreshold(threshold: Double)
    suspend fun updateHumidityThreshold(threshold: Int)
    suspend fun updatePrecipitationThreshold(threshold: Double)
    suspend fun updateSensitivity(value: Double)
    suspend fun updateAlertStartHour(hour: Int)
    suspend fun updateAlertEndHour(hour: Int)
    suspend fun updateCarModeEnabled(isEnabled: Boolean)
    suspend fun updateCarModeHour(hour: Int)
    suspend fun updateTheme(value: Int)
    suspend fun updateManualLocation(isEnabled: Boolean, lat: Double, lon: Double, name: String)
    suspend fun setOnboardingCompleted(isCompleted: Boolean)
    suspend fun updateUseFahrenheit(useFahrenheit: Boolean)
    suspend fun updateGeofencingEnabled(isEnabled: Boolean)
    suspend fun updateGeofenceRadius(radiusMeters: Double)
    suspend fun updateTrendChangeNotificationsEnabled(isEnabled: Boolean)
    suspend fun updateLastTrend(trend: pl.oki.frostalert.utils.TrendCalculations.TrendDirection?)
    suspend fun updatePendingTrend(trend: pl.oki.frostalert.utils.TrendCalculations.TrendDirection?)
    suspend fun updateTtsEnabled(isEnabled: Boolean)
    suspend fun updateCalendarSyncEnabled(isEnabled: Boolean)
    suspend fun updateEnabledDashboardCards(cards: Set<String>)
    suspend fun updateDashboardCardOrder(order: String)
    suspend fun updateSmartHomeEnabled(isEnabled: Boolean)
    suspend fun updateSmartHomeWebhookUrl(url: String)
    suspend fun updateSmartHomeThreshold(threshold: Int)
    suspend fun updateSmartHomeIftttKey(key: String)
    // Morning Brief
    suspend fun updateMorningBriefEnabled(isEnabled: Boolean)
    suspend fun updateMorningLearningDays(days: Int)
    suspend fun updateMorningBriefDelayMinutes(minutes: Int)
    suspend fun updateAppLanguage(languageCode: String)
}

class SettingsRepositoryImpl(private val settingsDataStore: SettingsDataStore) : SettingsRepository {

    override val userPreferencesFlow: Flow<UserPreferences> = settingsDataStore.userPreferencesFlow

    override suspend fun updateAutoModeEnabled(isEnabled: Boolean) {
        settingsDataStore.updateAutoModeEnabled(isEnabled)
    }

    override suspend fun updateMataOptionEnabled(isEnabled: Boolean) {
        settingsDataStore.updateMataOptionEnabled(isEnabled)
    }

    override suspend fun updateAppMode(mode: Int) {
        settingsDataStore.updateAppMode(mode)
    }

    override suspend fun updateLastFeedbackTimestamp(timestamp: Long) {
        settingsDataStore.updateLastFeedbackTimestamp(timestamp)
    }

    override suspend fun updateIgnoreUntil(timestamp: Long) {
        settingsDataStore.updateIgnoreUntil(timestamp)
    }

    override suspend fun updateIsProForced(isForced: Boolean) {
        settingsDataStore.updateIsProForced(isForced)
    }

    override suspend fun updateHeatThreshold(threshold: Double) {
        settingsDataStore.updateHeatThreshold(threshold)
    }

    override suspend fun updateStormAlertEnabled(isEnabled: Boolean) {
        settingsDataStore.updateStormAlertEnabled(isEnabled)
    }

    override suspend fun updateWateringReminderEnabled(isEnabled: Boolean) {
        settingsDataStore.updateWateringReminderEnabled(isEnabled)
    }

    override suspend fun updateTempThreshold(threshold: Double) {
        settingsDataStore.updateTempThreshold(threshold)
    }

    override suspend fun updateHumidityThreshold(threshold: Int) {
        settingsDataStore.updateHumidityThreshold(threshold)
    }

    override suspend fun updatePrecipitationThreshold(threshold: Double) {
        settingsDataStore.updatePrecipitationThreshold(threshold)
    }

    override suspend fun updateSensitivity(value: Double) {
        settingsDataStore.updateSensitivity(value)
    }

    override suspend fun updateAlertStartHour(hour: Int) {
        settingsDataStore.updateAlertStartHour(hour)
    }

    override suspend fun updateAlertEndHour(hour: Int) {
        settingsDataStore.updateAlertEndHour(hour)
    }

    override suspend fun updateCarModeEnabled(isEnabled: Boolean) {
        settingsDataStore.updateCarModeEnabled(isEnabled)
    }

    override suspend fun updateCarModeHour(hour: Int) {
        settingsDataStore.updateCarModeHour(hour)
    }

    override suspend fun updateTheme(value: Int) {
        settingsDataStore.updateTheme(value)
    }

    override suspend fun updateManualLocation(isEnabled: Boolean, lat: Double, lon: Double, name: String) {
        settingsDataStore.updateManualLocation(isEnabled, lat, lon, name)
    }

    override suspend fun setOnboardingCompleted(isCompleted: Boolean) {
        settingsDataStore.setOnboardingCompleted(isCompleted)
    }

    override suspend fun updateUseFahrenheit(useFahrenheit: Boolean) {
        settingsDataStore.updateUseFahrenheit(useFahrenheit)
    }

    override suspend fun updateGeofencingEnabled(isEnabled: Boolean) {
        settingsDataStore.updateGeofencingEnabled(isEnabled)
    }

    override suspend fun updateGeofenceRadius(radiusMeters: Double) {
        settingsDataStore.updateGeofenceRadius(radiusMeters)
    }

    override suspend fun updateTrendChangeNotificationsEnabled(isEnabled: Boolean) {
        settingsDataStore.updateTrendChangeNotificationsEnabled(isEnabled)
    }

    override suspend fun updateLastTrend(trend: pl.oki.frostalert.utils.TrendCalculations.TrendDirection?) {
        settingsDataStore.updateLastTrend(trend)
    }

    override suspend fun updatePendingTrend(trend: pl.oki.frostalert.utils.TrendCalculations.TrendDirection?) {
        settingsDataStore.updatePendingTrend(trend)
    }

    override suspend fun updateTtsEnabled(isEnabled: Boolean) {
        settingsDataStore.updateTtsEnabled(isEnabled)
    }

    override suspend fun updateCalendarSyncEnabled(isEnabled: Boolean) {
        settingsDataStore.updateCalendarSyncEnabled(isEnabled)
    }

    override suspend fun updateEnabledDashboardCards(cards: Set<String>) {
        settingsDataStore.updateEnabledDashboardCards(cards)
    }

    override suspend fun updateDashboardCardOrder(order: String) {
        settingsDataStore.updateDashboardCardOrder(order)
    }

    override suspend fun updateSmartHomeEnabled(isEnabled: Boolean) {
        settingsDataStore.updateSmartHomeEnabled(isEnabled)
    }

    override suspend fun updateSmartHomeWebhookUrl(url: String) {
        settingsDataStore.updateSmartHomeWebhookUrl(url)
    }

    override suspend fun updateSmartHomeThreshold(threshold: Int) {
        settingsDataStore.updateSmartHomeThreshold(threshold)
    }

    override suspend fun updateSmartHomeIftttKey(key: String) {
        settingsDataStore.updateSmartHomeIftttKey(key)
    }

    override suspend fun updateMorningBriefEnabled(isEnabled: Boolean) {
        settingsDataStore.updateMorningBriefEnabled(isEnabled)
    }

    override suspend fun updateMorningLearningDays(days: Int) {
        settingsDataStore.updateMorningLearningDays(days)
    }

    override suspend fun updateMorningBriefDelayMinutes(minutes: Int) {
        settingsDataStore.updateMorningBriefDelayMinutes(minutes)
    }

    override suspend fun updateAppLanguage(languageCode: String) {
        settingsDataStore.updateAppLanguage(languageCode)
    }
}
