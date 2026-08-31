package com.sonora.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonora.music.service.SonoraAudioPlayer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

@Composable
fun QueueBottomSheet(
    audioPlayer: SonoraAudioPlayer,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val textColor = themeColors.textPrimary
    val subtextColor = themeColors.textSecondary

    val currentSong by audioPlayer.currentSong.collectAsState()
    val playlist by audioPlayer.playlist.collectAsState()
    val listState = rememberLazyListState()

    val currentIdx = playlist.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)

    LaunchedEffect(currentIdx) {
        if (currentIdx in playlist.indices) {
            listState.animateScrollToItem(currentIdx)
        }
    }

    val hazeState = com.sonora.music.ui.theme.LocalHazeState.current ?: remember { HazeState() }
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val sheetGlassStyle = HazeStyle(
        blurRadius = 28.dp,
        tint = if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg.copy(alpha = 0.35f) else com.sonora.music.ui.theme.SonoraGlassLightBg.copy(alpha = 0.20f),
        noiseFactor = 0.05f
    )
    val sheetGlareBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.50f else 0.90f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clip(sheetShape)
                .then(
                    if (isGlass) {
                        Modifier
                            .hazeChild(state = hazeState, shape = sheetShape, style = sheetGlassStyle)
                            .background(if (isDark) Color(0xDC141D2B) else Color(0xEAFFFFFF))
                            .border(1.2.dp, sheetGlareBorder, sheetShape)
                    } else {
                        Modifier
                            .background(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
                            .border(1.dp, themeColors.borderCol, sheetShape)
                    }
                )
                .clickable(enabled = false) {}
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COLA DE REPRODUCCIÓN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = textColor
                    )
                    Text(
                        text = "${playlist.size} canciones en cola",
                        fontSize = 11.sp,
                        color = subtextColor
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = textColor)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playlist, key = { _, s -> s.id }) { idx, song ->
                    val isPlayingThis = song.id == currentSong?.id
                    val itemShape = RoundedCornerShape(16.dp)
                    val itemBorder = if (isGlass) (if (isDark) Brush.verticalGradient(listOf(Color(0x60FFFFFF), Color(0x15FFFFFF))) else Brush.verticalGradient(listOf(Color.White, Color(0x6094A3B8)))) else SolidColor(Color.Transparent)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(itemShape)
                            .then(
                                if (isGlass) {
                                    if (isPlayingThis) {
                                        Modifier
                                            .background(if (isDark) Brush.linearGradient(listOf(Color.White, Color(0xFFE2E8F0))) else Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
                                            .border(1.2.dp, Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.3f))), itemShape)
                                    } else {
                                        Modifier
                                            .background(if (isDark) Color(0x351E293B) else Color(0x75FFFFFF))
                                            .border(1.dp, itemBorder, itemShape)
                                    }
                                } else {
                                    Modifier
                                        .background(if (isPlayingThis) textColor else themeColors.cardBg)
                                        .border(1.dp, if (isPlayingThis) Color.Transparent else themeColors.borderCol, itemShape)
                                }
                            )
                            .clickable {
                                audioPlayer.playSong(song, playlist)
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isPlayingThis) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = if (isGlass) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.Black else Color.White),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 8.dp)
                                )
                            } else {
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subtextColor,
                                    modifier = Modifier.width(24.dp)
                                )
                            }

                            AsyncImage(
                                model = song.coverUri ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=600&auto=format&fit=crop",
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlayingThis && isGlass) (if (isDark) Color.Black else Color.White) else (if (isPlayingThis) (if (isDark) Color.Black else Color.White) else textColor),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 11.sp,
                                    color = if (isPlayingThis && isGlass) (if (isDark) Color(0xCC000000) else Color(0xCCE2E8F0)) else (if (isPlayingThis) (if (isDark) Color(0xCC000000) else Color(0xCCE2E8F0)) else subtextColor),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val durationMinutes = song.durationMs / 60000
                        val durationSeconds = (song.durationMs % 60000) / 1000
                        Text(
                            text = String.format(java.util.Locale.US, "%02d:%02d", durationMinutes, durationSeconds),
                            fontSize = 11.sp,
                            color = if (isPlayingThis && isGlass) (if (isDark) Color.Black else Color.White) else (if (isPlayingThis) (if (isDark) Color.Black else Color.White) else subtextColor),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
