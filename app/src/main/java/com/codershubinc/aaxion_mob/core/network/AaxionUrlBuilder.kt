package com.codershubinc.aaxion_mob.core.network

import com.codershubinc.aaxion_mob.core.model.FileItem
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Centralized URL builder for file streaming, downloads, thumbnails, and preview endpoints.
 * Handles trailing slash normalization and standard UTF-8 URL encoding.
 */
object AaxionUrlBuilder {
    fun cleanBaseUrl(baseUrl: String): String = baseUrl.trimEnd('/')

    fun encodePath(rawPath: String): String =
        try {
            URLEncoder.encode(rawPath, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            rawPath
        }

    fun buildStreamUrl(baseUrl: String, rawPath: String, token: String): String {
        val base = ApiEndpoints.url(baseUrl, ApiEndpoints.FILES.STREAM)
        val encoded = encodePath(rawPath)
        return "$base?path=$encoded&tkn=$token"
    }

    fun buildDownloadUrl(baseUrl: String, rawPath: String, token: String): String {
        val base = ApiEndpoints.url(baseUrl, ApiEndpoints.FILES.DOWNLOAD)
        val encoded = encodePath(rawPath)
        return "$base?path=$encoded&tkn=$token"
    }

    fun buildThumbUrl(baseUrl: String, rawPath: String, token: String = ""): String {
        val base = ApiEndpoints.url(baseUrl, ApiEndpoints.IMAGES.THUMB)
        val encoded = encodePath(rawPath)
        return if (token.isNotEmpty()) {
            "$base?path=$encoded&tkn=$token"
        } else {
            "$base?path=$encoded"
        }
    }

    fun buildImageViewUrl(baseUrl: String, rawPath: String, token: String = ""): String {
        val base = ApiEndpoints.url(baseUrl, ApiEndpoints.IMAGES.VIEW)
        val encoded = encodePath(rawPath)
        return if (token.isNotEmpty()) {
            "$base?path=$encoded&tkn=$token"
        } else {
            "$base?path=$encoded"
        }
    }

    fun buildMusicStreamUrl(baseUrl: String, trackId: Long, token: String): String {
        val base = ApiEndpoints.url(baseUrl, ApiEndpoints.MUSIC.STREAM)
        return "$base?id=$trackId&tkn=$token"
    }

    fun buildMusicArtworkUrl(baseUrl: String, imagePath: String, token: String = ""): String {
        if (imagePath.isBlank()) return ""
        val base = ApiEndpoints.url(baseUrl, ApiEndpoints.IMAGES.VIEW)
        val encoded = encodePath(imagePath)
        return if (token.isNotEmpty()) {
            "$base?path=$encoded&tkn=$token"
        } else {
            "$base?path=$encoded"
        }
    }
}

fun FileItem.buildStreamUrl(baseUrl: String, token: String): String =
    AaxionUrlBuilder.buildStreamUrl(baseUrl, rawPath, token)

fun FileItem.buildDownloadUrl(baseUrl: String, token: String): String =
    AaxionUrlBuilder.buildDownloadUrl(baseUrl, rawPath, token)

fun FileItem.buildThumbUrl(baseUrl: String, token: String = ""): String =
    AaxionUrlBuilder.buildThumbUrl(baseUrl, rawPath, token)

fun FileItem.buildImageViewUrl(baseUrl: String, token: String = ""): String =
    AaxionUrlBuilder.buildImageViewUrl(baseUrl, rawPath, token)

fun com.codershubinc.aaxion_mob.core.model.MusicTrack.buildStreamUrl(baseUrl: String, token: String): String =
    AaxionUrlBuilder.buildMusicStreamUrl(baseUrl, id, token)

fun com.codershubinc.aaxion_mob.core.model.MusicTrack.buildArtworkUrl(baseUrl: String, token: String = ""): String =
    AaxionUrlBuilder.buildMusicArtworkUrl(baseUrl, imagePath, token)

