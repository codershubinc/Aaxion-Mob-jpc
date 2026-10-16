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
        val clean = cleanBaseUrl(baseUrl)
        val encoded = encodePath(rawPath)
        return "$clean/api/v1/files/stream?path=$encoded&tkn=$token"
    }

    fun buildDownloadUrl(baseUrl: String, rawPath: String, token: String): String {
        val clean = cleanBaseUrl(baseUrl)
        val encoded = encodePath(rawPath)
        return "$clean/api/v1/files/download?path=$encoded&tkn=$token"
    }

    fun buildThumbUrl(baseUrl: String, rawPath: String, token: String = ""): String {
        val clean = cleanBaseUrl(baseUrl)
        val encoded = encodePath(rawPath)
        return if (token.isNotEmpty()) {
            "$clean/api/v1/images/thumb?path=$encoded&tkn=$token"
        } else {
            "$clean/api/v1/images/thumb?path=$encoded"
        }
    }

    fun buildImageViewUrl(baseUrl: String, rawPath: String, token: String = ""): String {
        val clean = cleanBaseUrl(baseUrl)
        val encoded = encodePath(rawPath)
        return if (token.isNotEmpty()) {
            "$clean/api/v1/images/view?path=$encoded&tkn=$token"
        } else {
            "$clean/api/v1/images/view?path=$encoded"
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
