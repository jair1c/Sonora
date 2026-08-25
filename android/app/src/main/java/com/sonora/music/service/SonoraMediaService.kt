package com.sonora.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sonora.music.SonoraNativeActivity

class SonoraMediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "sonora_music_playback"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioPlayer = SonoraAudioPlayer.getInstance(applicationContext)
        val player = audioPlayer.exoPlayer

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SonoraNativeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setId("SonoraMediaSession")
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setNotificationId(NOTIFICATION_ID)
                .build()
        )

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                mediaSession?.let { session ->
                    onUpdateNotification(session, startInForegroundRequired = isPlaying)
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                mediaSession?.let { session ->
                    onUpdateNotification(session, startInForegroundRequired = player.isPlaying)
                }
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Sonora Reproducción",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción multimedia de Sonora"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val player = mediaSession?.player
        if (player != null && player.isPlaying) {
            mediaSession?.let { session ->
                onUpdateNotification(session, startInForegroundRequired = true)
            }
        }
        return START_STICKY
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
