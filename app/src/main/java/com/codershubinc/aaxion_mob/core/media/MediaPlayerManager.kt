package com.codershubinc.aaxion_mob.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.view.Surface
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.model.MusicTrack
import com.codershubinc.aaxion_mob.core.network.AaxionUrlBuilder
import com.codershubinc.aaxion_mob.core.network.buildImageViewUrl
import com.codershubinc.aaxion_mob.core.network.buildStreamUrl
import com.codershubinc.aaxion_mob.core.network.buildThumbUrl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State representing active media playback in the Floating Island Player.
 */
data class MediaPlaybackState(
    val currentFile: FileItem? = null,
    val streamUrl: String = "",
    val artworkUrl: String? = null,
    val isVideo: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLooping: Boolean = false,
    val isShuffle: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val isExpanded: Boolean = false,
    val isVisible: Boolean = false,
    val errorMessage: String? = null,
    val queueSize: Int = 0,
    val queueIndex: Int = -1,
    val displayTitle: String? = null,
    val displayArtist: String? = null,
    val displayAlbum: String? = null,
    val musicTrackId: Long? = null
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Resolves local folder artwork for a track by checking for same-named image files
 * or standard cover art in the same directory.
 */
object ArtworkResolver {
    fun findArtworkUrl(
        trackFile: FileItem,
        siblingFiles: List<FileItem>?,
        baseUrl: String,
        token: String
    ): String? {
        if (siblingFiles.isNullOrEmpty()) {
            return if (trackFile.isVideo) trackFile.buildThumbUrl(baseUrl, token) else null
        }

        val trackBaseName = trackFile.nameWithoutExtension.trim()
        if (trackBaseName.isEmpty()) return null

        // 1. Exact match on track base name (e.g. "Song.jpg" for "Song.mp3")
        val exactMatch = siblingFiles.firstOrNull { sibling ->
            sibling.isImage && sibling.nameWithoutExtension.equals(trackBaseName, ignoreCase = true)
        }
        if (exactMatch != null) {
            return exactMatch.buildImageViewUrl(baseUrl, token)
        }

        // 2. Exact match ignoring punctuation and special characters
        val cleanTrack = trackBaseName.lowercase().filter { it.isLetterOrDigit() }
        if (cleanTrack.isNotEmpty()) {
            val normalizedMatch = siblingFiles.firstOrNull { sibling ->
                sibling.isImage && sibling.nameWithoutExtension.lowercase().filter { it.isLetterOrDigit() } == cleanTrack
            }
            if (normalizedMatch != null) {
                return normalizedMatch.buildImageViewUrl(baseUrl, token)
            }
        }

        // 3. Common directory album art names (cover, folder, album, poster, artwork, front)
        val directoryArtNames = setOf(
            "cover", "folder", "album", "poster", "artwork", "front", "albumart",
            "default", "backdrop", "fanart"
        )
        val directoryArt = siblingFiles.firstOrNull { sibling ->
            sibling.isImage && sibling.nameWithoutExtension.lowercase() in directoryArtNames
        }
        if (directoryArt != null) {
            return directoryArt.buildImageViewUrl(baseUrl, token)
        }

        // 4. Any image that starts with or contains the track name
        val prefixMatch = siblingFiles.firstOrNull { sibling ->
            sibling.isImage && (
                sibling.nameWithoutExtension.startsWith(trackBaseName, ignoreCase = true) ||
                trackBaseName.startsWith(sibling.nameWithoutExtension, ignoreCase = true)
            )
        }
        if (prefixMatch != null) {
            return prefixMatch.buildImageViewUrl(baseUrl, token)
        }

        // 5. Fallback for video thumbnails
        return if (trackFile.isVideo) trackFile.buildThumbUrl(baseUrl, token) else null
    }
}

/**
 * Centralized, singleton media player manager that powers in-app floating island playback.
 * Supports continuous background streaming for audio and video while users navigate the app.
 */
class MediaPlayerManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var currentSurface: Surface? = null
    private var progressJob: Job? = null

    private var activeQueue: List<FileItem> = emptyList()
    private var activeMusicQueue: List<MusicTrack>? = null
    private var activeQueueIndex: Int = -1
    private var activeBaseUrl: String? = null
    private var activeToken: String? = null
    private var activeSiblingFiles: List<FileItem>? = null

    private val _state = MutableStateFlow(MediaPlaybackState())
    val state: StateFlow<MediaPlaybackState> = _state.asStateFlow()

    private val sessionManager = AaxionMediaSessionManager(appContext)

    init {
        scope.launch {
            _state.collect { state ->
                sessionManager.update(state)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: MediaPlayerManager? = null

        fun getInstance(context: Context): MediaPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MediaPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Starts playback for a given FileItem and direct stream URL.
     * Automatically scans siblingFiles in the same directory for matching album art / poster image.
     */
    fun play(
        file: FileItem,
        streamUrl: String,
        isVideo: Boolean = file.isVideo,
        token: String? = null,
        baseUrl: String? = null,
        siblingFiles: List<FileItem>? = null,
        artworkUrlOverride: String? = null,
        displayTitle: String? = null,
        displayArtist: String? = null,
        displayAlbum: String? = null,
        musicTrackId: Long? = null
    ) {
        scope.launch {
            stopAndResetPlayer()

            val effectiveBaseUrl = baseUrl ?: extractBaseUrl(streamUrl)
            val effectiveToken = token ?: extractToken(streamUrl)
            val resolvedArt = artworkUrlOverride ?: if (effectiveBaseUrl.isNotEmpty()) {
                ArtworkResolver.findArtworkUrl(file, siblingFiles, effectiveBaseUrl, effectiveToken)
            } else null

            _state.update {
                it.copy(
                    currentFile = file,
                    streamUrl = streamUrl,
                    artworkUrl = resolvedArt,
                    isVideo = isVideo,
                    isPlaying = false,
                    isBuffering = true,
                    playbackSpeed = 1.0f,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    videoWidth = 0,
                    videoHeight = 0,
                    isExpanded = isVideo,
                    isVisible = true,
                    errorMessage = null,
                    displayTitle = displayTitle,
                    displayArtist = displayArtist,
                    displayAlbum = displayAlbum,
                    musicTrackId = musicTrackId
                )
            }

            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    try {
                        setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                    } catch (e: Exception) {
                        // ignore permission issue if any
                    }

                    if (isVideo && currentSurface != null && currentSurface!!.isValid) {
                        setSurface(currentSurface)
                    }

                    val uri = Uri.parse(streamUrl)
                    if (!effectiveToken.isNullOrBlank()) {
                        val headers = mapOf("Authorization" to "Bearer $effectiveToken")
                        setDataSource(appContext, uri, headers)
                    } else {
                        setDataSource(appContext, uri)
                    }

                    setOnPreparedListener { player ->
                        val dur = try { player.duration.toLong().coerceAtLeast(0L) } catch (e: Exception) { 0L }
                        _state.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = true,
                                durationMs = dur
                            )
                        }
                        player.isLooping = _state.value.isLooping
                        applyPlaybackSpeed(player, _state.value.playbackSpeed)
                        player.start()
                        startProgressTracker()
                    }

                    setOnVideoSizeChangedListener { _, width, height ->
                        if (width > 0 && height > 0) {
                            _state.update { it.copy(videoWidth = width, videoHeight = height) }
                        }
                    }

                    setOnBufferingUpdateListener { _, percent ->
                        // Buffering update
                    }

                    setOnCompletionListener {
                        if (!_state.value.isLooping && hasNextTrack()) {
                            playNext()
                        } else if (!_state.value.isLooping) {
                            _state.update { it.copy(isPlaying = false, currentPositionMs = it.durationMs) }
                            stopProgressTracker()
                        }
                    }

                    setOnErrorListener { _, what, extra ->
                        _state.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = false,
                                errorMessage = "Playback error ($what, $extra)"
                            )
                        }
                        stopProgressTracker()
                        true
                    }

                    prepareAsync()
                }

                mediaPlayer = mp
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = e.message ?: "Failed to initialize playback"
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                _state.update { it.copy(isPlaying = false) }
                stopProgressTracker()
            } else {
                mp.start()
                _state.update { it.copy(isPlaying = true) }
                startProgressTracker()
            }
        } catch (e: Exception) {
            // Player might be in an invalid state
        }
    }

    fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        try {
            val clamped = positionMs.coerceIn(0L, _state.value.durationMs)
            mp.seekTo(clamped.toInt())
            _state.update { it.copy(currentPositionMs = clamped) }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun seekRelative(offsetMs: Long) {
        val current = _state.value.currentPositionMs
        seekTo(current + offsetMs)
    }

    fun toggleLoop() {
        val newLoop = !_state.value.isLooping
        _state.update { it.copy(isLooping = newLoop) }
        try {
            mediaPlayer?.isLooping = newLoop
        } catch (e: Exception) {
            // ignore
        }
    }

    fun setExpanded(expanded: Boolean) {
        _state.update { it.copy(isExpanded = expanded) }
    }

    fun toggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    /**
     * If artwork was missing at start, dynamically resolves it from current folder files.
     */
    fun syncArtworkIfAvailable(siblingFiles: List<FileItem>, baseUrl: String, token: String) {
        val current = _state.value.currentFile ?: return
        if (_state.value.artworkUrl == null) {
            val art = ArtworkResolver.findArtworkUrl(current, siblingFiles, baseUrl, token)
            if (art != null) {
                _state.update { it.copy(artworkUrl = art) }
            }
        }
    }

    /**
     * Sets playback speed (0.75x, 1.0x, 1.25x, 1.5x, 2.0x).
     */
    fun setPlaybackSpeed(speed: Float) {
        val mp = mediaPlayer ?: return
        applyPlaybackSpeed(mp, speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
        val current = _state.value.playbackSpeed
        val nextIdx = (speeds.indexOf(current) + 1).let { if (it in speeds.indices) it else 0 }
        setPlaybackSpeed(speeds[nextIdx])
    }

    /**
     * Starts playback for a single server-indexed MusicTrack.
     */
    fun playMusicTrack(
        track: MusicTrack,
        token: String,
        baseUrl: String
    ) {
        activeMusicQueue = null
        activeQueue = emptyList()
        val fileItem = track.toFileItem()
        val streamUrl = AaxionUrlBuilder.buildMusicStreamUrl(baseUrl, track.id, token)
        val artworkUrl = if (track.imagePath.isNotBlank()) {
            AaxionUrlBuilder.buildMusicArtworkUrl(baseUrl, track.imagePath, token)
        } else null

        play(
            file = fileItem,
            streamUrl = streamUrl,
            isVideo = false,
            token = token,
            baseUrl = baseUrl,
            artworkUrlOverride = artworkUrl,
            displayTitle = track.displayTitle,
            displayArtist = track.displayArtist,
            displayAlbum = track.album,
            musicTrackId = track.id
        )
    }

    /**
     * Starts queue playback for a list of server-indexed MusicTracks with optional shuffle.
     */
    fun playMusicQueue(
        tracks: List<MusicTrack>,
        startIndex: Int = 0,
        shuffle: Boolean = false,
        token: String,
        baseUrl: String
    ) {
        if (tracks.isEmpty()) return
        val queue = if (shuffle) tracks.shuffled() else tracks
        activeMusicQueue = queue
        activeQueue = emptyList()
        activeBaseUrl = baseUrl
        activeToken = token
        activeSiblingFiles = null
        val idx = startIndex.coerceIn(0, queue.size - 1)
        activeQueueIndex = idx
        _state.update { it.copy(isShuffle = shuffle, queueSize = queue.size, queueIndex = idx) }

        val target = queue[idx]
        val fileItem = target.toFileItem()
        val streamUrl = AaxionUrlBuilder.buildMusicStreamUrl(baseUrl, target.id, token)
        val artworkUrl = if (target.imagePath.isNotBlank()) {
            AaxionUrlBuilder.buildMusicArtworkUrl(baseUrl, target.imagePath, token)
        } else null

        play(
            file = fileItem,
            streamUrl = streamUrl,
            isVideo = false,
            token = token,
            baseUrl = baseUrl,
            artworkUrlOverride = artworkUrl,
            displayTitle = target.displayTitle,
            displayArtist = target.displayArtist,
            displayAlbum = target.album,
            musicTrackId = target.id
        )
    }

    /**
     * Starts playback for a list of tracks with optional shuffle.
     * Automatically sets the active queue and starts with startIndex.
     */
    fun playQueue(
        tracks: List<FileItem>,
        startIndex: Int = 0,
        shuffle: Boolean = false,
        token: String? = null,
        baseUrl: String? = null,
        siblingFiles: List<FileItem>? = null
    ) {
        if (tracks.isEmpty()) return
        val queue = if (shuffle) tracks.shuffled() else tracks
        activeMusicQueue = null
        activeQueue = queue
        activeBaseUrl = baseUrl
        activeToken = token
        activeSiblingFiles = siblingFiles
        val idx = startIndex.coerceIn(0, queue.size - 1)
        activeQueueIndex = idx
        _state.update { it.copy(isShuffle = shuffle, queueSize = queue.size, queueIndex = idx) }
        val target = queue[idx]
        val effectiveBase = baseUrl ?: extractBaseUrl(target.rawPath)
        val effectiveTkn = token ?: ""
        val streamUrl = target.buildStreamUrl(effectiveBase, effectiveTkn)
        play(
            file = target,
            streamUrl = streamUrl,
            isVideo = target.isVideo,
            token = effectiveTkn,
            baseUrl = effectiveBase,
            siblingFiles = siblingFiles
        )
    }

    fun playNext() {
        if (!activeMusicQueue.isNullOrEmpty()) {
            val mQueue = activeMusicQueue!!
            val nextIdx = if (activeQueueIndex + 1 < mQueue.size) {
                activeQueueIndex + 1
            } else if (_state.value.isLooping) {
                0
            } else return

            activeQueueIndex = nextIdx
            _state.update { it.copy(queueIndex = nextIdx) }
            val target = mQueue[nextIdx]
            val effectiveBase = activeBaseUrl ?: ""
            val effectiveTkn = activeToken ?: ""
            val fileItem = target.toFileItem()
            val streamUrl = AaxionUrlBuilder.buildMusicStreamUrl(effectiveBase, target.id, effectiveTkn)
            val artworkUrl = if (target.imagePath.isNotBlank()) {
                AaxionUrlBuilder.buildMusicArtworkUrl(effectiveBase, target.imagePath, effectiveTkn)
            } else null

            play(
                file = fileItem,
                streamUrl = streamUrl,
                isVideo = false,
                token = effectiveTkn,
                baseUrl = effectiveBase,
                artworkUrlOverride = artworkUrl,
                displayTitle = target.displayTitle,
                displayArtist = target.displayArtist,
                displayAlbum = target.album,
                musicTrackId = target.id
            )
            return
        }

        if (activeQueue.isEmpty()) return
        val nextIdx = if (activeQueueIndex + 1 < activeQueue.size) {
            activeQueueIndex + 1
        } else if (_state.value.isLooping) {
            0
        } else return

        activeQueueIndex = nextIdx
        _state.update { it.copy(queueIndex = nextIdx) }
        val target = activeQueue[nextIdx]
        val effectiveBase = activeBaseUrl ?: ""
        val effectiveTkn = activeToken ?: ""
        val streamUrl = target.buildStreamUrl(effectiveBase, effectiveTkn)
        play(
            file = target,
            streamUrl = streamUrl,
            isVideo = target.isVideo,
            token = effectiveTkn,
            baseUrl = effectiveBase,
            siblingFiles = activeSiblingFiles
        )
    }

    fun playPrevious() {
        if (!activeMusicQueue.isNullOrEmpty()) {
            if (_state.value.currentPositionMs > 3000L) {
                seekTo(0L)
                return
            }
            val mQueue = activeMusicQueue!!
            val prevIdx = if (activeQueueIndex - 1 >= 0) {
                activeQueueIndex - 1
            } else {
                0
            }
            activeQueueIndex = prevIdx
            _state.update { it.copy(queueIndex = prevIdx) }
            val target = mQueue[prevIdx]
            val effectiveBase = activeBaseUrl ?: ""
            val effectiveTkn = activeToken ?: ""
            val fileItem = target.toFileItem()
            val streamUrl = AaxionUrlBuilder.buildMusicStreamUrl(effectiveBase, target.id, effectiveTkn)
            val artworkUrl = if (target.imagePath.isNotBlank()) {
                AaxionUrlBuilder.buildMusicArtworkUrl(effectiveBase, target.imagePath, effectiveTkn)
            } else null

            play(
                file = fileItem,
                streamUrl = streamUrl,
                isVideo = false,
                token = effectiveTkn,
                baseUrl = effectiveBase,
                artworkUrlOverride = artworkUrl,
                displayTitle = target.displayTitle,
                displayArtist = target.displayArtist,
                displayAlbum = target.album,
                musicTrackId = target.id
            )
            return
        }

        if (activeQueue.isEmpty()) return
        if (_state.value.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }
        val prevIdx = if (activeQueueIndex - 1 >= 0) {
            activeQueueIndex - 1
        } else {
            0
        }
        activeQueueIndex = prevIdx
        _state.update { it.copy(queueIndex = prevIdx) }
        val target = activeQueue[prevIdx]
        val effectiveBase = activeBaseUrl ?: ""
        val effectiveTkn = activeToken ?: ""
        val streamUrl = target.buildStreamUrl(effectiveBase, effectiveTkn)
        play(
            file = target,
            streamUrl = streamUrl,
            isVideo = target.isVideo,
            token = effectiveTkn,
            baseUrl = effectiveBase,
            siblingFiles = activeSiblingFiles
        )
    }

    fun hasNextTrack(): Boolean {
        val size = activeMusicQueue?.size ?: activeQueue.size
        return size > 0 && (activeQueueIndex + 1 < size || _state.value.isLooping)
    }

    fun hasPreviousTrack(): Boolean {
        val size = activeMusicQueue?.size ?: activeQueue.size
        return size > 0 && activeQueueIndex > 0
    }

    private fun applyPlaybackSpeed(player: MediaPlayer, speed: Float) {
        try {
            val params = player.playbackParams
            params.speed = speed
            player.playbackParams = params
        } catch (e: Exception) {
            // ignore if unsupported on current audio device
        }
    }

    private fun extractBaseUrl(url: String): String {
        return if (url.contains("/api/")) url.substringBefore("/api/") else ""
    }

    private fun extractToken(url: String): String {
        return if (url.contains("tkn=")) url.substringAfter("tkn=").substringBefore("&") else ""
    }

    fun attachSurface(surface: Surface?) {
        if (currentSurface != null && currentSurface != surface) {
            try {
                currentSurface?.release()
            } catch (e: Exception) {
                // ignore
            }
        }
        currentSurface = surface
        try {
            if (surface != null && surface.isValid) {
                mediaPlayer?.setSurface(surface)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun detachSurface() {
        try {
            mediaPlayer?.setSurface(null)
        } catch (e: Exception) {
            // ignore
        }
        try {
            currentSurface?.release()
        } catch (e: Exception) {
            // ignore
        }
        currentSurface = null
    }

    fun close() {
        stopProgressTracker()
        stopAndResetPlayer()
        detachSurface()
        _state.update { MediaPlaybackState() }
        sessionManager.clear()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null) {
                    try {
                        if (mp.isPlaying) {
                            val pos = mp.currentPosition.toLong()
                            val dur = mp.duration.toLong().coerceAtLeast(0L)
                            _state.update { it.copy(currentPositionMs = pos, durationMs = dur, isPlaying = true) }
                        }
                    } catch (e: Exception) {
                        // ignore state query error during transition
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopAndResetPlayer() {
        stopProgressTracker()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            // ignore cleanup errors
        } finally {
            mediaPlayer = null
        }
    }
}
