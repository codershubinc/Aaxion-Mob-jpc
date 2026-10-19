package com.codershubinc.aaxion_mob.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages persistent on-disk caching of movie and TV series poster images.
 * Keeps posters permanently accessible for offline usage, fast local rendering,
 * and lockscreen/media session artwork without redundant network fetches.
 */
class PosterCacheManager private constructor(private val context: Context) {

    private val posterDir = File(context.filesDir, "omdb_posters").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    // In-memory set of currently downloading poster keys to prevent duplicate requests
    private val inFlightDownloads = ConcurrentHashMap.newKeySet<String>()

    /**
     * Checks if the poster for [posterUrl] has already been downloaded to disk.
     */
    fun getCachedFile(posterUrl: String?): File? {
        if (posterUrl.isNullOrBlank() || posterUrl == "N/A") return null
        val fileName = hashUrl(posterUrl) + ".jpg"
        val file = File(posterDir, fileName)
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Resolves either the local file URI if cached on disk, or the remote URL as fallback.
     */
    fun resolvePosterSource(posterUrl: String?): Any? {
        if (posterUrl.isNullOrBlank() || posterUrl == "N/A") return null
        val cached = getCachedFile(posterUrl)
        return cached ?: posterUrl
    }

    /**
     * Loads a Bitmap directly from the local disk cache if available.
     */
    fun loadCachedBitmap(posterUrl: String?, targetWidth: Int = 512, targetHeight: Int = 512): Bitmap? {
        val file = getCachedFile(posterUrl) ?: return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var inSampleSize = 1
            if (options.outHeight > targetHeight || options.outWidth > targetWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode cached poster bitmap: ${e.message}")
            null
        }
    }

    /**
     * Downloads and writes the poster to disk cache.
     */
    suspend fun cachePoster(posterUrl: String?): File? = withContext(Dispatchers.IO) {
        if (posterUrl.isNullOrBlank() || posterUrl == "N/A") return@withContext null

        val existing = getCachedFile(posterUrl)
        if (existing != null) return@withContext existing

        val key = hashUrl(posterUrl)
        if (!inFlightDownloads.add(key)) {
            // Already being downloaded by another coroutine
            return@withContext null
        }

        try {
            val request = Request.Builder()
                .url(posterUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Poster fetch HTTP ${response.code} for $posterUrl")
                    return@withContext null
                }
                val body = response.body ?: return@withContext null

                val targetFile = File(posterDir, "$key.jpg")
                val tempFile = File(posterDir, "$key.tmp")

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    if (tempFile.renameTo(targetFile)) {
                        return@withContext targetFile
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed downloading poster from $posterUrl: ${e.message}")
        } finally {
            inFlightDownloads.remove(key)
        }
        null
    }

    /**
     * Asynchronously triggers downloading and caching of the poster in the background.
     */
    fun cachePosterAsync(posterUrl: String?, scope: CoroutineScope) {
        if (posterUrl.isNullOrBlank() || posterUrl == "N/A") return
        if (getCachedFile(posterUrl) != null) return

        scope.launch(Dispatchers.IO) {
            cachePoster(posterUrl)
        }
    }

    /**
     * Clears all cached posters from disk.
     */
    fun clearCache(): Boolean {
        return try {
            posterDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun hashUrl(url: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest(url.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            url.hashCode().toString()
        }
    }

    companion object {
        private const val TAG = "PosterCacheManager"

        @Volatile
        private var instance: PosterCacheManager? = null

        fun getInstance(context: Context): PosterCacheManager {
            return instance ?: synchronized(this) {
                instance ?: PosterCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
