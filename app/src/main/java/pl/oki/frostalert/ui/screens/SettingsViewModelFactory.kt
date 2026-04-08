package pl.oki.frostalert.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pl.oki.frostalert.billing.BillingClientWrapper
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.repository.SettingsRepositoryImpl
import pl.oki.frostalert.data.repository.LocationRepository

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val settingsDataStore = SettingsDataStore(context)
            val settingsRepository = SettingsRepositoryImpl(settingsDataStore)
            val locationRepo = LocationRepository(context, settingsDataStore)
            val geofenceRegistrar: pl.oki.frostalert.geofence.GeofenceRegistrarContract = pl.oki.frostalert.geofence.GeofenceRegistrar(context, settingsDataStore, locationRepo)
            val billingManager = BillingClientWrapper(context)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsRepository, geofenceRegistrar, locationRepo, billingManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
