package com.codershubinc.aaxion_mob.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast

object DownloadHelper {

    private var isReceiverRegistered = false

    /**
     * Enqueues a file download using Android's native DownloadManager.
     * Shows system progress notifications and saves to standard Downloads directory.
     */
    fun enqueueDownload(
        context: Context,
        downloadUrl: String,
        fileName: String,
        token: String? = null
    ): Long {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle(fileName)
                setDescription("Downloading from Aaxion Server...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                if (!token.isNullOrBlank()) {
                    addRequestHeader("Authorization", "Bearer $token")
                }
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)

            Toast.makeText(context, "Download started: $fileName", Toast.LENGTH_SHORT).show()
            registerCompletionReceiverIfNeeded(context.applicationContext)
            return downloadId
        } catch (e: Exception) {
            // Fallback to browser download intent if DownloadManager fails or is disabled
            Toast.makeText(context, "Starting via browser...", Toast.LENGTH_SHORT).show()
            IntentUtils.launchDownloadIntent(context, downloadUrl)
            return -1L
        }
    }

    private fun registerCompletionReceiverIfNeeded(context: Context) {
        if (isReceiverRegistered) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id != -1L && c != null) {
                        Toast.makeText(c, "Download complete", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        try {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            // Receiver registration failed or already registered
        }
    }
}
