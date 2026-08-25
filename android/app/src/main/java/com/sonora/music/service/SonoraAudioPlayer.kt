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

import android.content.Intent
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import java.io.File

class SonoraAudioPlayer(private val context: Context) {

    companion object {
        @Volatile
        private var instance: SonoraAudioPlayer? = null

        fun getInstance(context: Context): SonoraAudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: SonoraAudioPlayer(context.applicationContext).also { instance = it }
            }
        }
    }

    private val player: ExoPlayer by lazy {
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setFlacExtractorFlags(0)
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Auto-handle AudioFocus
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                shuffleModeEnabled = false
                addListener(playerListener)
            }
    }

    val exoPlayer: ExoPlayer get() = player

    val equalizerManager = SonoraEqualizerManager()
    val visualizerManager = SonoraVisualizerManager()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var crossfadeJob: Job? = null

    private var crossfadeSeconds: Int = 0

    private var lastCountedSongId: Long = -1L
    private var lastCountedTimestamp: Long = 0L

    private fun trackSongPlay(song: Song) {
        val now = System.currentTimeMillis()
        if (song.id != lastCountedSongId || (now - lastCountedTimestamp) > 10000L) {
            lastCountedSongId = song.id
            lastCountedTimestamp = now
            sonoraPrefs.incrementPlayCount(song.id)
        }
    }

    private fun ensureMediaServiceStarted() {
        try {
            val serviceIntent = Intent(context, SonoraMediaService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("SonoraAudioPlayer", "Could not start SonoraMediaService", e)
        }
    }

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

    private val _sleepTimerFinishSong = MutableStateFlow(false)
    val sleepTimerFinishSong = _sleepTimerFinishSong.asStateFlow()

    private var waitForSongEndJob: Job? = null

    private val mediaRepo = com.sonora.music.data.repository.MediaStoreRepository(context)
    private val sonoraPrefs = com.sonora.music.data.local.SonoraPreferences(context)

    init {
        scope.launch {
            kotlinx.coroutines.flow.combine(_currentSong, _isPlaying) { song, isPlaying ->
                Pair(song, isPlaying)
            }.collect { (song, isPlaying) ->
                com.sonora.music.widget.SonoraWidgetProvider.updateAllWidgets(context, song, isPlaying)
            }
        }
    }

    private fun buildMediaItem(s: Song, useFileFallback: Boolean = false): MediaItem {
        val uri = if (useFileFallback && s.filePath.isNotEmpty() && File(s.filePath).exists()) {
            android.net.Uri.fromFile(File(s.filePath))
        } else {
            s.contentUri
        }
        return MediaItem.Builder()
            .setUri(uri)
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

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) startPositionTracking() else stopPositionTracking()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            equalizerManager.attachAudioSession(audioSessionId)
            equalizerManager.setPreAmp(sonoraPrefs.getPreAmpGain())
            equalizerManager.setBassBoost(sonoraPrefs.getBassBoost().toShort())
            equalizerManager.setAutoVolumeLeveling(sonoraPrefs.isAutoVolumeLeveling())
            visualizerManager.attachAudioSession(audioSessionId)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isPlaying.value = player.isPlaying
            when (playbackState) {
                Player.STATE_READY -> {
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                    equalizerManager.attachAudioSession(player.audioSessionId)
                    equalizerManager.setPreAmp(sonoraPrefs.getPreAmpGain())
                    equalizerManager.setBassBoost(sonoraPrefs.getBassBoost().toShort())
                    equalizerManager.setAutoVolumeLeveling(sonoraPrefs.isAutoVolumeLeveling())
                    visualizerManager.attachAudioSession(player.audioSessionId)
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
            android.util.Log.e("SonoraAudioPlayer", "Playback error on ${_currentSong.value?.title}: ${error.errorCodeName} (${error.errorCode})", error)
            val current = _currentSong.value
            if (current != null && current.filePath.isNotEmpty() && File(current.filePath).exists()) {
                android.util.Log.d("SonoraAudioPlayer", "Retrying with direct File URI: ${current.filePath}")
                val fileItem = buildMediaItem(current, useFileFallback = true)
                player.setMediaItem(fileItem)
                player.prepare()
                player.playWhenReady = true
                player.play()
                _isPlaying.value = true
            } else {
                _isPlaying.value = false
                stopPositionTracking()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val currentIdx = player.currentMediaItemIndex
            val list = _playlist.value
            if (currentIdx in list.indices) {
                val nextSong = list[currentIdx]
                _currentSong.value = nextSong
                trackSongPlay(nextSong)
                scope.launch(Dispatchers.IO) {
                    val lyrics = mediaRepo.getLyricsForSong(nextSong)
                    if (lyrics.isNotEmpty() && _currentSong.value?.id == nextSong.id) {
                        _currentSong.value = _currentSong.value?.copy(lyrics = lyrics)
                    }
                }
                if (crossfadeSeconds > 0) {
                    fadeInVolume(crossfadeSeconds)
                } else {
                    player.volume = 1.0f
                }
            }
        }
    }

    private var originalPlaylist: List<Song> = emptyList()

    fun setCrossfadeSeconds(seconds: Int) {
        crossfadeSeconds = seconds.coerceIn(0, 12)
    }

    fun playSong(song: Song, newPlaylist: List<Song> = emptyList()) {
        val baseList = if (newPlaylist.isNotEmpty()) newPlaylist else listOf(song)
        originalPlaylist = baseList

        val listToUse = if (_isShuffle.value && baseList.size > 1) {
            val remaining = baseList.filter { it.id != song.id }.shuffled(java.util.Random(System.nanoTime()))
            listOf(song) + remaining
        } else {
            baseList
        }
        _playlist.value = listToUse

        val mediaItems = listToUse.map { s -> buildMediaItem(s) }

        val targetIndex = listToUse.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        player.stop()
        player.clearMediaItems()
        player.setMediaItems(mediaItems, targetIndex, 0L)
        _currentSong.value = song
        trackSongPlay(song)
        ensureMediaServiceStarted()
        scope.launch(Dispatchers.IO) {
            val lyrics = mediaRepo.getLyricsForSong(song)
            if (lyrics.isNotEmpty() && _currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(lyrics = lyrics)
            }
        }
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

    fun savePlaybackState() {
        val current = _currentSong.value ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        val qIds = _playlist.value.map { it.id }
        sonoraPrefs.saveLastPlayback(current.id, pos, qIds)
    }

    fun restorePlaybackState(allSongs: List<Song>) {
        if (_currentSong.value != null || allSongs.isEmpty()) return

        val lastSongId = sonoraPrefs.getLastSongId()
        val lastPos = sonoraPrefs.getLastPositionMs()
        val lastQueueIds = sonoraPrefs.getLastQueueIds()

        val songMap = allSongs.associateBy { it.id }
        val restoredSong = (if (lastSongId > 0) songMap[lastSongId] else null) ?: allSongs.firstOrNull() ?: return
        val restoredQueue = if (lastQueueIds.isNotEmpty()) {
            val q = lastQueueIds.mapNotNull { songMap[it] }
            if (q.isNotEmpty()) q else allSongs
        } else {
            allSongs
        }

        _playlist.value = restoredQueue
        originalPlaylist = restoredQueue
        _currentSong.value = restoredSong
        _currentPositionMs.value = lastPos
        _durationMs.value = restoredSong.durationMs

        val mediaItems = restoredQueue.map { s -> buildMediaItem(s) }
        val targetIndex = restoredQueue.indexOfFirst { it.id == restoredSong.id }.coerceAtLeast(0)

        player.stop()
        player.clearMediaItems()
        player.setMediaItems(mediaItems, targetIndex, lastPos)
        player.prepare()
        player.playWhenReady = false
        _isPlaying.value = false

        scope.launch(Dispatchers.IO) {
            val lyrics = mediaRepo.getLyricsForSong(restoredSong)
            if (lyrics.isNotEmpty() && _currentSong.value?.id == restoredSong.id) {
                _currentSong.value = _currentSong.value?.copy(lyrics = lyrics)
            }
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
        ensureMediaServiceStarted()
        player.playWhenReady = true
        player.play()
        _isPlaying.value = true
        startPositionTracking()
    }

    fun pause() {
        player.pause()
        _isPlaying.value = false
        savePlaybackState()
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
        val willBeShuffle = !_isShuffle.value
        _isShuffle.value = willBeShuffle

        val curr = _currentSong.value
        val currentPos = player.currentPosition
        val isCurrentlyPlaying = player.isPlaying

        val newQueue = if (willBeShuffle) {
            if (curr != null && originalPlaylist.isNotEmpty()) {
                val remaining = originalPlaylist.filter { it.id != curr.id }.shuffled(java.util.Random(System.nanoTime()))
                listOf(curr) + remaining
            } else if (originalPlaylist.isNotEmpty()) {
                originalPlaylist.shuffled(java.util.Random(System.nanoTime()))
            } else {
                _playlist.value.shuffled(java.util.Random(System.nanoTime()))
            }
        } else {
            if (originalPlaylist.isNotEmpty()) originalPlaylist else _playlist.value
        }

        _playlist.value = newQueue

        val mediaItems = newQueue.map { s -> buildMediaItem(s) }

        val targetIndex = if (curr != null) {
            newQueue.indexOfFirst { it.id == curr.id }.coerceAtLeast(0)
        } else 0

        player.setMediaItems(mediaItems, targetIndex, currentPos)
        if (isCurrentlyPlaying) {
            player.playWhenReady = true
            player.play()
        }
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
            var lastTickTime = System.currentTimeMillis()
            while (isActive) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L)
                _currentPositionMs.value = pos
                if (dur > 0L) _durationMs.value = dur

                val now = System.currentTimeMillis()
                if (player.isPlaying) {
                    val elapsedSec = (now - lastTickTime) / 1000L
                    if (elapsedSec >= 1L) {
                        sonoraPrefs.addListeningSeconds(elapsedSec)
                        sonoraPrefs.setLastPositionMs(pos)
                        lastTickTime = now
                    }
                } else {
                    lastTickTime = now
                }

                // Crossfade fade-out when near track end
                if (crossfadeSeconds > 0 && dur > crossfadeSeconds * 2000L) {
                    val remainingMs = dur - pos
                    val crossfadeMs = crossfadeSeconds * 1000L
                    if (remainingMs in 0..crossfadeMs) {
                        val factor = (remainingMs.toFloat() / crossfadeMs.toFloat()).coerceIn(0.05f, 1.0f)
                        player.volume = factor
                    }
                }

                delay(250)
            }
        }
    }

    private fun stopPositionTracking() {
        progressJob?.cancel()
        progressJob = null
        try {
            val pos = player.currentPosition.coerceAtLeast(0L)
            sonoraPrefs.setLastPositionMs(pos)
        } catch (_: Exception) {}
    }

    fun startSleepTimer(minutes: Int, finishCurrentSong: Boolean = false) {
        cancelSleepTimer()
        _sleepTimerFinishSong.value = finishCurrentSong
        if (minutes == -1) {
            _sleepTimerSecondsLeft.value = -1
            waitForSongToEndAndPause()
            return
        }
        var seconds = minutes * 60
        _sleepTimerSecondsLeft.value = seconds
        sleepTimerJob = scope.launch {
            while (seconds > 0 && isActive) {
                delay(1000)
                seconds--
                _sleepTimerSecondsLeft.value = seconds
            }
            if (seconds <= 0) {
                if (finishCurrentSong) {
                    _sleepTimerSecondsLeft.value = -1
                    waitForSongToEndAndPause()
                } else {
                    pause()
                    cancelSleepTimer()
                }
            }
        }
    }

    private fun waitForSongToEndAndPause() {
        val currentSongId = _currentSong.value?.id ?: run {
            pause()
            cancelSleepTimer()
            return
        }
        waitForSongEndJob?.cancel()
        waitForSongEndJob = scope.launch {
            while (isActive) {
                delay(500)
                val nowSong = _currentSong.value
                val isPlayingNow = _isPlaying.value
                if (!isPlayingNow || nowSong == null || nowSong.id != currentSongId || player.playbackState == Player.STATE_ENDED) {
                    pause()
                    cancelSleepTimer()
                    break
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        waitForSongEndJob?.cancel()
        waitForSongEndJob = null
        _sleepTimerSecondsLeft.value = null
        _sleepTimerFinishSong.value = false
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun release() {
        crossfadeJob?.cancel()
        cancelSleepTimer()
        stopPositionTracking()
        visualizerManager.release()
        equalizerManager.release()
        player.removeListener(playerListener)
        player.release()
    }
}
