package com.codershubinc.aaxion_mob.core.model

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

    val nameWithoutExtension: String
        get() = if (name.contains('.')) name.substringBeforeLast('.') else name

    val isVideo: Boolean
        get() = !isDir && isVideoFile(name)

    val isAudio: Boolean
        get() = !isDir && isAudioFile(name)

    val isImage: Boolean
        get() = !isDir && isImageFile(name)

    val isDoc: Boolean
        get() = !isDir && isDocFile(name)

    /**
     * Formats filename with middle ellipsis, preserving prefix and file extension.
     */
    fun displayName(maxChars: Int = 30): String {
        if (name.length <= maxChars) return name
        val ext = if (!isDir && name.contains('.') && name.lastIndexOf('.') > 0) {
            name.substring(name.lastIndexOf('.'))
        } else ""

        val suffix = if (ext.isNotEmpty()) {
            val extra = minOf(3, (name.length - ext.length) / 3)
            name.takeLast(ext.length + extra)
        } else {
            name.takeLast(4)
        }
        val prefixLen = (maxChars - suffix.length - 1).coerceAtLeast(3)
        return "${name.take(prefixLen).trimEnd()}…$suffix"
    }

    companion object {
        val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "m4v", "m2ts", "mts", "3gp")
        val AUDIO_EXTS = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma")
        val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg")
        val DOC_EXTS = setOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "zip", "tar", "gz")
        val DOC_FILENAMES = setOf(
            "license", "licence", "readme", "changelog", "authors", "contributors",
            "copying", "notice", "patents", "todo", "version"
        )
        val CODE_FILENAMES = setOf(
            "makefile", "gnumakefile", "dockerfile", "containerfile", "gemfile",
            "procfile", "vagrantfile", "rakefile", "brewfile", "cmakelists.txt"
        )

        fun isVideoFile(name: String): Boolean =
            name.substringAfterLast('.', "").lowercase() in VIDEO_EXTS

        fun isAudioFile(name: String): Boolean =
            name.substringAfterLast('.', "").lowercase() in AUDIO_EXTS

        fun isImageFile(name: String): Boolean =
            name.substringAfterLast('.', "").lowercase() in IMAGE_EXTS

        fun isDocFile(name: String): Boolean {
            val lower = name.lowercase()
            val ext = lower.substringAfterLast('.', "")
            return ext in DOC_EXTS || lower in DOC_FILENAMES
        }
    }
}
