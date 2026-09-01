package com.sonora.music.ui.screens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

import com.sonora.music.ui.components.PlaybackSpeedModal
import com.sonora.music.util.AudioFormatDetails

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import com.sonora.music.ui.theme.SonoraGold
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Usb
import com.sonora.music.service.OutputIconType
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
import com.sonora.music.ui.components.SonoraSongCover
import com.sonora.music.data.repository.SongCoverRepository
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
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val bgColor = if (isGlass) Color.Transparent else themeColors.bg
    val cardBg = themeColors.cardBg
    val borderCol = themeColors.borderCol
    val textColor = themeColors.textPrimary
    val subtextColor = themeColors.textSecondary

    val currentSong by audioPlayer.currentSong.collectAsState()
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentPositionMs by audioPlayer.currentPositionMs.collectAsState()
    val durationMs by audioPlayer.durationMs.collectAsState()
    val isShuffle by audioPlayer.isShuffle.collectAsState()
    val repeatMode by audioPlayer.repeatMode.collectAsState()
    val fftData by audioPlayer.visualizerManager.fftData.collectAsState()
    val currentSpeed by audioPlayer.playbackSpeed.collectAsState()
    val currentPitch by audioPlayer.playbackPitch.collectAsState()

    val hazeState = remember { HazeState() }
    var showExpandedLyrics by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSpeedModal by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isLiked by remember(currentSong) { mutableStateOf(currentSong?.let { sonoraPrefs.isSongLiked(it.id) } ?: false) }

    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var dragHorizontalAccumulator by remember { mutableFloatStateOf(0f) }
    var dragVerticalAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(seekFeedbackText) {
        if (seekFeedbackText != null) {
            kotlinx.coroutines.delay(900)
            seekFeedbackText = null
        }
    }

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

    val playerHazeState = remember { dev.chrisbanes.haze.HazeState() }
    val basePlayerBg = if (isGlass) (if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg else com.sonora.music.ui.theme.SonoraGlassLightBg) else themeColors.bg
    val playerBtnBrush = if (isGlass) {
        if (isDark) Brush.linearGradient(listOf(Color(0x606080A8), Color(0x35385070))) else Brush.linearGradient(listOf(Color(0xEEFFFFFF), Color(0xC0E2E8F0)))
    } else {
        SolidColor(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
    }
    val playerBtnBorder = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0xB5FFFFFF), Color(0x30FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x8094A3B8)))
    } else {
        SolidColor(borderCol)
    }

    val badgeBrush = if (isGlass) {
        if (isDark) Brush.linearGradient(listOf(Color(0x456080A8), Color(0x25385070))) else Brush.linearGradient(listOf(Color(0xDDFFFFFF), Color(0xB5E2E8F0)))
    } else {
        SolidColor(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
    }
    val badgeBorder = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x20FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x80CBD5E1)))
    } else {
        SolidColor(borderCol)
    }

    val lyricsPillBrush = if (isGlass) {
        if (isDark) Brush.linearGradient(listOf(Color(0x55283C56), Color(0x381D2C40))) else Brush.linearGradient(listOf(Color(0xEEFFFFFF), Color(0xC8E8EEF6)))
    } else {
        SolidColor(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
    }
    val lyricsPillBorder = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0x95FFFFFF), Color(0x25FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x80CBD5E1)))
    } else {
        SolidColor(borderCol)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(basePlayerBg)
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
    ) {
        if (isGlass) {
            com.sonora.music.ui.theme.LiquidGlassBackdrop(isDark = isDark)
        }
        val playerGlassStyle = HazeStyle(
            blurRadius = 24.dp,
            tint = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
            noiseFactor = 0.05f
        )
        val playerGlassGlareBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.05f)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isGlass) Modifier.haze(state = playerHazeState) else Modifier)
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                        .then(
                            if (isGlass) {
                                Modifier
                                    .hazeChild(state = hazeState, shape = CircleShape, style = playerGlassStyle)
                                    .background(if (isDark) Color(0x351E293B) else Color(0x70FFFFFF))
                                    .border(1.2.dp, if (isDark) playerGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x8094A3B8))), CircleShape)
                            } else {
                                Modifier
                                    .background(playerBtnBrush)
                                    .border(1.dp, playerBtnBorder, CircleShape)
                            }
                        )
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
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = textColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .then(
                                if (isGlass) {
                                    Modifier
                                        .hazeChild(state = hazeState, shape = CircleShape, style = playerGlassStyle)
                                        .background(if (isDark) Color(0x351E293B) else Color(0x70FFFFFF))
                                        .border(1.2.dp, if (isDark) playerGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x8094A3B8))), CircleShape)
                                } else {
                                    Modifier
                                        .background(playerBtnBrush)
                                        .border(1.dp, playerBtnBorder, CircleShape)
                                }
                            )
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
                            .then(
                                if (isGlass) {
                                    Modifier
                                        .hazeChild(state = hazeState, shape = CircleShape, style = playerGlassStyle)
                                        .background(if (isDark) Color(0x351E293B) else Color(0x70FFFFFF))
                                        .border(1.2.dp, if (isDark) playerGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x8094A3B8))), CircleShape)
                                } else {
                                    Modifier
                                        .background(playerBtnBrush)
                                        .border(1.dp, playerBtnBorder, CircleShape)
                                }
                            )
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
                val formatDetails by audioPlayer.realAudioFormat.collectAsState()


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val badgeText = formatDetails?.formattedString ?: currentSong?.audioQualityBadge ?: "Hi-Fi Audio"
                    val isHiRes = formatDetails?.isHiRes == true || badgeText.contains("FLAC", ignoreCase = true) || badgeText.contains("Hi-Res", ignoreCase = true) || badgeText.contains("24-bit", ignoreCase = true) || badgeText.contains("DSD", ignoreCase = true)
                    val hiResTextColor = if (isHiRes) (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)) else textColor
                    val hiResBgColor = if (isHiRes) (if (isDark) Color(0x35D97706) else Color(0x30F59E0B)) else (if (isDark) Color(0x351E293B) else Color(0x70FFFFFF))
                    val hiResBorderBrush = if (isHiRes) SolidColor(if (isDark) Color(0xFFFBBF24).copy(alpha = 0.6f) else Color(0xFFF59E0B).copy(alpha = 0.7f)) else (if (isDark) playerGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x8094A3B8))))

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isGlass) {
                                    Modifier
                                        .hazeChild(state = hazeState, shape = RoundedCornerShape(10.dp), style = playerGlassStyle)
                                        .background(hiResBgColor)
                                        .border(1.2.dp, hiResBorderBrush, RoundedCornerShape(10.dp))
                                } else {
                                    Modifier
                                        .background(if (isHiRes) SolidColor(Color(0xFFD97706).copy(alpha = 0.22f)) else badgeBrush)
                                        .border(1.dp, if (isHiRes) SolidColor(Color(0xFFD97706).copy(alpha = 0.6f)) else badgeBorder, RoundedCornerShape(10.dp))
                                }
                            )
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Default,
                            letterSpacing = 0.4.sp,
                            color = hiResTextColor
                        )
                    }

                    // Speed Pill Button
                    val isCustomSpeed = currentSpeed != 1.0f || currentPitch != 1.0f
                    val speedTextColor = if (isCustomSpeed) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else subtextColor
                    val speedBgColor = if (isCustomSpeed) (if (isDark) Color(0x35059669) else Color(0x3010B981)) else (if (isDark) Color(0x351E293B) else Color(0x70FFFFFF))
                    val speedBorderBrush = if (isCustomSpeed) SolidColor(if (isDark) Color(0xFF34D399).copy(alpha = 0.6f) else Color(0xFF10B981).copy(alpha = 0.7f)) else (if (isDark) playerGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x8094A3B8))))

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isGlass) {
                                    Modifier
                                        .hazeChild(state = hazeState, shape = RoundedCornerShape(10.dp), style = playerGlassStyle)
                                        .background(speedBgColor)
                                        .border(1.2.dp, speedBorderBrush, RoundedCornerShape(10.dp))
                                } else {
                                    Modifier
                                        .background(if (isCustomSpeed) SolidColor(Color(0xFF10B981).copy(alpha = 0.22f)) else badgeBrush)
                                        .border(1.dp, if (isCustomSpeed) SolidColor(Color(0xFF10B981).copy(alpha = 0.6f)) else badgeBorder, RoundedCornerShape(10.dp))
                                }
                            )
                            .clickable { showSpeedModal = true }
                            .padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentSpeed != 1.0f || currentPitch != 1.0f) "⚡ ${String.format(java.util.Locale.US, "%.2fx", currentSpeed)}" else "1.0x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Default,
                            letterSpacing = 0.4.sp,
                            color = speedTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Audio Spectrum Visualizer
                LiveAudioSpectrum(
                    fftData = fftData,
                    isPlaying = isPlaying,
                    isDark = isDark
                )
            }

            // 3. Central Artwork & Wavy Scrubber (20% proportioned clearance)
            Box(
                modifier = Modifier.size(350.dp),
                contentAlignment = Alignment.Center
            ) {
                WavyScrubberRing(
                    progressPercent = progress,
                    isDarkTheme = isDark,
                    onSeekPercent = { pct ->
                        audioPlayer.seekTo((pct * safeDuration).toLong())
                    }
                )

                // 8-Petal Vinyl Image (Expanded flower is 20% smaller than scrubber ring contour)
                Box(
                    modifier = Modifier
                        .size(242.dp)
                        .scale(flowerScale)
                        .clip(Organic8PetalShape(petalCount = 8, amplitude = 0.08f))
                        .background(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { audioPlayer.togglePlay() },
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2f) {
                                        val targetPos = (currentPositionMs - 10000L).coerceAtLeast(0L)
                                        audioPlayer.seekTo(targetPos)
                                        seekFeedbackText = "⏪ -10s"
                                    } else {
                                        val targetPos = (currentPositionMs + 10000L).coerceAtMost(safeDuration)
                                        audioPlayer.seekTo(targetPos)
                                        seekFeedbackText = "⏩ +10s"
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { _, dragAmount ->
                                    dragHorizontalAccumulator += dragAmount.x
                                    dragVerticalAccumulator += dragAmount.y
                                },
                                onDragEnd = {
                                    if (dragHorizontalAccumulator > 100f) {
                                        // Swipe right -> Previous track
                                        audioPlayer.prevTrack()
                                    } else if (dragHorizontalAccumulator < -100f) {
                                        // Swipe left -> Next track
                                        audioPlayer.nextTrack()
                                    } else if (dragVerticalAccumulator < -100f) {
                                        // Swipe up -> Open Queue
                                        showQueueSheet = true
                                    }
                                    dragHorizontalAccumulator = 0f
                                    dragVerticalAccumulator = 0f
                                },
                                onDragCancel = {
                                    dragHorizontalAccumulator = 0f
                                    dragVerticalAccumulator = 0f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().rotate(if (isPlaying) rotationAngle else 0f)) {
                        SonoraSongCover(
                            song = currentSong,
                            contentDescription = currentSong?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Vinyl center pinhole
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                    )

                    // Double Tap Transient Indicator Overlay
                    if (seekFeedbackText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = seekFeedbackText ?: "",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // 4. Title, Artist & Audio Output Device Glass Pill
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
                Spacer(modifier = Modifier.height(8.dp))

                // Subtle Audio Output Device Glass Pill
                val outputDevice by audioPlayer.currentOutputDevice.collectAsState()
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .then(
                            if (isGlass) {
                                Modifier
                                    .hazeChild(state = hazeState, shape = RoundedCornerShape(100.dp), style = playerGlassStyle)
                                    .background(if (isDark) Color(0x351E293B) else Color(0x60FFFFFF))
                                    .border(1.dp, if (isDark) playerGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x6094A3B8))), RoundedCornerShape(100.dp))
                            } else {
                                Modifier
                                    .background(badgeBrush)
                                    .border(1.dp, badgeBorder, RoundedCornerShape(100.dp))
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val icon = when (outputDevice.iconType) {
                        OutputIconType.BLUETOOTH -> Icons.Default.Bluetooth
                        OutputIconType.HEADPHONES -> Icons.Default.Headphones
                        OutputIconType.USB -> Icons.Default.Usb
                        OutputIconType.SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = outputDevice.name,
                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = outputDevice.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.85f),
                        letterSpacing = 0.3.sp
                    )
                }
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

            val lyricsGlassBg = if (isDark) Color(0x351E293B) else Color(0x75FFFFFF)
            val lyricsGlassBorder = if (isDark) playerGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x8094A3B8)))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (isGlass) {
                            Modifier
                                .hazeChild(state = hazeState, shape = RoundedCornerShape(20.dp), style = playerGlassStyle)
                                .background(lyricsGlassBg)
                                .border(1.2.dp, lyricsGlassBorder, RoundedCornerShape(20.dp))
                        } else {
                            Modifier
                                .background(lyricsPillBrush)
                                .border(1.dp, SolidColor(borderCol), RoundedCornerShape(20.dp))
                        }
                    )
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

        // Fullscreen Synchronized Lyrics Overlay with Real Haze Glass & Touch Event Blocking
        if (showExpandedLyrics) {
            androidx.activity.compose.BackHandler { showExpandedLyrics = false }
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

            val lyricsGlassStyle = dev.chrisbanes.haze.HazeStyle(
                blurRadius = 36.dp,
                tint = if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg.copy(alpha = 0.55f) else com.sonora.music.ui.theme.SonoraGlassLightBg.copy(alpha = 0.45f),
                noiseFactor = 0.05f
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isGlass) {
                            Modifier
                                .hazeChild(state = playerHazeState, shape = RoundedCornerShape(0.dp), style = lyricsGlassStyle)
                                .background(if (isDark) Color(0x650F172A) else Color(0x95FFFFFF))
                        } else {
                            Modifier.background(if (isDark) Color(0xFF121110) else Color(0xFFFAF7F0))
                        }
                    )
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consumes all taps so nothing behind is triggered
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LETRAS SINCRONIZADAS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = textColor
                            )
                            Text(
                                text = currentSong?.title ?: "",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!currentSong?.artist.isNullOrBlank()) {
                                Text(
                                    text = currentSong?.artist ?: "",
                                    fontSize = 11.sp,
                                    color = subtextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { showExpandedLyrics = false },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isGlass) {
                                        Modifier
                                            .background(if (isDark) Color(0x351E293B) else Color(0x80FFFFFF))
                                            .border(1.2.dp, if (isDark) playerGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x8094A3B8))), CircleShape)
                                    } else {
                                        Modifier
                                            .background(playerBtnBrush)
                                            .border(1.dp, playerBtnBorder, CircleShape)
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Cerrar",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (lyrics.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 100.dp, bottom = 200.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    } else {
                        // Empty State when no .lrc is found
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(if (isGlass) (if (isDark) Color(0x30FFFFFF) else Color(0x80FFFFFF)) else (if (isDark) Color(0xFF22201C) else Color(0xFFE6E1D5)))
                                        .border(1.dp, if (isGlass) (if (isDark) Color(0x45FFFFFF) else Color(0xB5FFFFFF)) else borderCol, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                Text(
                                    text = "Sin Letras Sincronizadas",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "No se encontró un archivo .lrc sincronizado para esta canción.\nPuedes añadir un archivo .lrc con el mismo nombre en la carpeta de la canción.",
                                    fontSize = 12.sp,
                                    color = subtextColor,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
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

        PlaybackSpeedModal(
            isOpen = showSpeedModal,
            onClose = { showSpeedModal = false },
            currentSpeed = currentSpeed,
            currentPitch = currentPitch,
            onApply = { speed, pitch ->
                audioPlayer.setPlaybackParameters(speed, pitch)
                showSpeedModal = false
            },
            isDark = isDark
        )
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
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val cardBg = themeColors.cardBg
    val borderCol = themeColors.borderCol

    val dockContainerBrush = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0xFF263A52), Color(0xFF1B2A3E))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE5ECF4)))
    } else {
        SolidColor(if (isDark) Color(0xFF161513) else Color(0xFFEAE5DA))
    }
    val dockContainerBorder = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0xB5FFFFFF), Color(0x35FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x90CBD5E1)))
    } else {
        SolidColor(if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(dockContainerBrush)
            .border(if (isGlass) 1.5.dp else 1.dp, dockContainerBorder, RoundedCornerShape(40.dp))
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

        val dockPlayBrush = if (isGlass) {
            if (isDark) Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD8E4F0))) else Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
        } else {
            SolidColor(textColor)
        }
        val dockPlayIconTint = if (isGlass) (if (isDark) Color(0xFF0A0C10) else Color.White) else (if (isDark) Color(0xFF0F0E0D) else Color(0xFFF5F2EA))

        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(dockPlayBrush)
                .clickable { audioPlayer.togglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = dockPlayIconTint,
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
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val cardBg = themeColors.cardBg
    val borderCol = themeColors.borderCol

    val circleBtnBrush = if (isGlass) {
        if (isDark) Brush.linearGradient(listOf(Color(0x606080A8), Color(0x35385070))) else Brush.linearGradient(listOf(Color(0xEEFFFFFF), Color(0xC0E2E8F0)))
    } else {
        SolidColor(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
    }
    val circleBtnBorder = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0xB5FFFFFF), Color(0x30FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x8094A3B8)))
    } else {
        SolidColor(if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD))
    }

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
                .background(circleBtnBrush)
                .border(
                    if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) 2.dp else (if (isGlass) 1.2.dp else 1.dp),
                    if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) SolidColor(SonoraGold) else circleBtnBorder,
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
                .background(circleBtnBrush)
                .border(if (isGlass) 1.2.dp else 1.dp, circleBtnBorder, CircleShape)
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

        val circlePlayBrush = if (isGlass) {
            if (isDark) Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD8E4F0))) else Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
        } else {
            SolidColor(textColor)
        }

        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(circlePlayBrush)
                .clickable { audioPlayer.togglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = if (isGlass) (if (isDark) Color(0xFF0A0C10) else Color.White) else (if (isDark) Color(0xFF0F0E0D) else Color(0xFFF5F2EA)),
                modifier = Modifier.size(46.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(circleBtnBrush)
                .border(if (isGlass) 1.2.dp else 1.dp, circleBtnBorder, CircleShape)
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
                .background(circleBtnBrush)
                .border(
                    if (isShuffle) 2.dp else (if (isGlass) 1.2.dp else 1.dp),
                    if (isShuffle) SolidColor(SonoraGold) else circleBtnBorder,
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
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val cardBg = themeColors.cardBg
    val borderCol = themeColors.borderCol

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

        val organicPlayBrush = if (isGlass) {
            if (isDark) Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD8E4F0))) else Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
        } else {
            SolidColor(textColor)
        }
        val organicPlayIconTint = if (isGlass) (if (isDark) Color(0xFF0A0C10) else Color.White) else (if (isDark) Color(0xFF0F0E0D) else Color(0xFFF5F2EA))

        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(Organic8PetalShape(petalCount = 8, amplitude = 0.12f))
                .background(organicPlayBrush)
                .clickable { audioPlayer.togglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = organicPlayIconTint,
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
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val cardBg = themeColors.cardBg
    val borderCol = themeColors.borderCol

    val squircleBtnBrush = if (isGlass) {
        if (isDark) Brush.linearGradient(listOf(Color(0x606080A8), Color(0x35385070))) else Brush.linearGradient(listOf(Color(0xEEFFFFFF), Color(0xC0E2E8F0)))
    } else {
        SolidColor(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
    }
    val squircleBtnBorder = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0xB5FFFFFF), Color(0x30FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x8094A3B8)))
    } else {
        SolidColor(if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD))
    }

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
                .background(if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) SolidColor(SonoraGold) else squircleBtnBrush)
                .border(if (isGlass) 1.2.dp else 1.dp, squircleBtnBorder, RoundedCornerShape(14.dp))
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
                .background(squircleBtnBrush)
                .border(if (isGlass) 1.2.dp else 1.dp, squircleBtnBorder, RoundedCornerShape(18.dp))
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

        val squirclePlayBrush = if (isGlass) {
            if (isDark) Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD8E4F0))) else Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
        } else {
            SolidColor(textColor)
        }

        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(squirclePlayBrush)
                .clickable { audioPlayer.togglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = if (isGlass) (if (isDark) Color(0xFF0A0C10) else Color.White) else (if (isDark) Color(0xFF0F0E0D) else Color(0xFFF5F2EA)),
                modifier = Modifier.size(42.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(squircleBtnBrush)
                .border(if (isGlass) 1.2.dp else 1.dp, squircleBtnBorder, RoundedCornerShape(18.dp))
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
                .background(if (isShuffle) SolidColor(SonoraGold) else squircleBtnBrush)
                .border(if (isGlass) 1.2.dp else 1.dp, squircleBtnBorder, RoundedCornerShape(14.dp))
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
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val cardBg = themeColors.cardBg
    val borderCol = themeColors.borderCol

    val waveBtnBrush = if (isGlass) {
        if (isDark) Brush.linearGradient(listOf(Color(0x606080A8), Color(0x35385070))) else Brush.linearGradient(listOf(Color(0xEEFFFFFF), Color(0xC0E2E8F0)))
    } else {
        SolidColor(if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA))
    }
    val waveBtnBorder = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0xB5FFFFFF), Color(0x30FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x8094A3B8)))
    } else {
        SolidColor(if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD))
    }

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
                .background(waveBtnBrush)
                .border(if (isGlass) 1.2.dp else 1.dp, waveBtnBorder, CircleShape)
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

        val wavePlayBrush = if (isGlass) {
            if (isDark) Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD8E4F0))) else Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
        } else {
            SolidColor(textColor)
        }
        val wavePlayContentTint = if (isGlass) (if (isDark) Color(0xFF0A0C10) else Color.White) else bgColor

        Box(
            modifier = Modifier
                .height(66.dp)
                .width(146.dp)
                .clip(RoundedCornerShape(33.dp))
                .background(wavePlayBrush)
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
                    tint = wavePlayContentTint,
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
                                .background(wavePlayContentTint)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(waveBtnBrush)
                .border(if (isGlass) 1.2.dp else 1.dp, waveBtnBorder, CircleShape)
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

@Composable
fun LiveAudioSpectrum(
    fftData: FloatArray,
    isPlaying: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = if (isDark) SonoraGold else Color(0xFF1E1B16)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        fftData.forEachIndexed { idx, mag ->
            val effectiveMag = if (isPlaying) mag else 0.04f
            val targetHeight = if (isPlaying) (effectiveMag * 22f).coerceIn(3.5f, 22f) else 2.5f
            val animatedHeight by animateFloatAsState(
                targetValue = targetHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "spec_bar_$idx"
            )
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        barColor.copy(
                            alpha = if (isPlaying) (0.4f + effectiveMag * 0.6f).coerceIn(0.4f, 1f) else 0.2f
                        )
                    )
            )
        }
    }
}
