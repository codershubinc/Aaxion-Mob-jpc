package com.codershubinc.aaxion_mob.models

import com.google.gson.annotations.SerializedName

data class FileItem(
    @SerializedName("name") val name: String,
    @SerializedName("is_dir") val isDir: Boolean,
    @SerializedName("size") val size: Long,
    @SerializedName("path") val path: String,
    @SerializedName("raw_path") val rawPath: String
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val isVideo: Boolean
        get() = !isDir && extension in VIDEO_EXTS

    val isAudio: Boolean
        get() = !isDir && extension in AUDIO_EXTS

    val isImage: Boolean
        get() = !isDir && extension in IMAGE_EXTS

    val isDoc: Boolean
        get() = !isDir && extension in DOC_EXTS

    companion object {
        private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "m4v", "ts", "3gp")
        private val AUDIO_EXTS = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma")
        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg")
        private val DOC_EXTS = setOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "zip", "tar", "gz")
    }
}

