package com.sonora.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sonora.app.R
import com.sonora.music.SonoraNativeActivity
import com.sonora.music.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SonoraMediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "sonora_music_playback"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.sonora.app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.sonora.app.ACTION_PAUSE"
        const val ACTION_TOGGLE = "com.sonora.app.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.sonora.app.ACTION_NEXT"
        const val ACTION_PREV = "com.sonora.app.ACTION_PREV"
        const val ACTION_STOP = "com.sonora.app.ACTION_STOP"
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

        observerJob = serviceScope.launch {
            combine(audioPlayer.currentSong, audioPlayer.isPlaying) { song, isPlaying ->
                Pair(song, isPlaying)
            }.collect { (song, isPlaying) ->
                updateNotification(song, isPlaying)
            }
        }

        updateNotification(audioPlayer.currentSong.value, audioPlayer.isPlaying.value)
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
        val audioPlayer = SonoraAudioPlayer.getInstance(applicationContext)

        when (intent?.action) {
            ACTION_PLAY -> audioPlayer.resume()
            ACTION_PAUSE -> audioPlayer.pause()
            ACTION_TOGGLE -> audioPlayer.togglePlay()
            ACTION_NEXT -> audioPlayer.nextTrack()
            ACTION_PREV -> audioPlayer.prevTrack()
            ACTION_STOP -> {
                audioPlayer.pause()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }

        updateNotification(audioPlayer.currentSong.value, audioPlayer.isPlaying.value)
        return START_STICKY
    }

    private fun updateNotification(song: Song?, isPlaying: Boolean) {
        val title = song?.title ?: "Sonora Music"
        val artist = song?.artist ?: "Reproductor de Música"
        val album = song?.album ?: ""

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SonoraNativeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, SonoraMediaService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, SonoraMediaService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, SonoraMediaService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, SonoraMediaService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        var coverBitmap: Bitmap? = null
        if (song != null && song.coverUri != null) {
            try {
                contentResolver.openInputStream(song.coverUri)?.use { stream ->
                    coverBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_sonora)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(album.ifEmpty { null })
            .setContentIntent(sessionActivityPendingIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_widget_prev, "Anterior", prevIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                if (isPlaying) "Pausar" else "Reproducir",
                toggleIntent
            )
            .addAction(R.drawable.ic_widget_next, "Siguiente", nextIntent)
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (coverBitmap != null) {
            builder.setLargeIcon(coverBitmap)
        }

        val notification = builder.build()

        try {
            if (isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                val notificationManager = NotificationManagerCompat.from(this)
                if (notificationManager.areNotificationsEnabled()) {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SonoraMediaService", "Error posting notification", e)
        }
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
        observerJob?.cancel()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
