package com.sonora.music.service

import com.sonora.music.util.AudioMetadataHelper
import com.sonora.music.util.AudioSilenceDetector
import com.sonora.music.util.AudioFormatDetails

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import com.sonora.music.data.model.Song
import com.sonora.music.data.repository.SongCoverRepository
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

enum class OutputIconType {
    SPEAKER,
    BLUETOOTH,
    HEADPHONES,
    USB
}

data class OutputDeviceInfo(
    val name: String,
    val iconType: OutputIconType
)

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



    private fun createExoPlayer(): ExoPlayer {
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                shuffleModeEnabled = false
            }
    }

    private val player1: ExoPlayer by lazy {
        createExoPlayer().apply { addListener(createPlayerListener(this)) }
    }

    private val player2: ExoPlayer by lazy {
        createExoPlayer().apply { addListener(createPlayerListener(this)) }
    }

    @Volatile
    private var activePlayer: ExoPlayer = player1

    @Volatile
    private var standbyPlayer: ExoPlayer = player2

    val exoPlayer: ExoPlayer get() = activePlayer

    private val _activePlayerFlow = MutableStateFlow<ExoPlayer>(player1)
    val activePlayerFlow = _activePlayerFlow.asStateFlow()

    val equalizerManager = SonoraEqualizerManager()
    val visualizerManager = SonoraVisualizerManager()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var crossfadeJob: Job? = null

    private var crossfadeSeconds: Int = 0
    private var isCrossfading = false

    private var preloadedSong: Song? = null
    private var preloadJob: Job? = null

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("SonoraAudioPlayer", "Could not start SonoraMediaService", e)
        }
    }

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _realAudioFormat = MutableStateFlow<AudioFormatDetails?>(null)
    val realAudioFormat = _realAudioFormat.asStateFlow()

    var onStateInvalidated: (() -> Unit)? = null

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch = _playbackPitch.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentOutputDevice = MutableStateFlow(OutputDeviceInfo("Altavoz del teléfono", OutputIconType.SPEAKER))
    val currentOutputDevice = _currentOutputDevice.asStateFlow()

    private var preloadedStartOffsetMs = 0L

    private val audioDeviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                updateOutputDevice()
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                updateOutputDevice()
            }
        }
    } else null

    private fun updateOutputDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                var bestDevice: OutputDeviceInfo? = null

                for (device in devices) {
                    val type = device.type
                    val rawName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && device.productName.isNotBlank()) {
                        device.productName.toString()
                    } else null

                    when (type) {
                        android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        android.media.AudioDeviceInfo.TYPE_BLE_HEADSET,
                        android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        26, // TYPE_BLE_SPEAKER
                        27  // TYPE_BLE_BROADCAST
                        -> {
                            bestDevice = OutputDeviceInfo(rawName ?: "Audífonos Bluetooth", OutputIconType.BLUETOOTH)
                            break
                        }
                        android.media.AudioDeviceInfo.TYPE_USB_DEVICE,
                        android.media.AudioDeviceInfo.TYPE_USB_HEADSET,
                        android.media.AudioDeviceInfo.TYPE_USB_ACCESSORY -> {
                            if (bestDevice == null || bestDevice.iconType == OutputIconType.SPEAKER) {
                                bestDevice = OutputDeviceInfo(rawName ?: "Dispositivo USB", OutputIconType.USB)
                            }
                        }
                        android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        android.media.AudioDeviceInfo.TYPE_LINE_ANALOG,
                        android.media.AudioDeviceInfo.TYPE_LINE_DIGITAL -> {
                            if (bestDevice == null || bestDevice.iconType == OutputIconType.SPEAKER) {
                                bestDevice = OutputDeviceInfo(rawName ?: "Audífonos con cable", OutputIconType.HEADPHONES)
                            }
                        }
                        android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                        android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                            if (bestDevice == null) {
                                bestDevice = OutputDeviceInfo("Altavoz del teléfono", OutputIconType.SPEAKER)
                            }
                        }
                    }
                }
                _currentOutputDevice.value = bestDevice ?: OutputDeviceInfo("Altavoz del teléfono", OutputIconType.SPEAKER)
            } else {
                _currentOutputDevice.value = OutputDeviceInfo("Altavoz del teléfono", OutputIconType.SPEAKER)
            }
        } catch (_: Exception) {
            _currentOutputDevice.value = OutputDeviceInfo("Altavoz del teléfono", OutputIconType.SPEAKER)
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
            try {
                audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
            } catch (e: Exception) {
                android.util.Log.e("SonoraAudioPlayer", "Error registering audioDeviceCallback", e)
            }
        }
        updateOutputDevice()
    }


    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist = _playlist.asStateFlow()

    private var audioFocusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false
    private var isDucked = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                isDucked = false
                pause(abandonFocus = true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_isPlaying.value) {
                    resumeOnFocusGain = true
                    pause(abandonFocus = false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_isPlaying.value) {
                    isDucked = true
                    try {
                        activePlayer.volume = 0.2f
                        if (isCrossfading) {
                            standbyPlayer.volume = 0.2f
                        }
                    } catch (_: Exception) {}
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isDucked) {
                    isDucked = false
                    try {
                        activePlayer.volume = 1.0f
                        if (isCrossfading) {
                            standbyPlayer.volume = 1.0f
                        }
                    } catch (_: Exception) {}
                }
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    resume()
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AndroidAudioAttributes.Builder()
                .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
                .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
                audioFocusRequest = null
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }


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

    fun buildMediaItem(s: Song, useFileFallback: Boolean = false): MediaItem {
        val uri = if (useFileFallback && s.filePath.isNotEmpty() && File(s.filePath).exists()) {
            android.net.Uri.fromFile(File(s.filePath))
        } else {
            s.contentUri
        }

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(s.title)
            .setArtist(s.artist)
            .setAlbumTitle(s.album)
            .setArtworkUri(s.coverUri)

        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(s.id.toString())
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun createPlayerListener(p: ExoPlayer) = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            if (isCrossfading) {
                if (playing) {
                    _isPlaying.value = true
                    startPositionTracking()
                    visualizerManager.setPlaying(true)
                }
                return
            }
            if (p == activePlayer) {
                if (playing) {
                    _isPlaying.value = true
                    startPositionTracking()
                    visualizerManager.setPlaying(true)
                } else if (!p.playWhenReady && p.playbackState != Player.STATE_BUFFERING) {
                    _isPlaying.value = false
                    stopPositionTracking()
                    visualizerManager.setPlaying(false)
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (p == activePlayer && !isCrossfading) {
                if (!playWhenReady) {
                    _isPlaying.value = false
                    stopPositionTracking()
                    visualizerManager.setPlaying(false)
                } else if (p.playbackState != Player.STATE_ENDED) {
                    _isPlaying.value = true
                    startPositionTracking()
                    visualizerManager.setPlaying(true)
                }
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (p == activePlayer) {
                equalizerManager.attachAudioSession(audioSessionId)
                equalizerManager.setPreAmp(sonoraPrefs.getPreAmpGain())
                equalizerManager.setBassBoost(sonoraPrefs.getBassBoost().toShort())
                equalizerManager.setAutoVolumeLeveling(sonoraPrefs.isAutoVolumeLeveling())
                visualizerManager.attachAudioSession(audioSessionId)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (isCrossfading && p != activePlayer && p != standbyPlayer) {
                return
            }
            if (p == activePlayer) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _durationMs.value = p.duration.coerceAtLeast(0L)
                        if (p.playWhenReady) {
                            _isPlaying.value = true
                            startPositionTracking()
                            visualizerManager.setPlaying(true)
                        }
                        equalizerManager.attachAudioSession(p.audioSessionId)
                        equalizerManager.setPreAmp(sonoraPrefs.getPreAmpGain())
                        equalizerManager.setBassBoost(sonoraPrefs.getBassBoost().toShort())
                        equalizerManager.setAutoVolumeLeveling(sonoraPrefs.isAutoVolumeLeveling())
                        visualizerManager.attachAudioSession(p.audioSessionId)
                    }
                    Player.STATE_ENDED -> {
                        if (!isCrossfading) {
                            _isPlaying.value = false
                            stopPositionTracking()
                            visualizerManager.setPlaying(false)
                            nextTrack()
                        }
                    }
                    else -> {}
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("SonoraAudioPlayer", "Playback error: ${error.errorCodeName} (${error.errorCode})", error)
            val current = _currentSong.value
            if (current != null && p == activePlayer) {
                try {
                    val currentUri = p.currentMediaItem?.localConfiguration?.uri
                    val fallbackUri = if (currentUri == current.contentUri && current.filePath.isNotEmpty() && File(current.filePath).exists()) {
                        android.net.Uri.fromFile(File(current.filePath))
                    } else {
                        current.contentUri
                    }
                    val fallbackItem = MediaItem.Builder()
                        .setUri(fallbackUri)
                        .setMediaId(current.id.toString())
                        .build()
                    p.setMediaItem(fallbackItem)
                    p.prepare()
                    p.playWhenReady = true
                    p.play()
                    _isPlaying.value = true
                    startPositionTracking()
                } catch (_: Exception) {
                    if (!isCrossfading) {
                        _isPlaying.value = false
                        stopPositionTracking()
                    }
                }
            }
        }
    }

    private var originalPlaylist: List<Song> = emptyList()

    fun setCrossfadeSeconds(seconds: Int) {
        crossfadeSeconds = seconds.coerceIn(0, 12)
    }

    fun playSong(song: Song, newPlaylist: List<Song> = emptyList()) {
        preloadJob?.cancel()
        preloadJob = null
        preloadedSong = null
        crossfadeJob?.cancel()
        isCrossfading = false

        try {
            standbyPlayer.stop()
            standbyPlayer.clearMediaItems()
        } catch (_: Exception) {}

        val baseList = if (newPlaylist.isNotEmpty()) newPlaylist else listOf(song)
        originalPlaylist = baseList

        val listToUse = if (_isShuffle.value && baseList.size > 1) {
            val remaining = baseList.filter { it.id != song.id }.shuffled(java.util.Random(System.nanoTime()))
            listOf(song) + remaining
        } else {
            baseList
        }
        _playlist.value = listToUse

        val useFile = song.filePath.isNotEmpty() && File(song.filePath).exists() && File(song.filePath).canRead()
        val mediaItem = buildMediaItem(song, useFileFallback = useFile)

        activePlayer.setMediaItem(mediaItem)
        activePlayer.volume = 1.0f

        _currentSong.value = song
        _currentPositionMs.value = 0L
        _durationMs.value = song.durationMs
        _isPlaying.value = true
        _activePlayerFlow.value = activePlayer

        sonoraPrefs.saveLastPlayback(song.id, 0L, listToUse.map { it.id })
        trackSongPlay(song)
        ensureMediaServiceStarted()
        visualizerManager.setPlaying(true)
        val hasArtwork = activePlayer.mediaMetadata.artworkData != null
        scope.launch(Dispatchers.IO) {
            val format = AudioMetadataHelper.getAudioDetails(song, context)
            _realAudioFormat.value = format

            val lyrics = mediaRepo.getLyricsForSong(song)
            if (lyrics.isNotEmpty() && _currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(lyrics = lyrics)
            }

            if (!hasArtwork) {
                try {
                    val coverUrl = SongCoverRepository.getSongCoverUrl(song)
                    if (coverUrl != null && coverUrl.startsWith("http")) {
                        val conn = (URL(coverUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 3000
                            readTimeout = 3000
                        }
                        if (conn.responseCode == 200) {
                            val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                            if (bitmap != null && _currentSong.value?.id == song.id) {
                                val byteStream = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteStream)
                                val bytes = byteStream.toByteArray()
                                withContext(Dispatchers.Main) {
                                    if (_currentSong.value?.id == song.id) {
                                        val updatedMetadata = activePlayer.mediaMetadata.buildUpon()
                                            .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                            .build()
                                        activePlayer.setPlaylistMetadata(updatedMetadata)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

                requestAudioFocus()
        activePlayer.playWhenReady = true
        activePlayer.prepare()
        activePlayer.play()
        startPositionTracking()
    }

    fun savePlaybackState() {
        val current = _currentSong.value ?: return
        val pos = activePlayer.currentPosition.coerceAtLeast(0L)
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

        activePlayer.stop()
        activePlayer.clearMediaItems()
        activePlayer.setMediaItem(buildMediaItem(restoredSong, useFileFallback = true), lastPos)
        activePlayer.prepare()
        activePlayer.playWhenReady = false
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
        requestAudioFocus()
        if (activePlayer.playbackState == Player.STATE_IDLE) {
            activePlayer.prepare()
        } else if (activePlayer.playbackState == Player.STATE_ENDED) {
            activePlayer.seekTo(0L)
        }
        ensureMediaServiceStarted()
        activePlayer.playWhenReady = true
        activePlayer.play()
        _isPlaying.value = true
        onStateInvalidated?.invoke()
        startPositionTracking()
    }

    fun pause(abandonFocus: Boolean = true) {
        if (abandonFocus) {
            resumeOnFocusGain = false
            abandonAudioFocus()
        }
        activePlayer.pause()
        if (isCrossfading) {
            standbyPlayer.pause()
        }
        _isPlaying.value = false
        savePlaybackState()
        onStateInvalidated?.invoke()
    }

    fun togglePlay() {
        if (activePlayer.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun prevTrack() {
        if (activePlayer.currentPosition > 3000L) {
            activePlayer.seekTo(0L)
            return
        }
        val queue = _playlist.value
        val current = _currentSong.value
        val currentIdx = if (current != null) queue.indexOfFirst { it.id == current.id } else -1
        val prevSong = if (currentIdx > 0) {
            queue[currentIdx - 1]
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL && queue.isNotEmpty()) {
            queue.last()
        } else null

        if (prevSong != null) {
            playSong(prevSong, queue)
        }
    }

    fun seekTo(positionMs: Long) {
        if (preloadedSong != null) {
            preloadJob?.cancel()
            preloadedSong = null
            try {
                standbyPlayer.stop()
                standbyPlayer.clearMediaItems()
            } catch (_: Exception) {}
        }
        val targetVol = if (isCrossfading) activePlayer.volume else 1.0f
        activePlayer.volume = 0f
        activePlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
        scope.launch {
            delay(80)
            for (step in 1..8) {
                delay(15)
                activePlayer.volume = (step / 8f * targetVol).coerceIn(0f, 1f)
            }
            activePlayer.volume = targetVol
        }
    }

    fun toggleShuffle() {
        val willBeShuffle = !_isShuffle.value
        _isShuffle.value = willBeShuffle

        val curr = _currentSong.value
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
        if (curr != null) {
            sonoraPrefs.saveLastPlayback(curr.id, activePlayer.currentPosition, newQueue.map { it.id })
        }
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
    }

    private fun preloadNextSong(nextSong: Song) {
        if (preloadedSong?.id == nextSong.id) return
        preloadJob?.cancel()
        preloadJob = scope.launch {
            try {
                standbyPlayer.stop()
                standbyPlayer.clearMediaItems()
                standbyPlayer.setMediaItem(buildMediaItem(nextSong, useFileFallback = true))
                standbyPlayer.volume = 0f
                standbyPlayer.playWhenReady = false
                standbyPlayer.prepare()
                preloadedSong = nextSong
                android.util.Log.d("SonoraAudioPlayer", "Preloaded next song into standbyPlayer: ${nextSong.title}")
            } catch (e: Exception) {
                android.util.Log.w("SonoraAudioPlayer", "Preload failed: ${e.message}")
                preloadedSong = null
            }
        }
    }

    private fun performCrossfadeTransition(targetNextSong: Song? = null) {
        if (isCrossfading) return
        val current = _currentSong.value ?: return
        val queue = _playlist.value
        val nextSong = targetNextSong ?: run {
            val currentIdx = queue.indexOfFirst { it.id == current.id }
            if (currentIdx != -1 && currentIdx + 1 < queue.size) {
                queue[currentIdx + 1]
            } else if (_repeatMode.value == Player.REPEAT_MODE_ALL && queue.isNotEmpty()) {
                queue.first()
            } else {
                null
            }
        } ?: return

        isCrossfading = true
        crossfadeJob?.cancel()
        preloadJob?.cancel()

        val durationMs = crossfadeSeconds * 1000L
        val steps = (crossfadeSeconds * 40).coerceAtLeast(25)
        val stepDelay = (durationMs / steps).coerceAtLeast(16L)

        val useFile = nextSong.filePath.isNotEmpty() && File(nextSong.filePath).exists() && File(nextSong.filePath).canRead()
        if (preloadedSong?.id != nextSong.id) {
            try {
                standbyPlayer.stop()
                standbyPlayer.clearMediaItems()
                val offset = AudioSilenceDetector.detectInitialSilenceMs(nextSong.filePath)
                standbyPlayer.setMediaItem(buildMediaItem(nextSong, useFileFallback = useFile), offset)
                standbyPlayer.prepare()
            } catch (e: Exception) {
                android.util.Log.e("SonoraAudioPlayer", "Error preparing standbyPlayer for crossfade", e)
            }
        }

        try {
            standbyPlayer.volume = 0f
            standbyPlayer.playWhenReady = true
            standbyPlayer.play()
        } catch (e: Exception) {
            android.util.Log.e("SonoraAudioPlayer", "Error starting standbyPlayer", e)
        }

        val fadingOutPlayer = activePlayer
        val fadingInPlayer = standbyPlayer
        var metadataSwitched = false

        crossfadeJob = scope.launch {
            for (step in 1..steps) {
                delay(stepDelay)
                val t = step.toDouble() / steps.toDouble()
                // Constant power equal-energy curve: cos(t * pi/2) for out, sin(t * pi/2) for in
                val angle = t * (Math.PI / 2.0)
                val mult = if (isDucked) 0.2f else 1.0f
                val volOut = Math.cos(angle).toFloat().coerceIn(0f, 1f) * mult
                val volIn = Math.sin(angle).toFloat().coerceIn(0f, 1f) * mult
                try { fadingOutPlayer.volume = volOut } catch (_: Exception) {}
                try { fadingInPlayer.volume = volIn } catch (_: Exception) {}

                // Switch UI and metadata seamlessly at 50% midpoint of the crossfade
                if (t >= 0.5 && !metadataSwitched) {
                    metadataSwitched = true
                    _currentSong.value = nextSong
                    sonoraPrefs.saveLastPlayback(nextSong.id, 0L, queue.map { it.id })
                    trackSongPlay(nextSong)
                    ensureMediaServiceStarted()
                    onStateInvalidated?.invoke()
                    onStateInvalidated?.invoke()
                    scope.launch(Dispatchers.IO) {
                        _realAudioFormat.value = AudioMetadataHelper.getAudioDetails(nextSong, context)
                        val lyrics = mediaRepo.getLyricsForSong(nextSong)
                        if (lyrics.isNotEmpty() && _currentSong.value?.id == nextSong.id) {
                            _currentSong.value = _currentSong.value?.copy(lyrics = lyrics)
                        }
                    }
                }
            }

            // 1. Promote fadingInPlayer to activePlayer FIRST
            fadingInPlayer.volume = if (isDucked) 0.2f else 1.0f
            activePlayer = fadingInPlayer
            standbyPlayer = fadingOutPlayer
            _activePlayerFlow.value = activePlayer

            if (!metadataSwitched) {
                _currentSong.value = nextSong
                sonoraPrefs.saveLastPlayback(nextSong.id, 0L, queue.map { it.id })
                trackSongPlay(nextSong)
            }

            // 2. Attach effects to new active player
            equalizerManager.attachAudioSession(activePlayer.audioSessionId)
            visualizerManager.attachAudioSession(activePlayer.audioSessionId)

            // 3. Stop fadingOutPlayer safely now that it is standbyPlayer
            try {
                fadingOutPlayer.stop()
                fadingOutPlayer.clearMediaItems()
                fadingOutPlayer.volume = 1.0f
            } catch (_: Exception) {}

            preloadedSong = null
            isCrossfading = false
            _isPlaying.value = true
            onStateInvalidated?.invoke()
            startPositionTracking()
        }
    }

    fun nextTrack(isManualSkip: Boolean = true) {
        val queue = _playlist.value
        val current = _currentSong.value
        val currentIdx = if (current != null) queue.indexOfFirst { it.id == current.id } else -1
        val nextSong = if (currentIdx != -1 && currentIdx + 1 < queue.size) {
            queue[currentIdx + 1]
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL && queue.isNotEmpty()) {
            queue.first()
        } else {
            null
        }

        if (nextSong != null) {
            if (!isManualSkip && crossfadeSeconds > 0 && activePlayer.isPlaying && !isCrossfading) {
                performCrossfadeTransition(nextSong)
            } else {
                playSong(nextSong, queue)
            }
        }
    }

    private fun startPositionTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastTickTime = System.currentTimeMillis()
            while (isActive) {
                val currentPlayer = if (isCrossfading && _currentSong.value?.id == preloadedSong?.id) standbyPlayer else activePlayer
                val pos = currentPlayer.currentPosition.coerceAtLeast(0L)
                val dur = currentPlayer.duration.coerceAtLeast(0L)
                _currentPositionMs.value = pos
                if (dur > 0L) _durationMs.value = dur

                val now = System.currentTimeMillis()
                if (activePlayer.isPlaying || (isCrossfading && standbyPlayer.isPlaying)) {
                    val elapsedSec = (now - lastTickTime) / 1000L
                    if (elapsedSec >= 1L) {
                        sonoraPrefs.addListeningSeconds(elapsedSec)
                        val curr = _currentSong.value
                        if (curr != null) {
                            sonoraPrefs.saveLastPlayback(curr.id, pos, _playlist.value.map { it.id })
                        } else {
                            sonoraPrefs.setLastPositionMs(pos)
                        }
                        lastTickTime = now
                    }
                } else {
                    lastTickTime = now
                }

                // Check crossfade trigger based on activePlayer (Song A)
                if (crossfadeSeconds > 0 && !isCrossfading && activePlayer.isPlaying) {
                    val activePos = activePlayer.currentPosition.coerceAtLeast(0L)
                    val activeDur = activePlayer.duration.coerceAtLeast(0L)
                    if (activeDur > crossfadeSeconds * 1500L) {
                        val remainingMs = activeDur - activePos
                        val crossfadeMs = crossfadeSeconds * 1000L
                        val preloadWindowMs = crossfadeMs * 2

                        if (remainingMs in 0..crossfadeMs) {
                            performCrossfadeTransition()
                        } else if (remainingMs in 0..preloadWindowMs && preloadedSong == null) {
                            val currentIdx = if (_currentSong.value != null) _playlist.value.indexOfFirst { it.id == _currentSong.value?.id } else -1
                            val next = if (currentIdx != -1 && currentIdx + 1 < _playlist.value.size) {
                                _playlist.value[currentIdx + 1]
                            } else if (_repeatMode.value == Player.REPEAT_MODE_ALL && _playlist.value.isNotEmpty()) {
                                _playlist.value.first()
                            } else null

                            if (next != null) {
                                preloadNextSong(next)
                            }
                        }
                    }
                }

                delay(250)
            }
        }
    }

    private fun stopPositionTracking() {
        if (isCrossfading) return
        progressJob?.cancel()
        progressJob = null
        try {
            val pos = activePlayer.currentPosition.coerceAtLeast(0L)
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
                if (!isPlayingNow || nowSong == null || nowSong.id != currentSongId || activePlayer.playbackState == Player.STATE_ENDED) {
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

    fun setPlaybackParameters(speed: Float, pitch: Float) {
        val s = speed.coerceIn(0.25f, 3.0f)
        val p = pitch.coerceIn(0.25f, 2.0f)
        _playbackSpeed.value = s
        _playbackPitch.value = p
        val params = androidx.media3.common.PlaybackParameters(s, p)
        try {
            activePlayer.playbackParameters = params
            standbyPlayer.playbackParameters = params
        } catch (_: Exception) {}
    }

    fun setPlaybackSpeed(speed: Float) {
        setPlaybackParameters(speed, _playbackPitch.value)
    }

    fun release() {
        abandonAudioFocus()
        crossfadeJob?.cancel()
        preloadJob?.cancel()
        preloadedSong = null
        cancelSleepTimer()
        stopPositionTracking()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
            try { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) } catch (_: Exception) {}
        }
        visualizerManager.release()
        equalizerManager.release()
        try {
            player1.release()
            player2.release()
        } catch (_: Exception) {}
    }
}
