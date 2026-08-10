package com.permissionlab.app.model

import androidx.compose.ui.graphics.vector.ImageVector

data class PermissionModule(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String,
    val status: PermissionStatus
)

enum class PermissionStatus {
    GRANTED, NOT_GRANTED, PARTIAL, NOT_CONNECTED
}
