package pl.oki.frostalert.ui.screens

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.oki.frostalert.billing.BillingManagerInterface
import pl.oki.frostalert.data.local.UserPreferences
import pl.oki.frostalert.data.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val geofenceRegistrar: pl.oki.frostalert.geofence.GeofenceRegistrarContract,
    private val locationRepository: pl.oki.frostalert.data.repository.LocationRepository,
    private val billingManager: BillingManagerInterface
) : ViewModel() {

    init {
        geofenceRegistrar.start()
    }

    val userPreferences: StateFlow<UserPreferences?> = settingsRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** `true` when the user has an active PRO purchase OR the debug override is enabled. */
    val isPro: StateFlow<Boolean> = combine(
        billingManager.isPro,
        settingsRepository.userPreferencesFlow
    ) { billingPro, prefs ->
        billingPro || (prefs?.isProForced == true)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /** Emits the last billing error message, or `null` when there is no error. */
    val purchaseError: StateFlow<String?> = billingManager.purchaseError
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Initiates the Play Billing purchase flow for PRO. */
    fun launchPurchaseFlow(activity: Activity) {
        billingManager.queryProductDetails { productDetails ->
            productDetails?.let { billingManager.launchPurchaseFlow(activity, it) }
        }
    }

    /** Clears the last purchase error (e.g. after showing a snackbar). */
    fun clearPurchaseError() {
        billingManager.clearError()
    }

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
            registerGeofenceForCurrentSettings()
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
                registerGeofenceForCurrentSettings()
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

    private fun registerGeofenceForCurrentSettings() {
        try {
            geofenceRegistrar.registerForCurrentLocation(locationRepository)
        } catch (_: Exception) {
        }
    }
}
