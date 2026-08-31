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
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sonora.app.R
import com.sonora.music.SonoraNativeActivity
import com.sonora.music.data.model.Song
import com.sonora.music.widget.SonoraWidgetProvider
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SonoraMediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var customPlayer: SonoraCustomPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null

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

    class SonoraCustomPlayer(
        private val audioPlayer: SonoraAudioPlayer
    ) : SimpleBasePlayer(Looper.getMainLooper()) {

        fun notifyStateChanged() {
            invalidateState()
        }

        override fun getState(): State {
            val currentSong = audioPlayer.currentSong.value
            val isPlaying = audioPlayer.isPlaying.value
            val pos = audioPlayer.currentPositionMs.value
            val dur = audioPlayer.durationMs.value

            val availableCommands = Player.Commands.Builder()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_GET_METADATA)
                .add(Player.COMMAND_GET_TIMELINE)
                .build()

            val stateBuilder = State.Builder()
                .setAvailableCommands(availableCommands)
                .setPlayWhenReady(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setPlaybackState(if (currentSong != null) Player.STATE_READY else Player.STATE_IDLE)

            if (currentSong != null) {
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(currentSong.title)
                    .setArtist(currentSong.artist)
                    .setAlbumTitle(currentSong.album)
                    .setArtworkUri(currentSong.coverUri)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(currentSong.id.toString())
                    .setUri(currentSong.contentUri)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                val mediaItemData = MediaItemData.Builder(mediaItem.mediaId)
                    .setMediaItem(mediaItem)
                    .setMediaMetadata(mediaMetadata)
                    .setDurationUs(if (dur > 0) dur * 1000L else androidx.media3.common.C.TIME_UNSET)
                    .build()

                stateBuilder
                    .setPlaylist(listOf(mediaItemData))
                    .setCurrentMediaItemIndex(0)
                    .setContentPositionMs(pos)
                    .setPlaylistMetadata(mediaMetadata)
            }

            return stateBuilder.build()
        }

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
            if (playWhenReady) {
                audioPlayer.resume()
            } else {
                audioPlayer.pause()
            }
            invalidateState()
            return Futures.immediateVoidFuture()
        }

        override fun handleSeek(
            mediaItemIndex: Int,
            positionMs: Long,
            seekCommand: Int
        ): ListenableFuture<*> {
            when (seekCommand) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    audioPlayer.nextTrack(isManualSkip = true)
                }
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    audioPlayer.prevTrack()
                }
                else -> {
                    if (positionMs != androidx.media3.common.C.TIME_UNSET) {
                        audioPlayer.seekTo(positionMs)
                    }
                }
            }
            invalidateState()
            return Futures.immediateVoidFuture()
        }

        override fun handleStop(): ListenableFuture<*> {
            audioPlayer.pause()
            invalidateState()
            return Futures.immediateVoidFuture()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioPlayer = SonoraAudioPlayer.getInstance(applicationContext)
        val player = SonoraCustomPlayer(audioPlayer)
        customPlayer = player
        audioPlayer.onStateInvalidated = {
            customPlayer?.notifyStateChanged()
        }

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
                        customPlayer?.notifyStateChanged()
                        return SessionResult.RESULT_SUCCESS
                    }
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                        audioPlayer.prevTrack()
                        customPlayer?.notifyStateChanged()
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
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> audioPlayer.nextTrack(isManualSkip = true)
                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> audioPlayer.prevTrack()
                    }
                    customPlayer?.notifyStateChanged()
                    return true
                }
                return super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(callback)
            .setId("SonoraMediaSession")
            .build()

        observerJob = serviceScope.launch {
            combine(audioPlayer.currentSong, audioPlayer.isPlaying) { song, isPlaying ->
                Pair(song, isPlaying)
            }.collect { (song, isPlaying) ->
                customPlayer?.notifyStateChanged()
                updateNotification(song, isPlaying)
                SonoraWidgetProvider.updateAllWidgets(applicationContext, song, isPlaying)
            }
        }

        serviceScope.launch {
            updateNotification(audioPlayer.currentSong.value, audioPlayer.isPlaying.value)
            SonoraWidgetProvider.updateAllWidgets(applicationContext, audioPlayer.currentSong.value, audioPlayer.isPlaying.value)
        }
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
            ACTION_NEXT -> audioPlayer.nextTrack(isManualSkip = true)
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

        customPlayer?.notifyStateChanged()
        serviceScope.launch {
            updateNotification(audioPlayer.currentSong.value, audioPlayer.isPlaying.value)
            SonoraWidgetProvider.updateAllWidgets(applicationContext, audioPlayer.currentSong.value, audioPlayer.isPlaying.value)
        }
        return START_STICKY
    }

    private suspend fun loadDownsampledArtwork(uri: android.net.Uri?, targetSize: Int = 256): Bitmap? {
        if (uri == null) return null
        return withContext(Dispatchers.IO) {
            try {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return@withContext null

                var sampleSize = 1
                val halfHeight = boundsOpts.outHeight / 2
                val halfWidth = boundsOpts.outWidth / 2
                while ((halfHeight / sampleSize) >= targetSize && (halfWidth / sampleSize) >= targetSize) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun updateNotification(song: Song?, isPlaying: Boolean) {
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

        val coverBitmap: Bitmap? = if (song != null && song.coverUri != null) {
            loadDownsampledArtwork(song.coverUri, targetSize = 256)
        } else null

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
            val notificationManager = NotificationManagerCompat.from(this)
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }
            }
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
        val audioPlayer = SonoraAudioPlayer.getInstance(applicationContext)
        audioPlayer.onStateInvalidated = null
        observerJob?.cancel()
        mediaSession?.release()
        mediaSession = null
        customPlayer?.release()
        customPlayer = null
    }
}
