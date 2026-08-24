package com.sonora.music.service

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sonora.music.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SonoraAudioPlayer(private val context: Context) {

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Auto-handle AudioFocus
            )
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                shuffleModeEnabled = false
                addListener(playerListener)
            }
    }

    val equalizerManager = SonoraEqualizerManager()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist = _playlist.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _sleepTimerSecondsLeft = MutableStateFlow<Int?>(null)
    val sleepTimerSecondsLeft = _sleepTimerSecondsLeft.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) startPositionTracking() else stopPositionTracking()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            equalizerManager.attachAudioSession(audioSessionId)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _durationMs.value = player.duration.coerceAtLeast(0L)
                equalizerManager.attachAudioSession(player.audioSessionId)
            } else if (playbackState == Player.STATE_ENDED) {
                nextTrack()
            }
        }


        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val currentIdx = player.currentMediaItemIndex
            val list = _playlist.value
            if (currentIdx in list.indices) {
                _currentSong.value = list[currentIdx]
            }
        }
    }

    fun playSong(song: Song, newPlaylist: List<Song> = emptyList()) {
        if (newPlaylist.isNotEmpty()) {
            _playlist.value = newPlaylist
            val mediaItems = newPlaylist.map { s ->
                MediaItem.Builder()
                    .setUri(s.contentUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setAlbumTitle(s.album)
                            .setArtworkUri(s.coverUri)
                            .build()
                    )
                    .build()
            }
            player.setMediaItems(mediaItems)
            val index = newPlaylist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            player.seekTo(index, 0L)
        } else {
            _playlist.value = listOf(song)
            val item = MediaItem.Builder()
                .setUri(song.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.coverUri)
                        .build()
                )
                .build()
            player.setMediaItem(item)
        }

        _currentSong.value = song
        player.prepare()
        player.play()
    }

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun nextTrack() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (_playlist.value.isNotEmpty()) {
            player.seekTo(0, 0L)
        }
    }

    fun prevTrack() {
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else if (_playlist.value.isNotEmpty()) {
            player.seekTo(_playlist.value.size - 1, 0L)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun toggleShuffle() {
        val newShuffle = !player.shuffleModeEnabled
        player.shuffleModeEnabled = newShuffle
        _isShuffle.value = newShuffle
    }

    fun toggleRepeat() {
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    private fun startPositionTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                delay(200)
            }
        }
    }

    private fun stopPositionTracking() {
        progressJob?.cancel()
        progressJob = null
    }


    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        var seconds = minutes * 60
        _sleepTimerSecondsLeft.value = seconds
        sleepTimerJob = scope.launch {
            while (seconds > 0 && isActive) {
                delay(1000)
                seconds--
                _sleepTimerSecondsLeft.value = seconds
            }
            if (seconds <= 0) {
                player.pause()
                _sleepTimerSecondsLeft.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerSecondsLeft.value = null
    }

    fun release() {
        cancelSleepTimer()
        stopPositionTracking()
        equalizerManager.release()
        player.removeListener(playerListener)
        player.release()
    }
}

