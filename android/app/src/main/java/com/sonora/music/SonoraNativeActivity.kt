package com.sonora.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.sonora.music.data.model.Song
import com.sonora.music.data.repository.MediaStoreRepository
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.screens.NativeHomeScreen
import com.sonora.music.ui.screens.NativePlayerScreen
import com.sonora.music.ui.theme.SonoraTheme
import kotlinx.coroutines.launch

class SonoraNativeActivity : ComponentActivity() {

    private lateinit var audioPlayer: SonoraAudioPlayer
    private lateinit var mediaRepo: MediaStoreRepository
    private var onPermissionGranted: (() -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onPermissionGranted?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        audioPlayer = SonoraAudioPlayer(this)
        mediaRepo = MediaStoreRepository(this)

        setContent {
            SonoraTheme {
                val scope = rememberCoroutineScope()
                var songList by remember { mutableStateOf<List<Song>>(emptyList()) }
                var isPlayerOpen by remember { mutableStateOf(false) }

                fun loadSongs() {
                    scope.launch {
                        val localSongs = mediaRepo.queryLocalSongs()
                        songList = localSongs
                    }
                }

                LaunchedEffect(Unit) {
                    onPermissionGranted = { loadSongs() }
                    checkAndRequestPermissions()
                    loadSongs()
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    NativeHomeScreen(
                        allSongs = songList,
                        audioPlayer = audioPlayer,
                        onOpenPlayer = { isPlayerOpen = true },
                        onRefresh = { loadSongs() }
                    )


                    AnimatedVisibility(
                        visible = isPlayerOpen,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        NativePlayerScreen(
                            audioPlayer = audioPlayer,
                            onDismiss = { isPlayerOpen = false }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(perm)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
    }
}

