package pl.oki.frostalert.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pl.oki.frostalert.data.local.SettingsDataStore
import pl.oki.frostalert.data.repository.SettingsRepository

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val settingsDataStore = SettingsDataStore(context)
            val settingsRepository = SettingsRepository(settingsDataStore)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
