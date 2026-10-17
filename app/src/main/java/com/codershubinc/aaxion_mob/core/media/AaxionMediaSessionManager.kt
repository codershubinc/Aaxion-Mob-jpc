package com.codershubinc.aaxion_mob.core.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.codershubinc.aaxion_mob.MainActivity
import com.codershubinc.aaxion_mob.R
import kotlinx.coroutines.*

/**
 * Manages system-level MediaSession and native Media Notification with transport controls,
 * lockscreen integration, and lockscreen/notification album artwork.
 */
class AaxionMediaSessionManager(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var currentArtworkUrl: String? = null
    private var currentArtworkBitmap: Bitmap? = null
    private var artworkJob: Job? = null

    val mediaSession: MediaSession = MediaSession(appContext, "AaxionMediaSession").apply {
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                MediaPlayerManager.getInstance(appContext).togglePlayPause()
            }

            override fun onPause() {
                MediaPlayerManager.getInstance(appContext).togglePlayPause()
            }

            override fun onSkipToNext() {
                val mgr = MediaPlayerManager.getInstance(appContext)
                if (mgr.hasNextTrack()) {
                    mgr.playNext()
                } else {
                    mgr.seekRelative(10000L)
                }
            }

            override fun onSkipToPrevious() {
                val mgr = MediaPlayerManager.getInstance(appContext)
                if (mgr.hasPreviousTrack()) {
                    mgr.playPrevious()
                } else {
                    mgr.seekRelative(-10000L)
                }
            }

            override fun onSeekTo(pos: Long) {
                MediaPlayerManager.getInstance(appContext).seekTo(pos)
            }

            override fun onFastForward() {
                MediaPlayerManager.getInstance(appContext).seekRelative(10000L)
            }

            override fun onRewind() {
                MediaPlayerManager.getInstance(appContext).seekRelative(-10000L)
            }

            override fun onStop() {
                MediaPlayerManager.getInstance(appContext).close()
            }
        })
        isActive = false
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Aaxion media playback controls and background streaming status"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun update(state: MediaPlaybackState) {
        if (!state.isVisible || state.currentFile == null) {
            clear()
            return
        }

        if (!mediaSession.isActive) {
            mediaSession.isActive = true
        }

        // 1. Update PlaybackState for lockscreen & bluetooth controllers
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_FAST_FORWARD or
            PlaybackState.ACTION_REWIND or
            PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_STOP

        val pState = PlaybackState.Builder()
            .setActions(actions)
            .setState(
                if (state.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                state.currentPositionMs,
                state.playbackSpeed
            )
            .build()
        mediaSession.setPlaybackState(pState)

        // 2. Artwork loading
        if (state.artworkUrl != currentArtworkUrl) {
            currentArtworkUrl = state.artworkUrl
            artworkJob?.cancel()
            currentArtworkBitmap = null

            if (!state.artworkUrl.isNullOrBlank()) {
                artworkJob = scope.launch(Dispatchers.IO) {
                    try {
                        val request = ImageRequest.Builder(appContext)
                            .data(state.artworkUrl)
                            .allowHardware(false) // Non-hardware bitmap needed for notifications & metadata
                            .size(512, 512)
                            .build()
                        val result = Coil.imageLoader(appContext).execute(request)
                        if (result is SuccessResult) {
                            val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                            withContext(Dispatchers.Main) {
                                currentArtworkBitmap = bmp
                                pushMetadata(state, bmp)
                                postNotification(state, bmp)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore artwork loading errors
                    }
                }
            }
        }

        pushMetadata(state, currentArtworkBitmap)
        postNotification(state, currentArtworkBitmap)
    }

    private fun pushMetadata(state: MediaPlaybackState, artworkBitmap: Bitmap?) {
        val fileName = state.displayTitle ?: state.currentFile?.name ?: "Media"
        val subtitle = state.displayArtist ?: if (state.isVideo) "Aaxion Video" else "Aaxion Audio"
        val album = state.displayAlbum ?: "Aaxion Library"

        val metaBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, fileName)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, subtitle)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)

        if (artworkBitmap != null) {
            metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artworkBitmap)
            metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, artworkBitmap)
        }

        try {
            mediaSession.setMetadata(metaBuilder.build())
        } catch (e: Exception) {
            // ignore metadata build errors
        }
    }

    private fun postNotification(state: MediaPlaybackState, artworkBitmap: Bitmap?) {
        val fileName = state.displayTitle ?: state.currentFile?.name ?: "Media"
        val subtitle = state.displayArtist ?: if (state.isVideo) "Aaxion Video" else "Aaxion Audio"

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            appContext,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rewIntent = Intent(ACTION_REWIND).setPackage(appContext.packageName)
        val rewPending = PendingIntent.getBroadcast(
            appContext,
            1,
            rewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(ACTION_PLAY_PAUSE).setPackage(appContext.packageName)
        val playPausePending = PendingIntent.getBroadcast(
            appContext,
            2,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fwdIntent = Intent(ACTION_FORWARD).setPackage(appContext.packageName)
        val fwdPending = PendingIntent.getBroadcast(
            appContext,
            3,
            fwdIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(ACTION_STOP).setPackage(appContext.packageName)
        val stopPending = PendingIntent.getBroadcast(
            appContext,
            4,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(appContext, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(appContext)
        }

        notifBuilder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(fileName)
            .setContentText(subtitle)
            .setContentIntent(openPending)
            .setDeleteIntent(stopPending)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(state.isPlaying)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(appContext, android.R.drawable.ic_media_rew),
                    "-10s",
                    rewPending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(
                        appContext,
                        if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    ),
                    if (state.isPlaying) "Pause" else "Play",
                    playPausePending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(appContext, android.R.drawable.ic_media_ff),
                    "+10s",
                    fwdPending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(appContext, android.R.drawable.ic_menu_close_clear_cancel),
                    "Close",
                    stopPending
                ).build()
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (artworkBitmap != null) {
            notifBuilder.setLargeIcon(artworkBitmap)
        }

        try {
            notificationManager.notify(NOTIFICATION_ID, notifBuilder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            // ignore notification dispatch issues
        }
    }

    fun clear() {
        artworkJob?.cancel()
        currentArtworkUrl = null
        currentArtworkBitmap = null
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // ignore
        }
        try {
            if (mediaSession.isActive) {
                mediaSession.isActive = false
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        clear()
        scope.cancel()
        try {
            mediaSession.release()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        const val CHANNEL_ID = "aaxion_media_playback"
        const val NOTIFICATION_ID = 2001

        const val ACTION_PLAY_PAUSE = "com.codershubinc.aaxion_mob.ACTION_PLAY_PAUSE"
        const val ACTION_REWIND = "com.codershubinc.aaxion_mob.ACTION_REWIND"
        const val ACTION_FORWARD = "com.codershubinc.aaxion_mob.ACTION_FORWARD"
        const val ACTION_STOP = "com.codershubinc.aaxion_mob.ACTION_STOP"
    }
}
