package com.sonora.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.music.ui.components.SonoraSongCover
import coil.compose.AsyncImage
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.theme.SonoraObsidianCard
import com.sonora.music.ui.theme.SonoraObsidianDark
import com.sonora.music.ui.theme.SonoraPaperBeige
import com.sonora.music.ui.theme.SonoraPaperCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    audioPlayer: SonoraAudioPlayer,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val bgColor = if (isGlass) (if (isDark) Color(0xF4131E2C) else Color(0xF4F0F4F9)) else (if (isDark) SonoraObsidianDark else SonoraPaperBeige)
    val cardBg = if (isGlass) (if (isDark) Color(0x355A7EAA) else Color(0x70FFFFFF)) else (if (isDark) SonoraObsidianCard else SonoraPaperCard)
    val textColor = themeColors.textPrimary
    val subtextColor = themeColors.textSecondary

    val currentSong by audioPlayer.currentSong.collectAsState()
    val playlist by audioPlayer.playlist.collectAsState()
    val listState = rememberLazyListState()

    val currentIdx = playlist.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)

    // Auto-scroll to currently playing song in the queue
    LaunchedEffect(currentIdx) {
        if (currentIdx in playlist.indices) {
            listState.animateScrollToItem(currentIdx)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
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

            // Queue List
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playlist, key = { _, s -> s.id }) { idx, song ->
                    val isPlayingThis = song.id == currentSong?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isPlayingThis) textColor else cardBg)
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
                                    contentDescription = "Sonando",
                                    tint = bgColor,
                                    modifier = Modifier.size(20.dp).padding(end = 6.dp)
                                )
                            }
                            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))) {
                                SonoraSongCover(
                                    song = song,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = song.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlayingThis) bgColor else textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 11.sp,
                                    color = if (isPlayingThis) bgColor.copy(alpha = 0.8f) else subtextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = song.durationFormatted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPlayingThis) bgColor else subtextColor
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }
}
