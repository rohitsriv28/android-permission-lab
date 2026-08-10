package com.permissionlab.app.ui.screens.camera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.permissionlab.app.data.PermissionRepository
import com.permissionlab.app.data.remote.NetworkService
import com.permissionlab.app.model.MediaItem
import com.permissionlab.app.model.PermissionStatus
import com.permissionlab.app.model.UploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: PermissionRepository,
    application: Application
) : AndroidViewModel(application) {

    val cameraPermissionStatus: StateFlow<PermissionStatus> = repository.getPermissionModules()
        .map { modules ->
            modules.find { it.id == "camera" }?.status ?: PermissionStatus.NOT_GRANTED
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PermissionStatus.NOT_GRANTED
        )

    fun onPermissionResult(isGranted: Boolean) {
        viewModelScope.launch {
            repository.updatePermissionStatus(
                moduleId = "camera",
                status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.NOT_GRANTED
            )
        }
    }

    fun onPhotoCaptured(uri: String, fileName: String, size: Long, width: Int, height: Int) {
        viewModelScope.launch {
            val mediaItem = MediaItem(
                id = UUID.randomUUID().toString(),
                uri = uri,
                fileName = fileName,
                mimeType = "image/jpeg",
                size = size,
                width = width,
                height = height,
                dateAdded = System.currentTimeMillis(),
                uploadStatus = UploadStatus.UPLOADING
            )
            repository.addMediaItems(listOf(mediaItem))

            // Real backend upload for camera captures
            val result = NetworkService.uploadPhoto(getApplication(), mediaItem)
            result.fold(
                onSuccess = { uploadedItem ->
                    repository.updateMediaItem(uploadedItem)
                },
                onFailure = {
                    repository.updateMediaItem(mediaItem.copy(uploadStatus = UploadStatus.FAILED))
                }
            )
        }
    }
}
