package com.permissionlab.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadApiResponse(
    val success: Boolean,
    val message: String,
    val data: MediaItemDto? = null,
    val error: String? = null
)

@Serializable
data class BatchUploadApiResponse(
    val success: Boolean,
    val message: String,
    val data: BatchDataDto? = null,
    val error: String? = null
)

@Serializable
data class BatchDataDto(
    val total: Int,
    val successful: Int,
    val failed: Int,
    val items: List<BatchItemResultDto> = emptyList()
)

@Serializable
data class BatchItemResultDto(
    val id: String,
    val clientMediaId: String,
    val status: String,
    val isDuplicate: Boolean = false,
    val mediaItem: MediaItemDto? = null,
    val error: String? = null
)

@Serializable
data class MediaItemDto(
    val id: String,
    val clientMediaId: String = "",
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val dateAdded: Long = 0,
    val uploadStatus: String = "UPLOADED",
    val cloudinaryPublicId: String = "",
    val cloudinaryUrl: String = ""
)
