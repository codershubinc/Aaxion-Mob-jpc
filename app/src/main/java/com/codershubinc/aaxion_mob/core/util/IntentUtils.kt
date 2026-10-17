package com.codershubinc.aaxion_mob.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale

object IntentUtils {

    /**
     * Attempts to stream a video or audio URL directly to VLC (org.videolan.vlc).
     * Falls back to general media player chooser or browser if VLC is not installed.
     */
    fun launchStreamIntent(context: Context, streamUrl: String, isVideo: Boolean, title: String? = null) {
        val mimeType = if (isVideo) "video/*" else "audio/*"
        val uri = Uri.parse(streamUrl)
        val streamTitle = title ?: uri.lastPathSegment ?: "Media Stream"

        // 1. Try launching VLC directly
        val vlcIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            setPackage("org.videolan.vlc")
            putExtra("title", streamTitle)
            putExtra("extra_title", streamTitle)
            putExtra(Intent.EXTRA_TITLE, streamTitle)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(vlcIntent)
            return
        } catch (e: Exception) {
            // VLC package not found or unable to handle directly, fall back to chooser / general player
        }

        // 2. Try generic media player intent (allowing system app chooser)
        try {
            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                putExtra("title", streamTitle)
                putExtra("extra_title", streamTitle)
                putExtra(Intent.EXTRA_TITLE, streamTitle)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(genericIntent, "Play with...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            // 3. Fallback to browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "Could not open stream", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Enqueues a download using system DownloadManager with notifications,
     * falling back to browser intent if needed.
     */
    fun launchDownloadIntent(
        context: Context,
        downloadUrl: String,
        fileName: String? = null,
        token: String? = null
    ) {
        val targetName = fileName ?: Uri.parse(downloadUrl).lastPathSegment ?: "download"
        DownloadHelper.enqueueDownload(context, downloadUrl, targetName, token)
    }

    /**
     * Launches a system intent to view a URL or file with an external application.
     */
    fun launchViewIntent(context: Context, url: String, mimeType: String = "*/*") {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "Could not open document", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Formats bytes into human-readable B, KB, MB, GB, TB, PB.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val unitIndex = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, unitIndex.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }
}

fun Long.toReadableSize(): String = IntentUtils.formatFileSize(this)
