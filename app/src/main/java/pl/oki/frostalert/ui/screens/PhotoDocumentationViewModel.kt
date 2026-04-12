package pl.oki.frostalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.oki.frostalert.data.local.FrostPhoto
import pl.oki.frostalert.data.local.FrostPhotoDao
import javax.inject.Inject

@HiltViewModel
class PhotoDocumentationViewModel @Inject constructor(
    private val frostPhotoDao: FrostPhotoDao
) : ViewModel() {

    val photos: StateFlow<List<FrostPhoto>> = frostPhotoDao.getAllPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deletePhoto(photo: FrostPhoto) {
        viewModelScope.launch {
            try {
                val file = java.io.File(photo.filePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) { /* ignore cleanup errors */ }
            frostPhotoDao.delete(photo)
        }
    }

    fun savePhoto(filePath: String, note: String = "") {
        viewModelScope.launch {
            frostPhotoDao.insert(
                FrostPhoto(
                    filePath = filePath,
                    timestamp = System.currentTimeMillis(),
                    note = note
                )
            )
        }
    }
}
