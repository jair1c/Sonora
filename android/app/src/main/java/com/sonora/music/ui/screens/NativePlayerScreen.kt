package com.sonora.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cerrar",
                        tint = textColor
                    )
                }

                Text(
                    text = "REPRODUCIENDO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = textColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Cola",
                            tint = textColor
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isLiked) Color(0xFFEF4444) else textColor
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = subtextColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = currentSong?.audioQualityBadge ?: "Hi-Fi Audio",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                }
            }

            // 3. Central Artwork & Wavy Scrubber
            Box(
                modifier = Modifier.size(310.dp),
                contentAlignment = Alignment.Center
            ) {
                WavyScrubberRing(
                    progressPercent = progress,
                    isDarkTheme = isDark,
                    onSeekPercent = { pct ->
                        audioPlayer.seekTo((pct * safeDuration).toLong())
                    }
                )

                // 8-Petal Vinyl Image
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(if (isPlaying) 1f else 0.85f)
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
                            .size(20.dp)
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${currentSong?.artist ?: "Offline"} • ${currentSong?.album ?: "Local"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = subtextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 5. Modern Playback Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat Button with Active Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { audioPlayer.toggleRepeat() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (repeatMode != Player.REPEAT_MODE_OFF) (if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repetir",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) textColor else subtextColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (repeatMode != Player.REPEAT_MODE_OFF) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(textColor)
                        )
                    }
                }

                // Skip Previous Button (Modern Circle)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                        .clickable { audioPlayer.prevTrack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Center Play/Pause Floating Action Circle (72dp Luxury)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(textColor)
                        .clickable { audioPlayer.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = bgColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Skip Next Button (Modern Circle)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                        .clickable { audioPlayer.nextTrack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Shuffle Button with Active Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { audioPlayer.toggleShuffle() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isShuffle) (if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (isShuffle) textColor else subtextColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (isShuffle) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(textColor)
                        )
                    }
                }
            }

            // 6. Dynamic Realtime Lyrics Preview Pill
            val lyrics = currentSong?.lyrics ?: emptyList()
            val currentLyricIdx = if (lyrics.isNotEmpty()) {
                lyrics.indexOfLast { currentPositionMs >= it.timeMs }.coerceAtLeast(0)
            } else 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                    .clickable { showExpandedLyrics = true }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                if (lyrics.isNotEmpty() && currentLyricIdx in lyrics.indices) {
                    val activeLine = lyrics[currentLyricIdx].text
                    val nextLine = lyrics.getOrNull(currentLyricIdx + 1)?.text
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "♪ $activeLine",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (nextLine != null) {
                            Text(
                                text = nextLine,
                                fontSize = 11.sp,
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
                        fontSize = 11.sp,
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
                onDismiss = { showQueueSheet = false }
            )
        }
    }
}

