package com.sonora.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.sonora.music.ui.theme.SonoraGold
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.sonora.music.data.local.SonoraPreferences
import com.sonora.music.data.model.Song
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.components.Organic8PetalShape
import com.sonora.music.ui.components.WavyScrubberRing
import com.sonora.music.ui.theme.SonoraObsidianDark
import com.sonora.music.ui.theme.SonoraPaperBeige
import kotlin.math.roundToInt

@Composable
fun NativePlayerScreen(
    audioPlayer: SonoraAudioPlayer,
    sonoraPrefs: SonoraPreferences,
    isDark: Boolean = when (sonoraPrefs.getThemeMode()) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    },
    onDismiss: () -> Unit
) {
    val bgColor = if (isDark) SonoraObsidianDark else SonoraPaperBeige
    val textColor = if (isDark) Color.White else Color(0xFF121212)
    val subtextColor = if (isDark) Color(0xFFA19C93) else Color(0xFF6B6760)

    val currentSong by audioPlayer.currentSong.collectAsState()
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentPositionMs by audioPlayer.currentPositionMs.collectAsState()
    val durationMs by audioPlayer.durationMs.collectAsState()
    val isShuffle by audioPlayer.isShuffle.collectAsState()
    val repeatMode by audioPlayer.repeatMode.collectAsState()

    var showExpandedLyrics by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isLiked by remember(currentSong) { mutableStateOf(currentSong?.let { sonoraPrefs.isSongLiked(it.id) } ?: false) }

    val safeDuration = if (durationMs > 0) durationMs else (currentSong?.durationMs ?: 180000L)
    val progress = if (safeDuration > 0) (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) else 0f

    // 120 FPS Rotation transition for Vinyl Artwork
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Smooth organic spring scaling for the entire flower artwork upon Play/Pause
    val flowerScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "flower_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) {
                            onDismiss()
                        }
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0 || offsetY > 0) {
                            offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                        }
                    }
                )
            }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar with Back, Title, Queue, and Heart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cerrar",
                        tint = textColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "REPRODUCIENDO",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = textColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Cola",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            currentSong?.let { s ->
                                val nowLiked = sonoraPrefs.toggleLikeSong(s.id)
                                isLiked = nowLiked
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isLiked) Color(0xFFEF4444) else textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }


            // 2. Time & Audio Quality Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val currentSec = currentPositionMs / 1000
                val totalSec = safeDuration / 1000
                val formattedTime = "%02d:%02d | %02d:%02d".format(
                    currentSec / 60, currentSec % 60,
                    totalSec / 60, totalSec % 60
                )
                Text(
                    text = formattedTime,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = subtextColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentSong?.audioQualityBadge ?: "Hi-Fi Audio",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                }
            }

            // 3. Central Artwork & Wavy Scrubber (20% Larger)
            Box(
                modifier = Modifier.size(368.dp),
                contentAlignment = Alignment.Center
            ) {
                WavyScrubberRing(
                    progressPercent = progress,
                    isDarkTheme = isDark,
                    onSeekPercent = { pct ->
                        audioPlayer.seekTo((pct * safeDuration).toLong())
                    }
                )

                // 8-Petal Vinyl Image (Smooth organic scale for the entire flower and artwork)
                Box(
                    modifier = Modifier
                        .size(288.dp)
                        .scale(flowerScale)
                        .clip(Organic8PetalShape(petalCount = 8, amplitude = 0.08f))
                        .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                        .clickable { audioPlayer.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentSong?.coverUri ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=600&auto=format&fit=crop",
                        contentDescription = currentSong?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(if (isPlaying) rotationAngle else 0f)
                    )

                    // Vinyl center pinhole
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                    )
                }
            }

            // 4. Title & Artist
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentSong?.title ?: "Sonora Music",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${currentSong?.artist ?: "Offline"} • ${currentSong?.album ?: "Local"}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = subtextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 5. Dynamic Luxury Playback Controls (5 Estilos Seleccionables)
            when (sonoraPrefs.getPlayerControlsStyle()) {
                "circles" -> PlaybackControlsCircles(
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    isShuffle = isShuffle,
                    isDark = isDark,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    bgColor = bgColor,
                    audioPlayer = audioPlayer
                )
                "organic" -> PlaybackControlsOrganic(
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    isShuffle = isShuffle,
                    isDark = isDark,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    bgColor = bgColor,
                    audioPlayer = audioPlayer
                )
                "squircle" -> PlaybackControlsSquircle(
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    isShuffle = isShuffle,
                    isDark = isDark,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    bgColor = bgColor,
                    audioPlayer = audioPlayer
                )
                "waveform" -> PlaybackControlsWaveform(
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    isShuffle = isShuffle,
                    isDark = isDark,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    bgColor = bgColor,
                    audioPlayer = audioPlayer
                )
                else -> PlaybackControlsDock(
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    isShuffle = isShuffle,
                    isDark = isDark,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    bgColor = bgColor,
                    audioPlayer = audioPlayer
                )
            }

            // 6. Dynamic Realtime Lyrics Preview Pill
            val lyrics = currentSong?.lyrics ?: emptyList()
            val currentLyricIdx = if (lyrics.isNotEmpty()) {
                lyrics.indexOfLast { currentPositionMs >= it.timeMs }.coerceAtLeast(0)
            } else 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                    .clickable { showExpandedLyrics = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                if (lyrics.isNotEmpty() && currentLyricIdx in lyrics.indices) {
                    val activeLine = lyrics[currentLyricIdx].text
                    val nextLine = lyrics.getOrNull(currentLyricIdx + 1)?.text
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "♪ $activeLine",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (nextLine != null) {
                            Text(
                                text = nextLine,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = subtextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Toca para ver letras o sincronizar archivos .lrc",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtextColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Fullscreen Synchronized Lyrics Overlay with Smooth Auto-Centering
        if (showExpandedLyrics) {
            val listState = rememberLazyListState()
            val lyrics = currentSong?.lyrics ?: emptyList()
            val activeIdx = if (lyrics.isNotEmpty()) {
                lyrics.indexOfLast { currentPositionMs >= it.timeMs }.coerceAtLeast(0)
            } else 0

            LaunchedEffect(activeIdx) {
                if (lyrics.isNotEmpty() && activeIdx in lyrics.indices) {
                    listState.animateScrollToItem(activeIdx)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LETRAS SINCRONIZADAS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = textColor
                            )
                            Text(
                                text = currentSong?.title ?: "",
                                fontSize = 11.sp,
                                color = subtextColor,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { showExpandedLyrics = false },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Cerrar",
                                tint = textColor
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 220.dp, bottom = 280.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(lyrics) { idx, line ->
                            val isActive = idx == activeIdx
                            Text(
                                text = line.text,
                                fontSize = if (isActive) 24.sp else 16.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isActive) textColor else subtextColor.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { audioPlayer.seekTo(line.timeMs) }
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showQueueSheet) {
            com.sonora.music.ui.components.QueueBottomSheet(
                audioPlayer = audioPlayer,
                isDark = isDark,
                onDismiss = { showQueueSheet = false }
            )
        }
    }
}

// -------------------------------------------------------------
// 5 LUXURY PLAYBACK CONTROLS STYLES
// -------------------------------------------------------------

// 1. Dock Flotante Clásico
@Composable
private fun PlaybackControlsDock(
    isPlaying: Boolean,
    repeatMode: Int,
    isShuffle: Boolean,
    isDark: Boolean,
    textColor: Color,
    subtextColor: Color,
    bgColor: Color,
    audioPlayer: com.sonora.music.service.SonoraAudioPlayer
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(if (isDark) Color(0xFF161513) else Color(0xFFEAE5DA))
            .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), RoundedCornerShape(40.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { audioPlayer.toggleRepeat() },
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repetir",
                    tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) SonoraGold else subtextColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { audioPlayer.prevTrack() },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = textColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(76.dp))

            IconButton(
                onClick = { audioPlayer.nextTrack() },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Siguiente",
                    tint = textColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            IconButton(
                onClick = { audioPlayer.toggleShuffle() },
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = if (isShuffle) SonoraGold else subtextColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(textColor)
                .clickable { audioPlayer.togglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = bgColor,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

// 2. Pura Geometría Suiza (Swiss Luxury Circles)
@Composable
private fun PlaybackControlsCircles(
    isPlaying: Boolean,
    repeatMode: Int,
    isShuffle: Boolean,
    isDark: Boolean,
    textColor: Color,
    subtextColor: Color,
    bgColor: Color,
    audioPlayer: com.sonora.music.service.SonoraAudioPlayer
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(
                    if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) 2.dp else 1.dp,
                    if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) SonoraGold else (if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)),
                    CircleShape
                )
                .clickable { audioPlayer.toggleRepeat() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = "Repetir",
                tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) SonoraGold else subtextColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), CircleShape)
                .clickable { audioPlayer.prevTrack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(textColor)
                .clickable { audioPlayer.togglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = bgColor,
                modifier = Modifier.size(46.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), CircleShape)
                .clickable { audioPlayer.nextTrack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(
                    if (isShuffle) 2.dp else 1.dp,
                    if (isShuffle) SonoraGold else (if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)),
                    CircleShape
                )
                .clickable { audioPlayer.toggleShuffle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Aleatorio",
                tint = if (isShuffle) SonoraGold else subtextColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 3. Orgánico Esculpido (Firma Sonora - Flor y Pétalos)
@Composable
private fun PlaybackControlsOrganic(
    isPlaying: Boolean,
    repeatMode: Int,
    isShuffle: Boolean,
    isDark: Boolean,
    textColor: Color,
    subtextColor: Color,
    bgColor: Color,
    audioPlayer: com.sonora.music.service.SonoraAudioPlayer
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { audioPlayer.toggleRepeat() },
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) (if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)) else Color.Transparent)
        ) {
            Icon(
                imageVector = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = "Repetir",
                tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) SonoraGold else subtextColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(width = 66.dp, height = 54.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp, topEnd = 10.dp, bottomEnd = 10.dp))
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp, topEnd = 10.dp, bottomEnd = 10.dp))
                .clickable { audioPlayer.prevTrack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(Organic8PetalShape(petalCount = 8, amplitude = 0.12f))
                .background(textColor)
                .clickable { audioPlayer.togglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = bgColor,
                modifier = Modifier.size(44.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(width = 66.dp, height = 54.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 26.dp, bottomEnd = 26.dp))
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 26.dp, bottomEnd = 26.dp))
                .clickable { audioPlayer.nextTrack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(
            onClick = { audioPlayer.toggleShuffle() },
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (isShuffle) (if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)) else Color.Transparent)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Aleatorio",
                tint = if (isShuffle) SonoraGold else subtextColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 4. Audiófilo Hi-Fi Streamline (Modern Squircle)
@Composable
private fun PlaybackControlsSquircle(
    isPlaying: Boolean,
    repeatMode: Int,
    isShuffle: Boolean,
    isDark: Boolean,
    textColor: Color,
    subtextColor: Color,
    bgColor: Color,
    audioPlayer: com.sonora.music.service.SonoraAudioPlayer
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) SonoraGold else (if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA)))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), RoundedCornerShape(14.dp))
                .clickable { audioPlayer.toggleRepeat() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = "Repetir",
                tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) Color.Black else subtextColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), RoundedCornerShape(18.dp))
                .clickable { audioPlayer.prevTrack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(textColor)
                .clickable { audioPlayer.togglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = bgColor,
                modifier = Modifier.size(42.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), RoundedCornerShape(18.dp))
                .clickable { audioPlayer.nextTrack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isShuffle) SonoraGold else (if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA)))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), RoundedCornerShape(14.dp))
                .clickable { audioPlayer.toggleShuffle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Aleatorio",
                tint = if (isShuffle) Color.Black else subtextColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 5. Dynamic Waveform Action Pill (Píldora Interactiva)
@Composable
private fun PlaybackControlsWaveform(
    isPlaying: Boolean,
    repeatMode: Int,
    isShuffle: Boolean,
    isDark: Boolean,
    textColor: Color,
    subtextColor: Color,
    bgColor: Color,
    audioPlayer: com.sonora.music.service.SonoraAudioPlayer
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { audioPlayer.toggleRepeat() },
            modifier = Modifier.size(46.dp)
        ) {
            Icon(
                imageVector = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = "Repetir",
                tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) SonoraGold else subtextColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), CircleShape)
                .clickable { audioPlayer.prevTrack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                tint = textColor,
                modifier = Modifier.size(30.dp)
            )
        }

        Box(
            modifier = Modifier
                .height(66.dp)
                .width(146.dp)
                .clip(RoundedCornerShape(33.dp))
                .background(textColor)
                .clickable { audioPlayer.togglePlay() }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = bgColor,
                    modifier = Modifier.size(36.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(28.dp)
                ) {
                    val barHeights = if (isPlaying) listOf(18.dp, 26.dp, 14.dp, 22.dp) else listOf(5.dp, 5.dp, 5.dp, 5.dp)
                    barHeights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(h)
                                .clip(RoundedCornerShape(2.dp))
                                .background(bgColor)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                .border(1.dp, if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD), CircleShape)
                .clickable { audioPlayer.nextTrack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                tint = textColor,
                modifier = Modifier.size(30.dp)
            )
        }

        IconButton(
            onClick = { audioPlayer.toggleShuffle() },
            modifier = Modifier.size(46.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Aleatorio",
                tint = if (isShuffle) SonoraGold else subtextColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
