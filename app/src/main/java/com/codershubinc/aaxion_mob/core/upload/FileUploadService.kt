package com.codershubinc.aaxion_mob.core.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.codershubinc.aaxion_mob.MainActivity
import com.codershubinc.aaxion_mob.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service that handles file upload progress and presents a custom styled notification.
 */
class FileUploadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var notificationManager: NotificationManager? = null
    private var isForeground = false

    companion object {
        const val CHANNEL_ID = "aaxion_upload_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_UPLOAD = "com.codershubinc.aaxion_mob.ACTION_START_UPLOAD"
        const val ACTION_CANCEL_TASK = "com.codershubinc.aaxion_mob.ACTION_CANCEL_TASK"
        const val ACTION_CANCEL_ALL = "com.codershubinc.aaxion_mob.ACTION_CANCEL_ALL"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        serviceScope.launch {
            UploadManager.getInstance(applicationContext).tasks.collectLatest { tasks ->
                val activeTasks = tasks.filter { it.status == UploadStatus.QUEUED || it.status == UploadStatus.UPLOADING }
                if (activeTasks.isNotEmpty()) {
                    updateProgressNotification(activeTasks, tasks.size)
                } else if (tasks.isNotEmpty()) {
                    showFinishedNotification(tasks)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (!taskId.isNullOrBlank()) {
                    UploadManager.getInstance(applicationContext).cancelUpload(taskId)
                }
            }
            ACTION_CANCEL_ALL -> {
                UploadManager.getInstance(applicationContext).cancelAll()
            }
            ACTION_START_UPLOAD -> {
                val tasks = UploadManager.getInstance(applicationContext).tasks.value
                val activeTasks = tasks.filter { it.status == UploadStatus.QUEUED || it.status == UploadStatus.UPLOADING }
                val initialNotification = buildCustomNotification(activeTasks, tasks.size)
                if (!isForeground) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            initialNotification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, initialNotification)
                    }
                    isForeground = true
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun updateProgressNotification(activeTasks: List<UploadTask>, totalCount: Int) {
        val notification = buildCustomNotification(activeTasks, totalCount)
        if (!isForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForeground = true
        } else {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildCustomNotification(activeTasks: List<UploadTask>, totalCount: Int): Notification {
        val currentTask = activeTasks.firstOrNull { it.status == UploadStatus.UPLOADING } ?: activeTasks.firstOrNull()
        val currentFileName = currentTask?.name ?: "Uploading files..."
        val currentProgress = currentTask?.progressPercent ?: 0
        val speedStr = currentTask?.formattedSpeed ?: "-- KB/s"

        val completedCount = totalCount - activeTasks.size
        val infoText = "$speedStr • File ${completedCount + 1} of $totalCount"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, FileUploadService::class.java).apply {
            action = if (currentTask != null) ACTION_CANCEL_TASK else ACTION_CANCEL_ALL
            if (currentTask != null) putExtra(EXTRA_TASK_ID, currentTask.id)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteViews = RemoteViews(packageName, R.layout.notification_upload_custom).apply {
            setTextViewText(R.id.notification_title, currentFileName)
            setTextViewText(R.id.notification_info_text, infoText)
            setTextViewText(R.id.notification_percentage_text, "$currentProgress%")
            setProgressBar(R.id.notification_progress_bar, 100, currentProgress, false)
            setOnClickPendingIntent(R.id.notification_cancel_button, cancelPendingIntent)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_upload)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showFinishedNotification(tasks: List<UploadTask>) {
        val successCount = tasks.count { it.status == UploadStatus.SUCCESS }
        val failedCount = tasks.count { it.status == UploadStatus.FAILED }
        val totalCount = tasks.size

        val title = if (failedCount == 0) {
            "Upload complete"
        } else {
            "Upload finished with errors"
        }
        val content = if (failedCount == 0) {
            "$successCount of $totalCount files uploaded successfully"
        } else {
            "$successCount uploaded, $failedCount failed"
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_upload)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager?.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aaxion File Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for ongoing background file uploads"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
