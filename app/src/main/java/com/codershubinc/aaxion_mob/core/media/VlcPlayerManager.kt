package com.codershubinc.aaxion_mob.core.media

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.ui.designsystem.formatTimeMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 * Dedicated manager for video playback sessions with progress tracking and resume capabilities.
 */
class VlcPlayerManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aaxion_video_progress", Context.MODE_PRIVATE)

    private var activeSession: ActiveVideoSession? = null
    private var videoLauncher: androidx.activity.result.ActivityResultLauncher<Intent>? = null

    private val _progressUpdates = MutableStateFlow<Map<String, Long>>(emptyMap())
    val progressUpdates: StateFlow<Map<String, Long>> = _progressUpdates.asStateFlow()

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
        if (!prefs.contains(keyPos)) return null
        val pos = prefs.getLong(keyPos, 0L)
        val dur = prefs.getLong("${rawPath.hashCode()}_dur", 0L)
        val ts = prefs.getLong("${rawPath.hashCode()}_ts", 0L)
        return VideoProgress(rawPath = rawPath, positionMs = pos, durationMs = dur, lastUpdatedTs = ts)
    }

    fun hasResumePosition(rawPath: String): Boolean {
        val progress = getProgress(rawPath) ?: return false
        return progress.positionMs >= 5000L
    }

    fun saveProgress(rawPath: String, positionMs: Long, durationMs: Long = 0L) {
        val safePos = positionMs.coerceAtLeast(0L)
        prefs.edit()
            .putLong("${rawPath.hashCode()}_pos", safePos)
            .putLong("${rawPath.hashCode()}_dur", durationMs)
            .putLong("${rawPath.hashCode()}_ts", System.currentTimeMillis())
            .apply()

        _progressUpdates.value = _progressUpdates.value + (rawPath to safePos)
    }

    fun clearProgress(rawPath: String) {
        prefs.edit()
            .remove("${rawPath.hashCode()}_pos")
            .remove("${rawPath.hashCode()}_dur")
            .remove("${rawPath.hashCode()}_ts")
            .apply()

        _progressUpdates.value = _progressUpdates.value - rawPath
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
        resume: Boolean = false
    ) {
        val rawPath = file.rawPath
        val startPositionMs = if (resume) {
            getProgress(rawPath)?.positionMs ?: 0L
        } else {
            0L
        }

        activeSession = ActiveVideoSession(
            rawPath = rawPath,
            startPositionMs = startPositionMs,
            startTimestamp = System.currentTimeMillis()
        )

        val uri = Uri.parse(streamUrl)
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            val launcher = videoLauncher
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
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                val chooser = Intent.createChooser(genericIntent, "Play video with...")
                val launcher = videoLauncher
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

    fun handleActivityResult(positionMs: Long, durationMs: Long = 0L) {
        val session = activeSession ?: return
        if (positionMs > 0L) {
            saveProgress(session.rawPath, positionMs, durationMs)
        } else {
            val elapsedMs = System.currentTimeMillis() - session.startTimestamp
            if (elapsedMs > 3000L) {
                val estimatedPosition = session.startPositionMs + elapsedMs
                saveProgress(session.rawPath, estimatedPosition, durationMs)
            }
        }
        activeSession = null
    }

    fun onActivityResumed() {
        val session = activeSession ?: return
        val elapsedMs = System.currentTimeMillis() - session.startTimestamp
        if (elapsedMs > 3000L) {
            val estimatedPosition = session.startPositionMs + elapsedMs
            saveProgress(session.rawPath, estimatedPosition)
        }
        activeSession = null
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
