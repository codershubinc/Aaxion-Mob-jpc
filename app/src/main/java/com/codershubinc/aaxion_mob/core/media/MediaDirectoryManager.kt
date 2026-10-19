package com.codershubinc.aaxion_mob.core.media

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class DirectoryMediaType {
    NONE,
    MOVIES,
    SERIES
}

/**
 * Manages persistent directory media type designations (Movies, Series, or Default)
 * and resolves inherited media types for subdirectories.
 */
class MediaDirectoryManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aaxion_media_directories", Context.MODE_PRIVATE)

    private val _directoryTypes = MutableStateFlow<Map<String, DirectoryMediaType>>(emptyMap())
    val directoryTypes: StateFlow<Map<String, DirectoryMediaType>> = _directoryTypes.asStateFlow()

    init {
        loadDesignations()
    }

    private fun normalizePath(path: String): String {
        var p = path.trim()
        if (p.isEmpty()) return "/"
        if (!p.startsWith('/')) p = "/$p"
        if (p.length > 1 && p.endsWith('/')) p = p.dropLast(1)
        return p
    }

    private fun loadDesignations() {
        val raw = prefs.getString("designated_dirs", null) ?: return
        try {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, DirectoryMediaType>()
            json.keys().forEach { key ->
                val typeName = json.optString(key, DirectoryMediaType.NONE.name)
                map[key] = try {
                    DirectoryMediaType.valueOf(typeName)
                } catch (_: Exception) {
                    DirectoryMediaType.NONE
                }
            }
            _directoryTypes.value = map
        } catch (_: Exception) {
        }
    }

    private fun saveDesignations() {
        val map = _directoryTypes.value
        val json = JSONObject()
        map.forEach { (path, type) ->
            if (type != DirectoryMediaType.NONE) {
                json.put(path, type.name)
            }
        }
        prefs.edit().putString("designated_dirs", json.toString()).apply()
    }

    /**
     * Gets the explicit media designation for the exact path.
     */
    fun getExplicitType(path: String): DirectoryMediaType {
        val normalized = normalizePath(path)
        return _directoryTypes.value[normalized] ?: DirectoryMediaType.NONE
    }

    /**
     * Resolves the effective media type for a directory by checking the exact path
     * first, then falling back to any parent/ancestor directory designation.
     */
    fun resolveDirectoryType(path: String): DirectoryMediaType {
        val normalized = normalizePath(path)
        val map = _directoryTypes.value

        // 1. Direct match
        map[normalized]?.let { if (it != DirectoryMediaType.NONE) return it }

        // 2. Ancestor lookup (e.g. /HDD/Movies/Action -> /HDD/Movies)
        var current = normalized
        while (current.contains('/') && current.length > 1) {
            current = current.substringBeforeLast('/', "").ifEmpty { "/" }
            map[current]?.let { if (it != DirectoryMediaType.NONE) return it }
            if (current == "/") break
        }

        return DirectoryMediaType.NONE
    }

    /**
     * Marks or unmarks a directory as Movies, Series, or Default.
     */
    fun setDirectoryType(path: String, type: DirectoryMediaType) {
        val normalized = normalizePath(path)
        val current = _directoryTypes.value.toMutableMap()
        if (type == DirectoryMediaType.NONE) {
            current.remove(normalized)
        } else {
            current[normalized] = type
        }
        _directoryTypes.value = current
        saveDesignations()
    }

    fun isMovieDirectory(path: String): Boolean =
        resolveDirectoryType(path) == DirectoryMediaType.MOVIES

    fun isSeriesDirectory(path: String): Boolean =
        resolveDirectoryType(path) == DirectoryMediaType.SERIES

    companion object {
        @Volatile
        private var instance: MediaDirectoryManager? = null

        fun getInstance(context: Context): MediaDirectoryManager {
            return instance ?: synchronized(this) {
                instance ?: MediaDirectoryManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
