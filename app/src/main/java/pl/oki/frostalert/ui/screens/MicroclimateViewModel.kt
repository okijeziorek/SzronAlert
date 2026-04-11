package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.GardenZone
import pl.oki.frostalert.data.local.GardenZoneDao
import javax.inject.Inject

@HiltViewModel
class MicroclimateViewModel @Inject constructor(
    private val gardenZoneDao: GardenZoneDao
) : ViewModel() {

    val zones: StateFlow<List<GardenZone>> = gardenZoneDao.getAllZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addZone(name: String, correction: Double, emoji: String = "🌡️") {
        viewModelScope.launch {
            gardenZoneDao.insert(
                GardenZone(
                    name = name,
                    temperatureCorrection = correction,
                    iconEmoji = emoji
                )
            )
        }
    }

    fun deleteZone(zone: GardenZone) {
        viewModelScope.launch {
            gardenZoneDao.delete(zone)
        }
    }
}
