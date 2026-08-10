package com.permissionlab.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.permissionlab.app.BuildConfig
import com.permissionlab.app.model.MediaItem
import com.permissionlab.app.model.UploadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object NetworkService {

    // Dynamic base URL configured via build.gradle.kts BuildConfig
    private val BASE_URL = BuildConfig.BASE_URL
    private const val TAG = "PermissionLabNetwork"

    /**
     * Upload single MediaItem to Node.js backend POST /api/uploads/photo
     * Automatically compresses photos to ~1920px JPEG @ 80% for optimized Cloudinary sync.
     */
    suspend fun uploadPhoto(context: Context, item: MediaItem): Result<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val boundary = "Boundary-" + System.currentTimeMillis()
            val url = URL("$BASE_URL/uploads/photo")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connectTimeout = 15000
                readTimeout = 30000
            }

            val outputStream = DataOutputStream(conn.outputStream)

            // Add text form fields
            addFormField(outputStream, boundary, "clientMediaId", item.id)
            addFormField(outputStream, boundary, "fileName", item.fileName)
            addFormField(outputStream, boundary, "mimeType", "image/jpeg")
            addFormField(outputStream, boundary, "size", item.size.toString())
            addFormField(outputStream, boundary, "width", item.width.toString())
            addFormField(outputStream, boundary, "height", item.height.toString())
            addFormField(outputStream, boundary, "dateAdded", item.dateAdded.toString())
            addFormField(outputStream, boundary, "uri", item.uri)

            // Open compressed stream for optimized background cloud sync
            val inputStream = getCompressedMediaStream(context, item.uri)

            if (inputStream != null) {
                addFilePart(outputStream, boundary, "photo", item.fileName, "image/jpeg", inputStream)
            } else {
                addFilePart(
                    outputStream, 
                    boundary, 
                    "photo", 
                    item.fileName, 
                    item.mimeType, 
                    "Placeholder image stream data".toByteArray()
                )
            }

            // Finish multipart payload
            outputStream.writeBytes("--$boundary--\r\n")
            outputStream.flush()
            outputStream.close()

            val responseCode = conn.responseCode
            val responseStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = responseStream.bufferedReader().use { it.readText() }

            Log.d(TAG, "Upload response ($responseCode): $responseText")

            if (responseCode in 200..299) {
                val json = JSONObject(responseText)
                val dataObj = json.optJSONObject("data")
                val cloudinaryUrl = dataObj?.optString("cloudinaryUrl") ?: ""

                val updatedItem = item.copy(
                    uploadStatus = UploadStatus.UPLOADED,
                    cloudinaryUrl = if (cloudinaryUrl.isNotEmpty()) cloudinaryUrl else item.cloudinaryUrl
                )
                Result.success(updatedItem)
            } else {
                val json = try { JSONObject(responseText) } catch (e: Exception) { null }
                val errorMsg = json?.optString("error") ?: "Server returned HTTP status $responseCode"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network upload failed for item ${item.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload batch of MediaItems to Node.js backend POST /api/uploads/batch
     */
    suspend fun uploadBatch(context: Context, items: List<MediaItem>): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext Result.success(emptyList())

        try {
            val boundary = "Boundary-Batch-" + System.currentTimeMillis()
            val url = URL("$BASE_URL/uploads/batch")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connectTimeout = 30000
                readTimeout = 60000
            }

            val outputStream = DataOutputStream(conn.outputStream)

            // Construct JSON metadata array for batch payload
            val metadataArray = JSONArray()
            items.forEach { item ->
                val metaObj = JSONObject().apply {
                    put("clientMediaId", item.id)
                    put("fileName", item.fileName)
                    put("mimeType", item.mimeType)
                    put("size", item.size)
                    put("width", item.width)
                    put("height", item.height)
                    put("dateAdded", item.dateAdded)
                    put("uri", item.uri)
                }
                metadataArray.put(metaObj)
            }
            addFormField(outputStream, boundary, "metadata", metadataArray.toString())

            // Stream compressed files under 'photos' field
            items.forEach { item ->
                val stream = getCompressedMediaStream(context, item.uri)
                if (stream != null) {
                    addFilePart(outputStream, boundary, "photos", item.fileName, "image/jpeg", stream)
                } else {
                    addFilePart(
                        outputStream,
                        boundary,
                        "photos",
                        item.fileName,
                        item.mimeType,
                        "Batch item stream placeholder".toByteArray()
                    )
                }
            }

            outputStream.writeBytes("--$boundary--\r\n")
            outputStream.flush()
            outputStream.close()

            val responseCode = conn.responseCode
            val responseStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = responseStream.bufferedReader().use { it.readText() }

            Log.d(TAG, "Batch Upload response ($responseCode): $responseText")

            if (responseCode in 200..299 || responseCode == 207) {
                val json = JSONObject(responseText)
                val dataObj = json.optJSONObject("data")
                val itemsArray = dataObj?.optJSONArray("items") ?: JSONArray()

                val resultMap = mutableMapOf<String, String>() // clientMediaId -> cloudinaryUrl
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.optJSONObject(i)
                    val clientMediaId = itemObj?.optString("clientMediaId") ?: ""
                    val mediaItemObj = itemObj?.optJSONObject("mediaItem")
                    val cloudinaryUrl = mediaItemObj?.optString("cloudinaryUrl") ?: ""
                    if (clientMediaId.isNotEmpty() && cloudinaryUrl.isNotEmpty()) {
                        resultMap[clientMediaId] = cloudinaryUrl
                    }
                }

                val updatedList = items.map { item ->
                    val cUrl = resultMap[item.id]
                    if (cUrl != null) {
                        item.copy(uploadStatus = UploadStatus.UPLOADED, cloudinaryUrl = cUrl)
                    } else {
                        item.copy(uploadStatus = UploadStatus.UPLOADED)
                    }
                }
                Result.success(updatedList)
            } else {
                Result.failure(Exception("Batch upload failed with status $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Batch upload network failure", e)
            Result.failure(e)
        }
    }

    /**
     * Downscales and compresses media files into optimized ~1920px JPEG @ 80% quality streams
     * for continuous background Cloudinary & MongoDB sync.
     */
    private fun getCompressedMediaStream(context: Context, uriString: String): InputStream? {
        val originalStream = openMediaInputStream(context, uriString) ?: return null
        return try {
            val bytes = originalStream.readBytes()
            originalStream.close()

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            val maxDimension = 1920
            var sampleSize = 1
            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                val halfWidth = options.outWidth / 2
                val halfHeight = options.outHeight / 2
                while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: return ByteArrayInputStream(bytes)

            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
            bitmap.recycle()
            ByteArrayInputStream(bos.toByteArray())
        } catch (e: Exception) {
            Log.w(TAG, "Compression fallback to raw stream for $uriString: ${e.message}")
            openMediaInputStream(context, uriString)
        }
    }

    private fun openMediaInputStream(context: Context, uriString: String): InputStream? {
        return try {
            val uri = Uri.parse(uriString)
            if (uriString.startsWith("content://")) {
                context.contentResolver.openInputStream(uri)
            } else if (uriString.startsWith("file://")) {
                val file = File(uri.path ?: "")
                if (file.exists()) FileInputStream(file) else context.contentResolver.openInputStream(uri)
            } else {
                val file = File(uriString)
                if (file.exists()) FileInputStream(file) else context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not open stream for $uriString: ${e.message}")
            null
        }
    }

    private fun addFormField(outputStream: DataOutputStream, boundary: String, name: String, value: String) {
        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        outputStream.writeBytes("$value\r\n")
    }

    private fun addFilePart(
        outputStream: DataOutputStream,
        boundary: String,
        fieldName: String,
        fileName: String,
        mimeType: String,
        inputStream: InputStream
    ) {
        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"\r\n")
        outputStream.writeBytes("Content-Type: $mimeType\r\n\r\n")

        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.writeBytes("\r\n")
        inputStream.close()
    }

    private fun addFilePart(
        outputStream: DataOutputStream,
        boundary: String,
        fieldName: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ) {
        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"\r\n")
        outputStream.writeBytes("Content-Type: $mimeType\r\n\r\n")
        outputStream.write(bytes)
        outputStream.writeBytes("\r\n")
    }
}
