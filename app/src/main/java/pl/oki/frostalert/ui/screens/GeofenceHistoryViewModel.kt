package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.repository.GeofenceRepository
import javax.inject.Inject

@HiltViewModel
class GeofenceHistoryViewModel @Inject constructor(
    private val repo: GeofenceRepository
) : ViewModel() {
    val recentRecords: StateFlow<List<pl.oki.frostalert.data.local.GeofenceRecord>> = repo.getRecentRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteOlderThan(days: Int) {
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
            repo.deleteOlderThan(cutoff)
        }
    }
}

