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
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
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
    private var playerObserverJob: Job? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "sonora_playback_live_v4"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.sonora.app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.sonora.app.ACTION_PAUSE"
        const val ACTION_TOGGLE = "com.sonora.app.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.sonora.app.ACTION_NEXT"
        const val ACTION_PREV = "com.sonora.app.ACTION_PREV"
        const val ACTION_STOP = "com.sonora.app.ACTION_STOP"
    }

    private class SonoraForwardingPlayer(
        private val delegatePlayer: Player,
        private val audioPlayer: SonoraAudioPlayer
    ) : ForwardingPlayer(delegatePlayer) {

        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands().buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return when (command) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
                else -> super.isCommandAvailable(command)
            }
        }

        override fun seekToNext() {
            audioPlayer.nextTrack()
        }

        override fun seekToNextMediaItem() {
            audioPlayer.nextTrack()
        }

        override fun seekToPrevious() {
            audioPlayer.prevTrack()
        }

        override fun seekToPreviousMediaItem() {
            audioPlayer.prevTrack()
        }

        override fun play() {
            audioPlayer.resume()
        }

        override fun pause() {
            audioPlayer.pause()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioPlayer = SonoraAudioPlayer.getInstance(applicationContext)

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SonoraNativeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ConnectionResult {
                val availableCommands = ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .build()
                return ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(availableCommands)
                    .build()
            }

            override fun onPlayerCommandRequest(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                playerCommand: Int
            ): Int {
                when (playerCommand) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                        audioPlayer.nextTrack(isManualSkip = true)
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                        audioPlayer.prevTrack()
                        return SessionResult.RESULT_SUCCESS
                    }
                }
                return super.onPlayerCommandRequest(session, controller, playerCommand)
            }

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                val keyEvent = intent.getParcelableExtra<android.view.KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent != null && keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> audioPlayer.resume()
                        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> audioPlayer.pause()
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        android.view.KeyEvent.KEYCODE_HEADSETHOOK -> audioPlayer.togglePlay()
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> audioPlayer.nextTrack()
                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> audioPlayer.prevTrack()
                    }
                    return true
                }
                return super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

        val initialForwardingPlayer = SonoraForwardingPlayer(audioPlayer.exoPlayer, audioPlayer)

        mediaSession = MediaSession.Builder(this, initialForwardingPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(callback)
            .setId("SonoraMediaSession")
            .build()

        playerObserverJob = serviceScope.launch {
            audioPlayer.activePlayerFlow.collect { activeExo ->
                try {
                    val wrapped = SonoraForwardingPlayer(activeExo, audioPlayer)
                    mediaSession?.setPlayer(wrapped)
                } catch (_: Throwable) {}
            }
        }

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
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Controles de reproducción multimedia de Sonora"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
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
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_widget_prev, "Anterior", prevIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                if (isPlaying) "Pausar" else "Reproducir",
                toggleIntent
            )
            .addAction(R.drawable.ic_widget_next, "Siguiente", nextIntent)

        val mediaStyle = MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)

        try {
            mediaSession?.sessionCompatToken?.let { token ->
                mediaStyle.setMediaSession(token as android.support.v4.media.session.MediaSessionCompat.Token)
            }
        } catch (_: Exception) {}

        builder.setStyle(mediaStyle)

        try {
            mediaSession?.platformToken?.let { platformToken ->
                if (platformToken is android.os.Parcelable) {
                    builder.extras.putParcelable("android.mediaSession", platformToken)
                    builder.extras.putParcelable(NotificationCompat.EXTRA_MEDIA_SESSION, platformToken)
                }
            }
        } catch (_: Exception) {}

        builder.extras.putString("android.substName", "Sonora")

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
            val notificationManager = NotificationManagerCompat.from(this)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("SonoraMediaService", "Error posting notification", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        super.onDestroy()
        playerObserverJob?.cancel()
        observerJob?.cancel()
        mediaSession?.release()
        mediaSession = null
    }
}
