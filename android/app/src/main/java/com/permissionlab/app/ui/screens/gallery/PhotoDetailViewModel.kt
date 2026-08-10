package com.permissionlab.app.ui.screens.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.permissionlab.app.data.PermissionRepository
import com.permissionlab.app.data.remote.NetworkService
import com.permissionlab.app.model.MediaItem
import com.permissionlab.app.model.UploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    private val repository: PermissionRepository,
    savedStateHandle: SavedStateHandle,
    application: Application
) : AndroidViewModel(application) {

    private val photoUri: String = checkNotNull(savedStateHandle["uri"])

    val mediaItem: StateFlow<MediaItem?> = repository.getSelectedMediaItems()
        .map { items -> items.find { it.uri == photoUri } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun uploadPhoto() {
        val currentItem = mediaItem.value ?: return
        if (currentItem.uploadStatus == UploadStatus.UPLOADED || 
            currentItem.uploadStatus == UploadStatus.UPLOADING) return

        viewModelScope.launch {
            // Update UI state to UPLOADING
            repository.updateMediaItem(currentItem.copy(uploadStatus = UploadStatus.UPLOADING))
            
            // Execute real backend REST API call
            val result = NetworkService.uploadPhoto(getApplication(), currentItem)
            
            result.fold(
                onSuccess = { uploadedItem ->
                    repository.updateMediaItem(uploadedItem)
                },
                onFailure = { error ->
                    repository.updateMediaItem(currentItem.copy(uploadStatus = UploadStatus.FAILED))
                }
            )
        }
    }
}
