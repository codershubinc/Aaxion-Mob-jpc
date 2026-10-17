package com.codershubinc.aaxion_mob.core.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a music track stored in the Aaxion database.
 */
data class MusicTrack(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String,
    @SerializedName("duration") val duration: Double,
    @SerializedName("releaseYear") val releaseYear: Int,
    @SerializedName("filePath") val filePath: String,
    @SerializedName("ytUri") val ytUri: String,
    @SerializedName("imagePath") val imagePath: String,
    @SerializedName("size") val size: Long,
    @SerializedName("createdAt") val createdAt: String
) {
    /**
     * Converts a server-indexed MusicTrack into a FileItem
     * for seamless compatibility with MediaPlayerManager and queue controls.
     */
    fun toFileItem(): FileItem {
        val ext = filePath.substringAfterLast('.', "mp3")
        val cleanName = if (title.isNotBlank()) {
            if (artist.isNotBlank()) "$artist - $title.$ext" else "$title.$ext"
        } else {
            filePath.substringAfterLast('/').ifEmpty { "track_$id.$ext" }
        }
        return FileItem(
            name = cleanName,
            isDir = false,
            size = size,
            path = filePath,
            rawPath = filePath
        )
    }

    val displayTitle: String
        get() = title.ifBlank { filePath.substringAfterLast('/').substringBeforeLast('.') }

    val displayArtist: String
        get() = artist.ifBlank { "Unknown Artist" }
}

/**
 * Response payload returned when downloading/adding tracks from a URI.
 */
data class AddTrackResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("total") val total: Int,
    @SerializedName("downloaded") val downloaded: Int,
    @SerializedName("failed") val failed: Int,
    @SerializedName("tracks") val tracks: List<MusicTrack> = emptyList(),
    @SerializedName("errors") val errors: List<String>? = null
)

/**
 * Request payload for updating track metadata.
 */
data class UpdateTrackRequest(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String,
    @SerializedName("releaseYear") val releaseYear: Int
)
