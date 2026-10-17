package com.codershubinc.aaxion_mob.core.media

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import com.codershubinc.aaxion_mob.core.data.VideoMetadataCacheDb
import com.codershubinc.aaxion_mob.core.data.VideoPlaybackMeta
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.ui.designsystem.formatTimeMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Playback progress and resume metadata for a video file.
 */
data class VideoProgress(
    val rawPath: String,
    val positionMs: Long,
    val durationMs: Long = 0L,
    val lastUpdatedTs: Long = System.currentTimeMillis()
) {
    val formattedPosition: String
        get() = formatTimeMs(positionMs)
    val formattedDuration: String
        get() = if (durationMs > 0) formatTimeMs(durationMs) else ""
}

/**
 * Active video playback session holding metadata, state, and visibility.
 */
data class VlcPlaybackSession(
    val rawPath: String,
    val file: FileItem,
    val streamUrl: String,
    val meta: VideoPlaybackMeta,
    val isProbing: Boolean = false,
    val isPlayingInVlc: Boolean = true,
    val isVisible: Boolean = true
)

/**
 * Dedicated manager for video playback sessions with progress tracking,
 * metadata extraction, live position updates, and persistent database caching.
 */
class VlcPlayerManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aaxion_video_progress", Context.MODE_PRIVATE)

    private val cacheDb = VideoMetadataCacheDb.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeSessionTracking: ActiveVideoSession? = null
    private var videoLauncher: androidx.activity.result.ActivityResultLauncher<Intent>? = null
    private var playbackTickerJob: Job? = null

    private val _progressUpdates = MutableStateFlow<Map<String, Long>>(emptyMap())
    val progressUpdates: StateFlow<Map<String, Long>> = _progressUpdates.asStateFlow()

    private val _activeVideoSession = MutableStateFlow<VlcPlaybackSession?>(null)
    val activeSession: StateFlow<VlcPlaybackSession?> = _activeVideoSession.asStateFlow()

    private data class ActiveVideoSession(
        val rawPath: String,
        val startPositionMs: Long,
        val startTimestamp: Long
    )

    fun setLauncher(launcher: androidx.activity.result.ActivityResultLauncher<Intent>?) {
        this.videoLauncher = launcher
    }

    fun getProgress(rawPath: String): VideoProgress? {
        val keyPos = "${rawPath.hashCode()}_pos"
        if (!prefs.contains(keyPos)) {
            val cached = cacheDb.get(rawPath)
            if (cached != null && cached.positionMs > 0L) {
                return VideoProgress(
                    rawPath = rawPath,
                    positionMs = cached.positionMs,
                    durationMs = cached.durationMs,
                    lastUpdatedTs = cached.lastWatchedTs
                )
            }
            return null
        }
        val pos = prefs.getLong(keyPos, 0L)
        val dur = prefs.getLong("${rawPath.hashCode()}_dur", 0L)
        val ts = prefs.getLong("${rawPath.hashCode()}_ts", 0L)
        return VideoProgress(rawPath = rawPath, positionMs = pos, durationMs = dur, lastUpdatedTs = ts)
    }

    fun hasResumePosition(rawPath: String): Boolean {
        val progress = getProgress(rawPath) ?: return false
        return progress.positionMs >= 5000L
    }

    fun getVideoMeta(rawPath: String): VideoPlaybackMeta? {
        return cacheDb.get(rawPath)
    }

    fun saveProgress(rawPath: String, positionMs: Long, durationMs: Long = 0L) {
        val safePos = positionMs.coerceAtLeast(0L)
        prefs.edit()
            .putLong("${rawPath.hashCode()}_pos", safePos)
            .putLong("${rawPath.hashCode()}_dur", durationMs)
            .putLong("${rawPath.hashCode()}_ts", System.currentTimeMillis())
            .apply()

        _progressUpdates.value = _progressUpdates.value + (rawPath to safePos)
        RecentPlaybackManager.getInstance(context).updateProgress(rawPath, safePos, durationMs)
        cacheDb.updatePosition(rawPath, safePos, durationMs)

        _activeVideoSession.value?.let { current ->
            if (current.rawPath == rawPath) {
                val safeDur = if (durationMs > 0L) durationMs else current.meta.durationMs
                val updatedMeta = current.meta.copy(
                    positionMs = safePos,
                    durationMs = safeDur,
                    lastWatchedTs = System.currentTimeMillis()
                )
                _activeVideoSession.value = current.copy(
                    meta = updatedMeta,
                    isPlayingInVlc = false
                )
            }
        }
    }

    fun clearProgress(rawPath: String) {
        playbackTickerJob?.cancel()
        activeSessionTracking = null

        prefs.edit()
            .remove("${rawPath.hashCode()}_pos")
            .remove("${rawPath.hashCode()}_dur")
            .remove("${rawPath.hashCode()}_ts")
            .apply()

        _progressUpdates.value = _progressUpdates.value - rawPath
        cacheDb.delete(rawPath)

        if (_activeVideoSession.value?.rawPath == rawPath) {
            _activeVideoSession.value = null
        }
    }

    fun dismissSession() {
        playbackTickerJob?.cancel()
        _activeVideoSession.value = _activeVideoSession.value?.copy(isVisible = false)
    }

    fun showSession() {
        _activeVideoSession.value = _activeVideoSession.value?.copy(isVisible = true)
    }

    fun isVlcInstalled(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage("org.videolan.vlc")
                setDataAndType(Uri.parse("http://dummy"), "video/*")
            }
            context.packageManager.resolveActivity(intent, 0) != null
        } catch (e: Exception) {
            false
        }
    }

    fun launchVlc(
        context: Context,
        file: FileItem,
        streamUrl: String,
        resume: Boolean = false,
        token: String = ""
    ) {
        val rawPath = file.rawPath
        val cached = cacheDb.get(rawPath)
        val startPositionMs = if (resume) {
            getProgress(rawPath)?.positionMs ?: cached?.positionMs ?: 0L
        } else {
            0L
        }

        val knownDuration = cached?.durationMs?.takeIf { it > 0L }
            ?: getProgress(rawPath)?.durationMs?.takeIf { it > 0L }
            ?: RecentPlaybackManager.getInstance(context).recentItems.value.firstOrNull { it.rawPath == rawPath }?.durationMs?.takeIf { it > 0L }
            ?: 0L

        val initialMeta = cached?.copy(
            positionMs = startPositionMs,
            durationMs = if (knownDuration > 0L) knownDuration else (cached.durationMs)
        ) ?: VideoPlaybackMeta(
            rawPath = rawPath,
            title = file.displayName(40),
            streamUrl = streamUrl,
            durationMs = knownDuration,
            positionMs = startPositionMs,
            fileSize = file.size,
            lastWatchedTs = System.currentTimeMillis(),
            source = "VLC"
        )

        cacheDb.upsert(initialMeta)

        _activeVideoSession.value = VlcPlaybackSession(
            rawPath = rawPath,
            file = file,
            streamUrl = streamUrl,
            meta = initialMeta,
            isProbing = initialMeta.durationMs == 0L || initialMeta.width == 0,
            isPlayingInVlc = true,
            isVisible = true
        )

        RecentPlaybackManager.getInstance(context).recordPlayback(
            RecentMediaItem(
                id = rawPath,
                title = file.displayName(40),
                subtitle = "Video",
                rawPath = rawPath,
                isVideo = true,
                isAudio = false,
                durationMs = initialMeta.durationMs,
                progressMs = startPositionMs,
                size = file.size
            )
        )

        activeSessionTracking = ActiveVideoSession(
            rawPath = rawPath,
            startPositionMs = startPositionMs,
            startTimestamp = System.currentTimeMillis()
        )

        startPlaybackTicker()
        probeMetadataAsync(streamUrl, token, file, initialMeta)

        val uri = Uri.parse(streamUrl)
        val launcher = videoLauncher
        val vlcIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            setPackage("org.videolan.vlc")
            putExtra("title", file.name)
            putExtra("extra_title", file.name)
            putExtra(Intent.EXTRA_TITLE, file.name)
            putExtra("from_start", !resume)
            if (startPositionMs > 0L) {
                putExtra("position", startPositionMs)
                putExtra("extra_position", startPositionMs)
            }
            putExtra("return_result", true)
            if (launcher == null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        try {
            if (launcher != null) {
                launcher.launch(vlcIntent)
            } else {
                context.startActivity(vlcIntent)
            }
        } catch (e: Exception) {
            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                putExtra("title", file.name)
                putExtra("extra_title", file.name)
                putExtra(Intent.EXTRA_TITLE, file.name)
                putExtra("from_start", !resume)
                if (startPositionMs > 0L) {
                    putExtra("position", startPositionMs)
                    putExtra("extra_position", startPositionMs)
                }
                if (launcher == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            try {
                val chooser = Intent.createChooser(genericIntent, "Play video with...")
                if (launcher != null) {
                    launcher.launch(chooser)
                } else {
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            } catch (err: Exception) {
                Toast.makeText(context, "No video player available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val tracking = activeSessionTracking ?: break
                val current = _activeVideoSession.value ?: break
                if (!current.isPlayingInVlc || current.rawPath != tracking.rawPath) break

                val elapsed = (System.currentTimeMillis() - tracking.startTimestamp).coerceAtLeast(0L)
                val currentPos = tracking.startPositionMs + elapsed
                val dur = current.meta.durationMs
                val clampedPos = if (dur > 0L) currentPos.coerceAtMost(dur) else currentPos

                val updatedMeta = current.meta.copy(
                    positionMs = clampedPos,
                    lastWatchedTs = System.currentTimeMillis()
                )
                _activeVideoSession.value = current.copy(meta = updatedMeta)

                // Periodically flush tracked progress every 3 seconds
                if (elapsed % 3000L < 1000L) {
                    cacheDb.updatePosition(tracking.rawPath, clampedPos, dur)
                    RecentPlaybackManager.getInstance(context).updateProgress(tracking.rawPath, clampedPos, dur)
                }
            }
        }
    }

    fun inspectVideo(file: FileItem, streamUrl: String, token: String = "") {
        val rawPath = file.rawPath
        val cached = cacheDb.get(rawPath)
        val savedPos = getProgress(rawPath)?.positionMs ?: cached?.positionMs ?: 0L

        val initialMeta = cached ?: VideoPlaybackMeta(
            rawPath = rawPath,
            title = file.displayName(40),
            streamUrl = streamUrl,
            durationMs = getProgress(rawPath)?.durationMs ?: 0L,
            positionMs = savedPos,
            fileSize = file.size,
            lastWatchedTs = System.currentTimeMillis(),
            source = "VLC"
        )

        _activeVideoSession.value = VlcPlaybackSession(
            rawPath = rawPath,
            file = file,
            streamUrl = streamUrl,
            meta = initialMeta,
            isProbing = initialMeta.width == 0 || initialMeta.durationMs == 0L,
            isPlayingInVlc = false,
            isVisible = true
        )

        if (initialMeta.width == 0 || initialMeta.durationMs == 0L) {
            probeMetadataAsync(streamUrl, token, file, initialMeta)
        }
    }

    private fun probeMetadataAsync(
        streamUrl: String,
        token: String,
        file: FileItem,
        baseMeta: VideoPlaybackMeta
    ) {
        scope.launch {
            var width = baseMeta.width
            var height = baseMeta.height
            var duration = baseMeta.durationMs
            var bitrate = baseMeta.bitrate
            var mime = baseMeta.mimeType

            // 1. Probe using MediaMetadataRetriever
            try {
                val retriever = MediaMetadataRetriever()
                val headers = if (token.isNotBlank()) {
                    mapOf("Authorization" to "Bearer $token")
                } else {
                    emptyMap()
                }
                retriever.setDataSource(streamUrl, headers)
                val probedWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val probedHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val probedDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val probedBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
                val probedMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
                try {
                    retriever.release()
                } catch (_: Exception) {}

                if (probedWidth > 0) width = probedWidth
                if (probedHeight > 0) height = probedHeight
                if (probedDuration > 0L) duration = probedDuration
                if (probedBitrate > 0L) bitrate = probedBitrate
                if (probedMime.isNotBlank()) mime = probedMime
            } catch (_: Exception) {}

            // 2. Fallback probe using MediaPlayer header parsing if duration is still unknown
            if (duration <= 0L) {
                try {
                    val mp = MediaPlayer()
                    if (token.isNotBlank()) {
                        mp.setDataSource(context, Uri.parse(streamUrl), mapOf("Authorization" to "Bearer $token"))
                    } else {
                        mp.setDataSource(streamUrl)
                    }
                    mp.prepare()
                    val mpDur = mp.duration.toLong().coerceAtLeast(0L)
                    val mpW = mp.videoWidth
                    val mpH = mp.videoHeight
                    mp.release()
                    if (mpDur > 0L) duration = mpDur
                    if (mpW > 0 && width <= 0) width = mpW
                    if (mpH > 0 && height <= 0) height = mpH
                } catch (_: Exception) {}
            }

            // 3. Fallback estimation based on stream file size and bitrate
            if (duration <= 0L && file.size > 0L) {
                val effectiveBitrate = if (bitrate > 0L) bitrate else 2_500_000L
                duration = (file.size * 8000L) / effectiveBitrate
            }

            val updated = baseMeta.copy(
                width = width,
                height = height,
                durationMs = duration,
                bitrate = bitrate,
                mimeType = mime,
                streamUrl = streamUrl,
                lastWatchedTs = System.currentTimeMillis()
            )

            cacheDb.upsert(updated)

            if (_activeVideoSession.value?.rawPath == file.rawPath) {
                _activeVideoSession.value = _activeVideoSession.value?.copy(
                    meta = updated,
                    isProbing = false
                )
            }
        }
    }

    fun handleActivityResult(positionMs: Long, durationMs: Long = 0L) {
        playbackTickerJob?.cancel()
        val session = activeSessionTracking
        val currentMeta = _activeVideoSession.value?.meta

        val safeDur = when {
            durationMs > 0L -> durationMs
            currentMeta != null && currentMeta.durationMs > 0L -> currentMeta.durationMs
            session != null -> getProgress(session.rawPath)?.durationMs ?: 0L
            else -> 0L
        }

        val finalPos = when {
            positionMs > 0L -> positionMs
            session != null -> {
                val elapsedMs = System.currentTimeMillis() - session.startTimestamp
                if (elapsedMs > 1500L) {
                    session.startPositionMs + elapsedMs
                } else {
                    currentMeta?.positionMs ?: session.startPositionMs
                }
            }
            currentMeta != null -> currentMeta.positionMs
            else -> 0L
        }

        if (session != null) {
            saveProgress(session.rawPath, finalPos, safeDur)
        }

        _activeVideoSession.value?.let { current ->
            val updatedMeta = current.meta.copy(
                positionMs = finalPos,
                durationMs = safeDur,
                lastWatchedTs = System.currentTimeMillis()
            )
            _activeVideoSession.value = current.copy(
                meta = updatedMeta,
                isPlayingInVlc = false
            )
        }
        activeSessionTracking = null
    }

    fun onActivityResumed() {
        val session = activeSessionTracking ?: return
        val elapsedMs = System.currentTimeMillis() - session.startTimestamp
        val currentMeta = _activeVideoSession.value?.meta
        val livePos = currentMeta?.positionMs ?: (session.startPositionMs + elapsedMs)
        val safeDur = currentMeta?.durationMs ?: (getProgress(session.rawPath)?.durationMs ?: 0L)

        if (elapsedMs > 1500L) {
            saveProgress(session.rawPath, livePos, safeDur)
        }
        playbackTickerJob?.cancel()
        _activeVideoSession.value?.let { current ->
            _activeVideoSession.value = current.copy(
                meta = current.meta.copy(positionMs = livePos, durationMs = safeDur),
                isPlayingInVlc = false
            )
        }
        activeSessionTracking = null
    }

    companion object {
        @Volatile
        private var instance: VlcPlayerManager? = null

        fun getInstance(context: Context): VlcPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: VlcPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
