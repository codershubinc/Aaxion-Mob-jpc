package com.codershubinc.aaxion_mob.core.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles action clicks from the system Media Notification shade.
 */
class MediaActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = MediaPlayerManager.getInstance(context)
        when (intent.action) {
            AaxionMediaSessionManager.ACTION_PLAY_PAUSE -> manager.togglePlayPause()
            AaxionMediaSessionManager.ACTION_REWIND -> manager.seekRelative(-10000L)
            AaxionMediaSessionManager.ACTION_FORWARD -> manager.seekRelative(10000L)
            AaxionMediaSessionManager.ACTION_STOP -> manager.close()
        }
    }
}
