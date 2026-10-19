package com.codershubinc.aaxion_mob.core.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Model representing cached OMDB metadata, movie/series poster, plot, and IMDb metrics.
 */
data class OmdbMediaItem(
    val queryKey: String,
    val rawPath: String = "",
    val imdbId: String = "",
    val title: String,
    val year: String = "",
    val type: String = "movie",
    val posterUrl: String = "",
    val plot: String = "",
    val genre: String = "",
    val rating: String = "",
    val runtime: String = "",
    val director: String = "",
    val actors: String = "",
    val lastUpdatedTs: Long = System.currentTimeMillis()
) {
    val isNotFound: Boolean
        get() = imdbId == "NOT_FOUND" || posterUrl == "NOT_FOUND"

    val hasPoster: Boolean
        get() = !isNotFound && posterUrl.isNotBlank() && posterUrl != "N/A"

    val hasRating: Boolean
        get() = !isNotFound && rating.isNotBlank() && rating != "N/A"

    val formattedRating: String
        get() = if (hasRating) "★ $rating" else ""
}

/**
 * Persistent SQLite cache for OMDB movie and series metadata and artwork.
 */
class OmdbMetadataCacheDb private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val _cachedItems = MutableStateFlow<Map<String, OmdbMediaItem>>(emptyMap())
    val cachedItems: StateFlow<Map<String, OmdbMediaItem>> = _cachedItems.asStateFlow()

    init {
        loadAllIntoMemory()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_OMDB (
                $COL_QUERY_KEY TEXT PRIMARY KEY,
                $COL_RAW_PATH TEXT,
                $COL_IMDB_ID TEXT,
                $COL_TITLE TEXT,
                $COL_YEAR TEXT,
                $COL_TYPE TEXT,
                $COL_POSTER_URL TEXT,
                $COL_PLOT TEXT,
                $COL_GENRE TEXT,
                $COL_RATING TEXT,
                $COL_RUNTIME TEXT,
                $COL_DIRECTOR TEXT,
                $COL_ACTORS TEXT,
                $COL_LAST_UPDATED INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_omdb_raw_path ON $TABLE_OMDB($COL_RAW_PATH)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OMDB")
        onCreate(db)
    }

    private fun loadAllIntoMemory() {
        val map = mutableMapOf<String, OmdbMediaItem>()
        try {
            val cursor = readableDatabase.query(
                TABLE_OMDB,
                null,
                null,
                null,
                null,
                null,
                "$COL_LAST_UPDATED DESC"
            )
            cursor.use { c ->
                val qIdx = c.getColumnIndex(COL_QUERY_KEY)
                val pIdx = c.getColumnIndex(COL_RAW_PATH)
                val iIdx = c.getColumnIndex(COL_IMDB_ID)
                val tIdx = c.getColumnIndex(COL_TITLE)
                val yIdx = c.getColumnIndex(COL_YEAR)
                val typeIdx = c.getColumnIndex(COL_TYPE)
                val postIdx = c.getColumnIndex(COL_POSTER_URL)
                val plotIdx = c.getColumnIndex(COL_PLOT)
                val gIdx = c.getColumnIndex(COL_GENRE)
                val rIdx = c.getColumnIndex(COL_RATING)
                val runIdx = c.getColumnIndex(COL_RUNTIME)
                val dIdx = c.getColumnIndex(COL_DIRECTOR)
                val aIdx = c.getColumnIndex(COL_ACTORS)
                val luIdx = c.getColumnIndex(COL_LAST_UPDATED)

                while (c.moveToNext()) {
                    val key = c.getString(qIdx)
                    val item = OmdbMediaItem(
                        queryKey = key,
                        rawPath = c.getString(pIdx) ?: "",
                        imdbId = c.getString(iIdx) ?: "",
                        title = c.getString(tIdx) ?: "",
                        year = c.getString(yIdx) ?: "",
                        type = c.getString(typeIdx) ?: "movie",
                        posterUrl = c.getString(postIdx) ?: "",
                        plot = c.getString(plotIdx) ?: "",
                        genre = c.getString(gIdx) ?: "",
                        rating = c.getString(rIdx) ?: "",
                        runtime = c.getString(runIdx) ?: "",
                        director = c.getString(dIdx) ?: "",
                        actors = c.getString(aIdx) ?: "",
                        lastUpdatedTs = c.getLong(luIdx)
                    )
                    map[key] = item
                }
            }
            _cachedItems.value = map
        } catch (_: Exception) {
        }
    }

    fun get(queryKey: String): OmdbMediaItem? {
        val normalized = queryKey.lowercase(Locale.US).trim()
        val direct = _cachedItems.value[normalized]
        if (direct != null) return direct

        val byPath = getByPath(queryKey)
        if (byPath != null) return byPath

        val parsed = com.codershubinc.aaxion_mob.core.media.MediaTitleParser.parse(queryKey)
        return getForMedia(parsed, queryKey)
    }

    fun getByPath(rawPath: String): OmdbMediaItem? {
        val items = _cachedItems.value
        return items.values.firstOrNull { it.rawPath == rawPath }
    }

    /**
     * Resolves metadata for a media item, hierarchically trying:
     * 1. Exact server path match
     * 2. Exact queryKey (e.g. "black clover:s1e1", "black clover:s1", "ex machina:2014")
     * 3. Season queryKey (e.g. "black clover:s1")
     * 4. Base series/movie title key (e.g. "black clover")
     */
    fun getForMedia(info: com.codershubinc.aaxion_mob.core.media.ParsedMediaInfo?, rawPath: String): OmdbMediaItem? {
        val byPath = getByPath(rawPath)
        if (byPath != null) return byPath
        if (info == null) return null

        val items = _cachedItems.value
        // 1. Exact query key
        items[info.queryKey.lowercase(Locale.US)]?.let { return it }
        // 2. Season query key
        info.seasonKey?.let { sk -> items[sk.lowercase(Locale.US)]?.let { return it } }
        // 3. Base series key (only for TV Series fallback across sibling seasons/episodes)
        if (info.isSeries) {
            items[info.seriesKey.lowercase(Locale.US)]?.let { return it }
        }

        return null
    }

    fun put(item: OmdbMediaItem) {
        val normalizedKey = item.queryKey.lowercase(Locale.US).trim()
        val values = ContentValues().apply {
            put(COL_QUERY_KEY, normalizedKey)
            put(COL_RAW_PATH, item.rawPath)
            put(COL_IMDB_ID, item.imdbId)
            put(COL_TITLE, item.title)
            put(COL_YEAR, item.year)
            put(COL_TYPE, item.type)
            put(COL_POSTER_URL, item.posterUrl)
            put(COL_PLOT, item.plot)
            put(COL_GENRE, item.genre)
            put(COL_RATING, item.rating)
            put(COL_RUNTIME, item.runtime)
            put(COL_DIRECTOR, item.director)
            put(COL_ACTORS, item.actors)
            put(COL_LAST_UPDATED, item.lastUpdatedTs)
        }

        try {
            writableDatabase.insertWithOnConflict(
                TABLE_OMDB,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            val updated = _cachedItems.value.toMutableMap()
            updated[normalizedKey] = item.copy(queryKey = normalizedKey)
            _cachedItems.value = updated
        } catch (_: Exception) {
        }
    }

    fun clear() {
        try {
            writableDatabase.delete(TABLE_OMDB, null, null)
            _cachedItems.value = emptyMap()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val DATABASE_NAME = "aaxion_omdb_metadata_cache.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_OMDB = "omdb_metadata_cache"
        private const val COL_QUERY_KEY = "query_key"
        private const val COL_RAW_PATH = "raw_path"
        private const val COL_IMDB_ID = "imdb_id"
        private const val COL_TITLE = "title"
        private const val COL_YEAR = "year"
        private const val COL_TYPE = "type"
        private const val COL_POSTER_URL = "poster_url"
        private const val COL_PLOT = "plot"
        private const val COL_GENRE = "genre"
        private const val COL_RATING = "rating"
        private const val COL_RUNTIME = "runtime"
        private const val COL_DIRECTOR = "director"
        private const val COL_ACTORS = "actors"
        private const val COL_LAST_UPDATED = "last_updated_ts"

        @Volatile
        private var instance: OmdbMetadataCacheDb? = null

        fun getInstance(context: Context): OmdbMetadataCacheDb {
            return instance ?: synchronized(this) {
                instance ?: OmdbMetadataCacheDb(context.applicationContext).also { instance = it }
            }
        }
    }
}
