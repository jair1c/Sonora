package com.sonora.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.sonora.music.data.local.SonoraPreferences
import com.sonora.music.data.model.Song
import com.sonora.music.data.repository.MediaStoreRepository
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.components.SongOptionsModal
import com.sonora.music.ui.screens.AlbumDetailScreen
import com.sonora.music.ui.screens.ArtistDetailScreen
import com.sonora.music.ui.screens.EqualizerModal
import com.sonora.music.ui.screens.EqualizerScreen
import com.sonora.music.ui.screens.NativeHomeScreen
import com.sonora.music.ui.screens.NativePlayerScreen
import com.sonora.music.ui.screens.SettingsScreen
import com.sonora.music.ui.screens.WelcomeScreen
import com.sonora.music.ui.theme.SonoraTheme
import kotlinx.coroutines.launch

class SonoraNativeActivity : ComponentActivity() {

    private lateinit var audioPlayer: SonoraAudioPlayer
    private lateinit var mediaRepo: MediaStoreRepository
    private lateinit var sonoraPrefs: SonoraPreferences
    private var onPermissionGranted: (() -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            onPermissionGranted?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        audioPlayer = SonoraAudioPlayer.getInstance(this)
        mediaRepo = MediaStoreRepository(this)
        sonoraPrefs = SonoraPreferences(this)

        audioPlayer.setCrossfadeSeconds(sonoraPrefs.getCrossfadeSeconds())
        audioPlayer.setPlaybackSpeed(sonoraPrefs.getPlaybackSpeed())

        setContent {
            var currentThemeMode by remember { mutableStateOf(sonoraPrefs.getThemeMode()) }
            val isDark = when (currentThemeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            SonoraTheme(darkTheme = isDark) {
                val scope = rememberCoroutineScope()
                val cachedInitial = remember { sonoraPrefs.getCachedSongs() }
                var songList by remember { mutableStateOf(cachedInitial) }
                var hasSeenWelcome by remember { mutableStateOf(sonoraPrefs.hasSeenWelcome()) }

                // Navigation states
                var isPlayerOpen by remember { mutableStateOf(false) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isEqualizerOpen by remember { mutableStateOf(false) }
                var selectedArtistName by remember { mutableStateOf<String?>(null) }
                var selectedAlbumTitle by remember { mutableStateOf<String?>(null) }
                var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }

                fun loadSongs() {
                    scope.launch {
                        val localSongs = mediaRepo.queryLocalSongs()
                        if (localSongs.isNotEmpty()) {
                            songList = localSongs
                            sonoraPrefs.setCachedSongs(localSongs)
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    onPermissionGranted = { loadSongs() }
                    checkAndRequestPermissions()
                    loadSongs()
                }

                // Handle Android Hardware Back Button
                BackHandler(enabled = isPlayerOpen || isEqualizerOpen || isSettingsOpen || selectedArtistName != null || selectedAlbumTitle != null) {
                    when {
                        isPlayerOpen -> isPlayerOpen = false
                        isEqualizerOpen -> isEqualizerOpen = false
                        isSettingsOpen -> isSettingsOpen = false
                        selectedArtistName != null -> selectedArtistName = null
                        selectedAlbumTitle != null -> selectedAlbumTitle = null
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    if (!hasSeenWelcome) {
                        WelcomeScreen(
                            isDark = isDark,
                            onStart = {
                                sonoraPrefs.setHasSeenWelcome(true)
                                hasSeenWelcome = true
                            }
                        )
                    } else {
                        // 1. Home Screen (5 tabs)
                        NativeHomeScreen(
                            allSongs = songList,
                            audioPlayer = audioPlayer,
                            sonoraPrefs = sonoraPrefs,
                            isDark = isDark,
                            onOpenPlayer = { isPlayerOpen = true },
                            onOpenSettings = { isSettingsOpen = true },
                            onOpenEqualizer = { isEqualizerOpen = true },
                            onOpenArtistDetail = { artist -> selectedArtistName = artist },
                            onOpenAlbumDetail = { album -> selectedAlbumTitle = album },
                            onSongOptions = { song -> selectedSongForOptions = song },
                            onRescanLibrary = { loadSongs() }
                        )

                        // 2. Artist Detail Screen
                        AnimatedVisibility(
                            visible = selectedArtistName != null,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) {
                            selectedArtistName?.let { artist ->
                                ArtistDetailScreen(
                                    artistName = artist,
                                    allSongs = songList,
                                    audioPlayer = audioPlayer,
                                    isDark = isDark,
                                    onBack = { selectedArtistName = null },
                                    onSongOptions = { song -> selectedSongForOptions = song }
                                )
                            }
                        }

                        // 3. Album Detail Screen
                        AnimatedVisibility(
                            visible = selectedAlbumTitle != null,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) {
                            selectedAlbumTitle?.let { album ->
                                AlbumDetailScreen(
                                    albumTitle = album,
                                    allSongs = songList,
                                    audioPlayer = audioPlayer,
                                    isDark = isDark,
                                    onBack = { selectedAlbumTitle = null },
                                    onSongOptions = { song -> selectedSongForOptions = song }
                                )
                            }
                        }

                        // 4. Settings Screen
                        AnimatedVisibility(
                            visible = isSettingsOpen,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) {
                            SettingsScreen(
                                sonoraPrefs = sonoraPrefs,
                                audioPlayer = audioPlayer,
                                songs = songList,
                                isDark = isDark,
                                onBack = { isSettingsOpen = false },
                                onOpenEqualizer = { isEqualizerOpen = true },
                                onRescanLibrary = { loadSongs() },
                                onThemeChanged = { mode -> currentThemeMode = mode }
                            )
                        }

                        // 5. Equalizer Modal
                        EqualizerModal(
                            isOpen = isEqualizerOpen,
                            onClose = { isEqualizerOpen = false },
                            audioPlayer = audioPlayer,
                            sonoraPrefs = sonoraPrefs,
                            isDark = isDark
                        )

                        // 6. Fullscreen Luxury Player
                        AnimatedVisibility(
                            visible = isPlayerOpen,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            NativePlayerScreen(
                                audioPlayer = audioPlayer,
                                sonoraPrefs = sonoraPrefs,
                                isDark = isDark,
                                onDismiss = { isPlayerOpen = false }
                            )
                        }

                        // 7. Song Options Modal (3-dots)
                        if (selectedSongForOptions != null) {
                            SongOptionsModal(
                                song = selectedSongForOptions!!,
                                playlists = sonoraPrefs.getCustomPlaylists(),
                                isDark = isDark,
                                onDismiss = { selectedSongForOptions = null },
                                onPlayNext = {
                                    audioPlayer.playSong(selectedSongForOptions!!, listOf(selectedSongForOptions!!) + audioPlayer.playlist.value)
                                },
                                onAddToPlaylist = { playlistId ->
                                    sonoraPrefs.addSongToPlaylist(playlistId, selectedSongForOptions!!.id)
                                },
                                onNavigateToArtist = { artist ->
                                    selectedArtistName = artist
                                },
                                onNavigateToAlbum = { album ->
                                    selectedAlbumTitle = album
                                },
                                onBlacklistFolder = { folder ->
                                    sonoraPrefs.toggleBlacklistFolder(folder)
                                    loadSongs()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !audioPlayer.isPlaying.value) {
            audioPlayer.release()
        }
    }
}
