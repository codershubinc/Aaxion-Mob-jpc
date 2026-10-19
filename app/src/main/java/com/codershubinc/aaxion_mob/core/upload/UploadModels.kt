package com.codershubinc.aaxion_mob.core.upload

import android.net.Uri
import java.util.UUID

enum class UploadStatus {
    QUEUED,
    UPLOADING,
    SUCCESS,
    FAILED,
    CANCELLED
}

data class UploadTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uri: Uri? = null,
    val sizeBytes: Long,
    val targetDir: String,
    val serverUri: String,
    val token: String,
    val status: UploadStatus = UploadStatus.QUEUED,
    val bytesUploaded: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null,
    val startTimeMs: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (sizeBytes > 0L) {
            (bytesUploaded.toFloat() / sizeBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            if (status == UploadStatus.SUCCESS) 1f else 0f
        }

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val formattedSpeed: String
        get() = when {
            speedBytesPerSec <= 0L -> "-- KB/s"
            speedBytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", speedBytesPerSec / 1024.0)
            else -> String.format("%.2f MB/s", speedBytesPerSec / (1024.0 * 1024.0))
        }

    val formattedSize: String
        get() = formatBytes(sizeBytes)

    val formattedUploaded: String
        get() = formatBytes(bytesUploaded)

    companion object {
        fun formatBytes(bytes: Long): String = when {
            bytes < 0L -> "0 B"
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024L * 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
