package com.codershubinc.aaxion_mob.core.media

import android.content.Context
import android.content.SharedPreferences
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.ui.designsystem.formatTimeMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Metadata representation of a recently played audio or video item.
 */
data class RecentMediaItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val rawPath: String,
    val isVideo: Boolean,
    val isAudio: Boolean,
    val durationMs: Long = 0L,
    val progressMs: Long = 0L,
    val artworkUrl: String? = null,
    val size: Long = 0L,
    val lastPlayedTs: Long = System.currentTimeMillis()
) {
    val formattedProgress: String
        get() = if (progressMs > 0) formatTimeMs(progressMs) else ""

    val formattedDuration: String
        get() = if (durationMs > 0) formatTimeMs(durationMs) else ""

    fun toFileItem(): FileItem {
        return FileItem(
            name = title,
            isDir = false,
            size = size,
            path = rawPath,
            rawPath = rawPath
        )
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("subtitle", subtitle)
            put("rawPath", rawPath)
            put("isVideo", isVideo)
            put("isAudio", isAudio)
            put("durationMs", durationMs)
            put("progressMs", progressMs)
            if (artworkUrl != null) put("artworkUrl", artworkUrl)
            put("size", size)
            put("lastPlayedTs", lastPlayedTs)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): RecentMediaItem? {
            val rawPath = json.optString("rawPath")
            if (rawPath.isBlank()) return null
            return RecentMediaItem(
                id = json.optString("id", rawPath),
                title = json.optString("title", rawPath.substringAfterLast('/')),
                subtitle = json.optString("subtitle", ""),
                rawPath = rawPath,
                isVideo = json.optBoolean("isVideo", false),
                isAudio = json.optBoolean("isAudio", true),
                durationMs = json.optLong("durationMs", 0L),
                progressMs = json.optLong("progressMs", 0L),
                artworkUrl = if (json.has("artworkUrl")) json.optString("artworkUrl") else null,
                size = json.optLong("size", 0L),
                lastPlayedTs = json.optLong("lastPlayedTs", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Manages persistent storage and reactive updates for recently played media items.
 */
class RecentPlaybackManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aaxion_recent_media_history", Context.MODE_PRIVATE)

    private val _recentItems = MutableStateFlow<List<RecentMediaItem>>(emptyList())
    val recentItems: StateFlow<List<RecentMediaItem>> = _recentItems.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val raw = prefs.getString("recent_list", null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<RecentMediaItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                RecentMediaItem.fromJson(obj)?.let { list.add(it) }
            }
            _recentItems.value = list
        } catch (e: Exception) {
            // ignore malformed data
        }
    }

    private fun persistItems(items: List<RecentMediaItem>) {
        val safeItems = items.take(30)
        val array = JSONArray()
        safeItems.forEach { array.put(it.toJson()) }
        prefs.edit().putString("recent_list", array.toString()).apply()
        _recentItems.value = safeItems
    }

    fun recordPlayback(item: RecentMediaItem) {
        val current = _recentItems.value.toMutableList()
        current.removeAll { it.rawPath == item.rawPath }
        current.add(0, item.copy(lastPlayedTs = System.currentTimeMillis()))
        persistItems(current)
    }

    fun updateProgress(rawPath: String, positionMs: Long, durationMs: Long = 0L) {
        val current = _recentItems.value.toMutableList()
        val index = current.indexOfFirst { it.rawPath == rawPath }
        if (index != -1) {
            val old = current[index]
            val updated = old.copy(
                progressMs = positionMs,
                durationMs = if (durationMs > 0) durationMs else old.durationMs,
                lastPlayedTs = System.currentTimeMillis()
            )
            current[index] = updated
            persistItems(current)
        }
    }

    fun removeItem(rawPath: String) {
        val current = _recentItems.value.toMutableList()
        current.removeAll { it.rawPath == rawPath }
        persistItems(current)
    }

    fun clearAll() {
        prefs.edit().remove("recent_list").apply()
        _recentItems.value = emptyList()
    }

    companion object {
        @Volatile
        private var instance: RecentPlaybackManager? = null

        fun getInstance(context: Context): RecentPlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: RecentPlaybackManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
