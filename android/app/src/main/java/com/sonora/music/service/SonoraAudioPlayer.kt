package com.sonora.music.service

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
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
    private var crossfadeJob: Job? = null

    private var crossfadeSeconds: Int = 0

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
            _isPlaying.value = player.isPlaying
            when (playbackState) {
                Player.STATE_READY -> {
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                    equalizerManager.attachAudioSession(player.audioSessionId)
                }
                Player.STATE_ENDED -> {
                    nextTrack()
                }
                Player.STATE_IDLE -> {
                    // Idle state
                }
                Player.STATE_BUFFERING -> {
                    // Buffering
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isPlaying.value = false
            stopPositionTracking()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val currentIdx = player.currentMediaItemIndex
            val list = _playlist.value
            if (currentIdx in list.indices) {
                _currentSong.value = list[currentIdx]
                if (crossfadeSeconds > 0) {
                    fadeInVolume(crossfadeSeconds)
                } else {
                    player.volume = 1.0f
                }
            }
        }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        crossfadeSeconds = seconds.coerceIn(0, 12)
    }

    fun playSong(song: Song, newPlaylist: List<Song> = emptyList()) {
        val listToUse = if (newPlaylist.isNotEmpty()) newPlaylist else listOf(song)
        _playlist.value = listToUse

        val mediaItems = listToUse.map { s ->
            MediaItem.Builder()
                .setUri(s.contentUri)
                .setMediaId(s.id.toString())
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

        val targetIndex = listToUse.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        player.stop()
        player.clearMediaItems()
        player.setMediaItems(mediaItems, targetIndex, 0L)
        _currentSong.value = song
        player.prepare()
        player.playWhenReady = true
        player.play()
        _isPlaying.value = true

        if (crossfadeSeconds > 0) {
            fadeInVolume(crossfadeSeconds)
        } else {
            player.volume = 1.0f
        }
    }

    fun play() {
        resume()
    }

    fun resume() {
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        } else if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0, 0L)
        }
        player.playWhenReady = true
        player.play()
        _isPlaying.value = true
    }

    fun pause() {
        player.pause()
        _isPlaying.value = false
    }

    fun togglePlay() {
        if (player.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun nextTrack() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.playWhenReady = true
            player.play()
        } else if (_playlist.value.isNotEmpty()) {
            player.seekTo(0, 0L)
            player.playWhenReady = true
            player.play()
        }
    }

    fun prevTrack() {
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.playWhenReady = true
            player.play()
        } else if (_playlist.value.isNotEmpty()) {
            player.seekTo(_playlist.value.size - 1, 0L)
            player.playWhenReady = true
            player.play()
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

    private fun fadeInVolume(seconds: Int) {
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            val totalSteps = (seconds * 10).coerceAtLeast(5)
            val stepDelay = (seconds * 1000L) / totalSteps
            player.volume = 0f
            for (i in 1..totalSteps) {
                delay(stepDelay)
                player.volume = (i.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
            }
            player.volume = 1.0f
        }
    }

    private fun startPositionTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L)
                _currentPositionMs.value = pos
                if (dur > 0L) _durationMs.value = dur

                // Crossfade fade-out when near track end
                if (crossfadeSeconds > 0 && dur > crossfadeSeconds * 2000L) {
                    val remainingMs = dur - pos
                    val crossfadeMs = crossfadeSeconds * 1000L
                    if (remainingMs in 0..crossfadeMs) {
                        val factor = (remainingMs.toFloat() / crossfadeMs.toFloat()).coerceIn(0.05f, 1.0f)
                        player.volume = factor
                    }
                }

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
                pause()
                _sleepTimerSecondsLeft.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerSecondsLeft.value = null
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun release() {
        crossfadeJob?.cancel()
        cancelSleepTimer()
        stopPositionTracking()
        equalizerManager.release()
        player.removeListener(playerListener)
        player.release()
    }
}
