package com.codershubinc.aaxion_mob.core.network

import android.content.Context
import android.content.SharedPreferences
import com.codershubinc.aaxion_mob.core.data.OmdbMediaItem
import com.codershubinc.aaxion_mob.core.data.OmdbMetadataCacheDb
import com.codershubinc.aaxion_mob.core.media.DirectoryMediaType
import com.codershubinc.aaxion_mob.core.media.MediaTitleParser
import com.codershubinc.aaxion_mob.core.media.ParsedMediaInfo
import com.codershubinc.aaxion_mob.core.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject

/**
 * Client for fetching movie and TV series metadata, high-resolution posters,
 * and IMDb ratings from the OMDB API.
 */
object OmdbClient {

    private const val PREFS_NAME = "aaxion_omdb_settings"
    private const val KEY_API_KEY = "omdb_api_key"

    // Default community key for OMDB
    const val DEFAULT_OMDB_KEY = "f6251cd"

    private val attemptedPaths = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = prefs.getString(KEY_API_KEY, null)?.trim()
        if (key.isNullOrBlank() || key == "b9a5e69d") {
            return DEFAULT_OMDB_KEY
        }
        return key
    }

    fun setApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    /**
     * Queries OMDB for a single media item by filename and path, with optional force refresh.
     */
    suspend fun fetchMovieMetadata(
        context: Context,
        fileName: String,
        rawPath: String,
        isSeries: Boolean = false,
        forceRefresh: Boolean = false
    ): OmdbMediaItem? = withContext(Dispatchers.IO) {
        val info = MediaTitleParser.parse(fileName, isSeriesHint = isSeries)
        val cacheDb = OmdbMetadataCacheDb.getInstance(context)

        if (!forceRefresh) {
            val existing = cacheDb.getForMedia(info, rawPath)
            if (existing != null) {
                return@withContext if (existing.isNotFound) null else existing
            }
            if (attemptedPaths.contains(rawPath)) {
                return@withContext null
            }
        }

        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) return@withContext null

        attemptedPaths.add(rawPath)
        val item = resolveMetadata(apiKey, info, rawPath)

        if (item != null) {
            cacheDb.put(item)
            if (info.isSeries && info.seriesKey != item.queryKey) {
                cacheDb.put(item.copy(queryKey = info.seriesKey))
            }
            if (info.seasonKey != null && info.seasonKey != item.queryKey) {
                cacheDb.put(item.copy(queryKey = info.seasonKey!!))
            }
            if (item.hasPoster) {
                com.codershubinc.aaxion_mob.core.data.PosterCacheManager.getInstance(context)
                    .cachePosterAsync(item.posterUrl, this)
            }
            item
        } else {
            val notFoundItem = OmdbMediaItem(
                queryKey = info.queryKey,
                rawPath = rawPath,
                imdbId = "NOT_FOUND",
                title = info.cleanTitle,
                year = info.year ?: "",
                type = if (info.isSeries) "series" else "movie",
                posterUrl = "NOT_FOUND",
                lastUpdatedTs = System.currentTimeMillis()
            )
            cacheDb.put(notFoundItem)
            null
        }
    }

    /**
     * Queries OMDB for a given parsed media info with cache lookup and fallback.
     */
    suspend fun fetchMetadata(
        context: Context,
        info: ParsedMediaInfo,
        rawPath: String
    ): OmdbMediaItem? = withContext(Dispatchers.IO) {
        val cacheDb = OmdbMetadataCacheDb.getInstance(context)

        // 1. Check SQLite Cache hierarchically
        val existing = cacheDb.getForMedia(info, rawPath)
        if (existing != null) {
            return@withContext if (existing.isNotFound) null else existing
        }
        if (attemptedPaths.contains(rawPath)) {
            return@withContext null
        }

        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) return@withContext null

        attemptedPaths.add(rawPath)
        val item = resolveMetadata(apiKey, info, rawPath)

        if (item != null) {
            cacheDb.put(item)
            if (info.isSeries && info.seriesKey != item.queryKey) {
                cacheDb.put(item.copy(queryKey = info.seriesKey))
            }
            if (info.seasonKey != null && info.seasonKey != item.queryKey) {
                cacheDb.put(item.copy(queryKey = info.seasonKey!!))
            }
            if (item.hasPoster) {
                com.codershubinc.aaxion_mob.core.data.PosterCacheManager.getInstance(context)
                    .cachePosterAsync(item.posterUrl, this)
            }
            item
        } else {
            val notFoundItem = OmdbMediaItem(
                queryKey = info.queryKey,
                rawPath = rawPath,
                imdbId = "NOT_FOUND",
                title = info.cleanTitle,
                year = info.year ?: "",
                type = if (info.isSeries) "series" else "movie",
                posterUrl = "NOT_FOUND",
                lastUpdatedTs = System.currentTimeMillis()
            )
            cacheDb.put(notFoundItem)
            null
        }
    }

    /**
     * Resolves metadata across candidate title variations without year first,
     * falling back to year only when needed, to ensure highest match accuracy.
     */
    private fun resolveMetadata(
        apiKey: String,
        info: ParsedMediaInfo,
        rawPath: String
    ): OmdbMediaItem? {
        val candidates = info.getCandidateTitles()

        // Pass 1: Try all candidates without year (most accurate across festival premieres, re-releases, and scene years)
        var bestItemWithoutPoster: OmdbMediaItem? = null
        for (cand in candidates) {
            val item = executeQuery(apiKey, cand, null, info.isSeries, info.queryKey, rawPath)
            if (item != null) {
                if (item.hasPoster) {
                    return item
                }
                if (bestItemWithoutPoster == null) {
                    bestItemWithoutPoster = item
                }
            }
        }

        // Pass 2: If a year was parsed from the filename and it's a movie, try primary candidate with year
        if (!info.year.isNullOrBlank() && !info.isSeries) {
            val primaryCand = candidates.firstOrNull() ?: info.cleanTitle
            val item = executeQuery(apiKey, primaryCand, info.year, false, info.queryKey, rawPath)
            if (item != null && item.hasPoster) {
                return item
            }
        }

        // Pass 3: Fallback to the best match found (even if no poster yet on IMDb, e.g. "The Whisper Man 2026")
        return bestItemWithoutPoster
    }

    private fun executeQuery(
        apiKey: String,
        title: String,
        year: String?,
        isSeries: Boolean,
        queryKey: String,
        rawPath: String
    ): OmdbMediaItem? {
        val urlBuilder = "https://www.omdbapi.com/".toHttpUrlOrNull()?.newBuilder() ?: return null
        urlBuilder.addQueryParameter("apikey", apiKey)
        urlBuilder.addQueryParameter("t", title)
        if (!year.isNullOrBlank()) {
            urlBuilder.addQueryParameter("y", year)
        }
        if (isSeries) {
            urlBuilder.addQueryParameter("type", "series")
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        return try {
            val response = Fetcher.getInstance().getClient().newCall(request).execute()
            val bodyString = response.body?.string() ?: return null
            val json = JSONObject(bodyString)
            if (json.optString("Response", "False").equals("True", ignoreCase = true)) {
                OmdbMediaItem(
                    queryKey = queryKey,
                    rawPath = rawPath,
                    imdbId = json.optString("imdbID", ""),
                    title = json.optString("Title", title),
                    year = json.optString("Year", year ?: ""),
                    type = json.optString("Type", if (isSeries) "series" else "movie"),
                    posterUrl = json.optString("Poster", ""),
                    plot = json.optString("Plot", ""),
                    genre = json.optString("Genre", ""),
                    rating = json.optString("imdbRating", ""),
                    runtime = json.optString("Runtime", ""),
                    director = json.optString("Director", ""),
                    actors = json.optString("Actors", ""),
                    lastUpdatedTs = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Batch synchronizes metadata for files and folders inside designated Movies or Series directories.
     */
    suspend fun syncDirectory(
        context: Context,
        files: List<FileItem>,
        dirType: DirectoryMediaType
    ) = withContext(Dispatchers.IO) {
        if (dirType == DirectoryMediaType.NONE) return@withContext

        val itemsToProcess = when (dirType) {
            DirectoryMediaType.MOVIES -> files.filter { it.isVideo }
            DirectoryMediaType.SERIES -> files.filter { file ->
                if (file.isVideo) true
                else if (file.isDir) {
                    val lower = file.name.lowercase().trim()
                    lower !in setOf("sample", "samples", "sub", "subs", "subtitle", "subtitles", "extra", "extras", "featurette", "featurettes")
                } else false
            }
            DirectoryMediaType.NONE -> emptyList()
        }

        val cacheDb = OmdbMetadataCacheDb.getInstance(context)

        itemsToProcess.forEach { file ->
            val info = MediaTitleParser.parse(file.name, isSeriesHint = dirType == DirectoryMediaType.SERIES)
            if (cacheDb.getForMedia(info, file.rawPath) == null) {
                fetchMetadata(context, info, file.rawPath)
            }
        }
    }
}
