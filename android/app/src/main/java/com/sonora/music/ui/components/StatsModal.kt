package com.sonora.music.ui.components

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sonora.music.data.local.SonoraPreferences
import com.sonora.music.data.model.Song



@Composable
fun StatsModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    songs: List<Song>,
    isDark: Boolean,
    sonoraPrefs: SonoraPreferences? = null,
    onPlaySong: (Song) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val prefs = remember(sonoraPrefs) { sonoraPrefs ?: SonoraPreferences(context) }
    val playCounts = remember(songs, isOpen) { prefs.getPlayCounts() }

    val songsWithPlays: List<Song> = remember(songs, playCounts) {
        songs.map { song ->
            val count = playCounts[song.id] ?: song.playCount
            if (count != song.playCount) song.copy(playCount = count) else song
        }
    }

    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val bgCard = if (isGlass) (if (isDark) Color(0xF610141D) else Color(0xF8F0F4F8)) else (if (isDark) Color(0xFF161513) else Color(0xFFF5F2EA))
    val borderCol = if (isGlass) (if (isDark) Color(0x45FFFFFF) else Color(0xB5FFFFFF)) else (if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD))
    val textPrimary = themeColors.textPrimary
    val textSecondary = themeColors.textSecondary
    val subCardBg = themeColors.subCardBg

    // Calculate stats accurately
    val totalPlayedSongs: Int = songsWithPlays.count { it.playCount > 0 }
    val totalMinutes: Int = prefs.getTotalListeningMinutes()
    val hours: Int = totalMinutes / 60
    val minutes: Int = totalMinutes % 60
    val topTracks: List<Song> = songsWithPlays.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(5)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(bgCard)
                    .border(1.dp, borderCol, RoundedCornerShape(28.dp))
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White else Color(0xFF121212)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Estadísticas",
                                tint = if (isDark) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "sonoraStats",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Tus estadísticas locales de escucha",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, borderCol, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Overview 2-column cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Time Listened
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "TIEMPO TOTAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                        Text(
                            text = "escuchados",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    // Songs Played
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "CANCIÓN ACTIVAS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalPlayedSongs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                        Text(
                            text = "con reproducciones",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Top 5 Most Played Tracks
                Text(
                    text = "TOP 5 MÁS ESCUCHADAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (topTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no has reproducido canciones. ¡Dale play a tu música!",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topTracks.forEachIndexed { index, song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(subCardBg)
                                    .clickable {
                                        onPlaySong(song)
                                        onClose()
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (index == 0) Color(0xFFC5A059) else textSecondary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Gray.copy(alpha = 0.2f))
                                ) {
                                    SonoraSongCover(
                                        song = song,
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        fontSize = 10.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "${song.playCount}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
