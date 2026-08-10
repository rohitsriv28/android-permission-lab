package com.permissionlab.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import com.permissionlab.app.model.MediaItem
import com.permissionlab.app.model.PermissionModule
import com.permissionlab.app.model.PermissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor() : PermissionRepository {

    private val _permissionModules = MutableStateFlow(
        listOf(
            PermissionModule(
                id = "camera",
                name = "Camera",
                icon = Icons.Default.CameraAlt,
                description = "Test camera permissions and take photos.",
                status = PermissionStatus.NOT_GRANTED
            ),
            PermissionModule(
                id = "gallery",
                name = "Gallery",
                icon = Icons.Default.PhotoLibrary,
                description = "Access all photos on your device using storage permissions.",
                status = PermissionStatus.NOT_GRANTED
            )
        )
    )
    override fun getPermissionModules(): Flow<List<PermissionModule>> = _permissionModules.asStateFlow()

    private val _selectedMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    override fun getSelectedMediaItems(): Flow<List<MediaItem>> = _selectedMediaItems.asStateFlow()

    override suspend fun updatePermissionStatus(moduleId: String, status: PermissionStatus) {
        _permissionModules.update { modules ->
            modules.map { 
                if (it.id == moduleId) it.copy(status = status) else it
            }
        }
    }

    override suspend fun addMediaItems(items: List<MediaItem>) {
        _selectedMediaItems.update { currentItems ->
            (currentItems + items).distinctBy { it.id }
        }
    }

    override suspend fun removeMediaItem(itemId: String) {
        _selectedMediaItems.update { items ->
            items.filterNot { it.id == itemId }
        }
    }

    override suspend fun clearMediaItems() {
        _selectedMediaItems.value = emptyList()
    }

    override suspend fun updateMediaItem(item: MediaItem) {
        _selectedMediaItems.update { items ->
            items.map { if (it.id == item.id) item else it }
        }
    }

    override suspend fun updateMediaItems(items: List<MediaItem>) {
        val updates = items.associateBy { it.id }
        _selectedMediaItems.update { currentItems ->
            currentItems.map { updates[it.id] ?: it }
        }
    }
}
