package com.permissionlab.app.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionlab.app.data.PermissionRepository
import com.permissionlab.app.model.PermissionModule
import com.permissionlab.app.model.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PermissionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val permissionModules: StateFlow<List<PermissionModule>> = repository.getPermissionModules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshPermissionStatus() {
        viewModelScope.launch {
            // Check Camera Permission
            val cameraStatus = if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.NOT_GRANTED
            }
            repository.updatePermissionStatus("camera", cameraStatus)

            // Check Gallery Permission
            val galleryStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasImages = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                if (hasImages) PermissionStatus.GRANTED else PermissionStatus.NOT_GRANTED
            } else {
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    PermissionStatus.GRANTED
                } else {
                    PermissionStatus.NOT_GRANTED
                }
            }
            repository.updatePermissionStatus("gallery", galleryStatus)
        }
    }
}
