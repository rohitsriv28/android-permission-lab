package com.permissionlab.app.model

data class MediaItem(
    val id: String,
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val uploadStatus: UploadStatus = UploadStatus.READY,
    val cloudinaryUrl: String? = null
)

enum class UploadStatus {
    READY, UPLOADING, UPLOADED, FAILED
}
