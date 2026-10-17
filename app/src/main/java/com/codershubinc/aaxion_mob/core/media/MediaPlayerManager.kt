package com.codershubinc.aaxion_mob.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.view.Surface
import com.codershubinc.aaxion_mob.core.model.FileItem
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
    val isVideo: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLooping: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val isExpanded: Boolean = false,
    val isVisible: Boolean = false,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
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

    private val _state = MutableStateFlow(MediaPlaybackState())
    val state: StateFlow<MediaPlaybackState> = _state.asStateFlow()

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
     */
    fun play(file: FileItem, streamUrl: String, isVideo: Boolean = file.isVideo, token: String? = null) {
        scope.launch {
            stopAndResetPlayer()

            _state.update {
                it.copy(
                    currentFile = file,
                    streamUrl = streamUrl,
                    isVideo = isVideo,
                    isPlaying = false,
                    isBuffering = true,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    videoWidth = 0,
                    videoHeight = 0,
                    isExpanded = isVideo,
                    isVisible = true,
                    errorMessage = null
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
                    if (!token.isNullOrBlank()) {
                        val headers = mapOf("Authorization" to "Bearer $token")
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
                        if (!_state.value.isLooping) {
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
