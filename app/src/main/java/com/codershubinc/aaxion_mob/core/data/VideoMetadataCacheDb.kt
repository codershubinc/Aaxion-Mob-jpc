package com.codershubinc.aaxion_mob.core.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.codershubinc.aaxion_mob.ui.designsystem.formatTimeMs
import java.util.Locale

/**
 * Model representing video playback metadata and streaming statistics.
 */
data class VideoPlaybackMeta(
    val rawPath: String,
    val title: String,
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val bitrate: Long = 0L,
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val lastWatchedTs: Long = System.currentTimeMillis(),
    val source: String = "VLC"
) {
    val resolutionLabel: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K UHD"
            width >= 2560 || height >= 1440 -> "1440p QHD"
            width >= 1920 || height >= 1080 -> "1080p FHD"
            width >= 1280 || height >= 720 -> "720p HD"
            height >= 480 -> "480p SD"
            height > 0 -> "${height}p"
            else -> ""
        }

    val dimensionsText: String
        get() = if (width > 0 && height > 0) "${width} × $height" else ""

    val aspectRatioText: String
        get() = if (width > 0 && height > 0) {
            val gcdVal = gcd(width, height)
            val wRatio = width / gcdVal
            val hRatio = height / gcdVal
            if (wRatio in 1..32 && hRatio in 1..32) "$wRatio:$hRatio" else String.format(Locale.US, "%.2f:1", width.toFloat() / height.toFloat())
        } else ""

    val formattedBitrate: String
        get() = when {
            bitrate >= 1_000_000L -> String.format(Locale.US, "%.1f Mbps", bitrate / 1_000_000f)
            bitrate >= 1_000L -> "${bitrate / 1000} kbps"
            else -> ""
        }

    val formattedPosition: String
        get() = formatTimeMs(positionMs)

    val formattedDuration: String
        get() = if (durationMs > 0L) formatTimeMs(durationMs) else ""

    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val percentWatched: Int
        get() = if (durationMs > 0L) ((positionMs * 100) / durationMs).toInt().coerceIn(0, 100) else 0

    companion object {
        private fun gcd(a: Int, b: Int): Int {
            var x = a
            var y = b
            while (y != 0) {
                val temp = y
                y = x % y
                x = temp
            }
            return if (x == 0) 1 else kotlin.math.abs(x)
        }
    }
}

/**
 * SQLite helper for caching video metadata, resolution parameters, and playback positions.
 */
class VideoMetadataCacheDb private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                raw_path TEXT PRIMARY KEY,
                title TEXT,
                stream_url TEXT,
                duration_ms INTEGER,
                position_ms INTEGER,
                width INTEGER,
                height INTEGER,
                bitrate INTEGER,
                mime_type TEXT,
                file_size INTEGER,
                last_watched_ts INTEGER,
                source TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun upsert(meta: VideoPlaybackMeta) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("raw_path", meta.rawPath)
            put("title", meta.title)
            put("stream_url", meta.streamUrl)
            put("duration_ms", meta.durationMs)
            put("position_ms", meta.positionMs)
            put("width", meta.width)
            put("height", meta.height)
            put("bitrate", meta.bitrate)
            put("mime_type", meta.mimeType)
            put("file_size", meta.fileSize)
            put("last_watched_ts", meta.lastWatchedTs)
            put("source", meta.source)
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun get(rawPath: String): VideoPlaybackMeta? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "raw_path = ?",
            arrayOf(rawPath),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToMeta(it)
            } else {
                null
            }
        }
    }

    fun getAll(): List<VideoPlaybackMeta> {
        val list = mutableListOf<VideoPlaybackMeta>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "last_watched_ts DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToMeta(it))
            }
        }
        return list
    }

    fun updatePosition(rawPath: String, positionMs: Long, durationMs: Long = 0L) {
        val existing = get(rawPath)
        if (existing != null) {
            val safeDur = if (durationMs > 0L) durationMs else existing.durationMs
            upsert(existing.copy(positionMs = positionMs, durationMs = safeDur, lastWatchedTs = System.currentTimeMillis()))
        } else {
            val filename = rawPath.substringAfterLast('/')
            upsert(
                VideoPlaybackMeta(
                    rawPath = rawPath,
                    title = filename,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    lastWatchedTs = System.currentTimeMillis()
                )
            )
        }
    }

    fun delete(rawPath: String) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "raw_path = ?", arrayOf(rawPath))
    }

    fun clear() {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
    }

    private fun cursorToMeta(cursor: android.database.Cursor): VideoPlaybackMeta {
        return VideoPlaybackMeta(
            rawPath = cursor.getString(cursor.getColumnIndexOrThrow("raw_path")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            streamUrl = cursor.getString(cursor.getColumnIndexOrThrow("stream_url")) ?: "",
            durationMs = cursor.getLong(cursor.getColumnIndexOrThrow("duration_ms")),
            positionMs = cursor.getLong(cursor.getColumnIndexOrThrow("position_ms")),
            width = cursor.getInt(cursor.getColumnIndexOrThrow("width")),
            height = cursor.getInt(cursor.getColumnIndexOrThrow("height")),
            bitrate = cursor.getLong(cursor.getColumnIndexOrThrow("bitrate")),
            mimeType = cursor.getString(cursor.getColumnIndexOrThrow("mime_type")) ?: "",
            fileSize = cursor.getLong(cursor.getColumnIndexOrThrow("file_size")),
            lastWatchedTs = cursor.getLong(cursor.getColumnIndexOrThrow("last_watched_ts")),
            source = cursor.getString(cursor.getColumnIndexOrThrow("source")) ?: "VLC"
        )
    }

    companion object {
        private const val DB_NAME = "aaxion_video_metadata_cache.db"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "video_metadata"

        @Volatile
        private var instance: VideoMetadataCacheDb? = null

        fun getInstance(context: Context): VideoMetadataCacheDb {
            return instance ?: synchronized(this) {
                instance ?: VideoMetadataCacheDb(context.applicationContext).also { instance = it }
            }
        }
    }
}
