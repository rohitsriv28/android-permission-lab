package com.permissionlab.app.data

import com.permissionlab.app.model.MediaItem
import com.permissionlab.app.model.PermissionModule
import com.permissionlab.app.model.PermissionStatus
import kotlinx.coroutines.flow.Flow

interface PermissionRepository {
    fun getPermissionModules(): Flow<List<PermissionModule>>
    suspend fun updatePermissionStatus(moduleId: String, status: PermissionStatus)
    
    fun getSelectedMediaItems(): Flow<List<MediaItem>>
    suspend fun addMediaItems(items: List<MediaItem>)
    suspend fun removeMediaItem(itemId: String)
    suspend fun clearMediaItems()
    suspend fun updateMediaItem(item: MediaItem)
    suspend fun updateMediaItems(items: List<MediaItem>)
}
