package com.sonora.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonora.music.data.local.SonoraPreferences
import com.sonora.music.data.model.Playlist
import com.sonora.music.data.model.Song
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.components.Organic8PetalShape
import com.sonora.music.ui.theme.SonoraObsidianCard
import com.sonora.music.ui.theme.SonoraObsidianDark
import com.sonora.music.ui.theme.SonoraPaperBeige
import com.sonora.music.ui.theme.SonoraPaperCard

@Composable
fun PlaylistsScreen(
    allSongs: List<Song>,
    sonoraPrefs: SonoraPreferences,
    audioPlayer: SonoraAudioPlayer,
    onSongOptions: (Song) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) SonoraObsidianDark else SonoraPaperBeige
    val cardBg = if (isDark) SonoraObsidianCard else SonoraPaperCard
    val textColor = if (isDark) Color.White else Color(0xFF121212)
    val subtextColor = if (isDark) Color(0xFF8A857B) else Color(0xFF75726B)

    var activeDetailPlaylist by remember { mutableStateOf<Pair<String, List<Song>>?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var customPlaylists by remember { mutableStateOf(sonoraPrefs.getCustomPlaylists()) }

    val currentSong by audioPlayer.currentSong.collectAsState()

    if (activeDetailPlaylist != null) {
        val (name, songs) = activeDetailPlaylist!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeDetailPlaylist = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = textColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = textColor
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = textColor)
                        Text("${songs.size} canciones", fontSize = 12.sp, color = subtextColor)
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (songs.isNotEmpty()) audioPlayer.playSong(songs[0], songs)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reproducir Todo", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(songs, key = { it.id }) { song ->
                    val isCurrent = currentSong?.id == song.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCurrent) textColor else cardBg)
                            .clickable { audioPlayer.playSong(song, songs) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = song.coverUri ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=600&auto=format&fit=crop",
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCurrent) bgColor else textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, fontSize = 11.sp, color = if (isCurrent) bgColor.copy(alpha = 0.8f) else subtextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(song.durationFormatted, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isCurrent) bgColor else subtextColor)
                            IconButton(onClick = { onSongOptions(song) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = if (isCurrent) bgColor else subtextColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    } else {
        // Playlists Main Tab
        val likedIds = remember(allSongs) { sonoraPrefs.getLikedSongIds() }
        val likedSongs = remember(likedIds, allSongs) { allSongs.filter { likedIds.contains(it.id) } }

        val playCounts = remember(allSongs) { sonoraPrefs.getPlayCounts() }
        val topSongs = remember(playCounts, allSongs) {
            allSongs.filter { (playCounts[it.id] ?: 0) > 0 }
                .sortedByDescending { playCounts[it.id] ?: 0 }
                .take(25)
        }

        val recentIds = remember(allSongs) { sonoraPrefs.getRecentSongIds() }
        val recentSongs = remember(recentIds, allSongs) {
            recentIds.mapNotNull { id -> allSongs.firstOrNull { it.id == id } }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LISTAS INTELIGENTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = subtextColor
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .clickable { showCreateDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Crear Lista", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }
            }

            // 1. Smart Card: Favoritas
            item {
                SmartPlaylistCard(
                    title = "Favoritas",
                    subtitle = "${likedSongs.size} canciones guardadas",
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFFEF4444),
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    onClick = { activeDetailPlaylist = Pair("Favoritas", likedSongs) }
                )
            }

            // 2. Smart Card: Top 25
            item {
                SmartPlaylistCard(
                    title = "Top 25 Más Escuchadas",
                    subtitle = "${topSongs.size} temas más reproducidos",
                    icon = Icons.Default.Whatshot,
                    iconTint = Color(0xFFF59E0B),
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    onClick = { activeDetailPlaylist = Pair("Top 25 Más Escuchadas", topSongs) }
                )
            }

            // 3. Smart Card: Recientes
            item {
                SmartPlaylistCard(
                    title = "Recientes",
                    subtitle = "${recentSongs.size} canciones en historial",
                    icon = Icons.Default.History,
                    iconTint = Color(0xFF3B82F6),
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    onClick = { activeDetailPlaylist = Pair("Recientes", recentSongs) }
                )
            }

            // Custom Playlists Section
            item {
                Text(
                    text = "TUS LISTAS PERSONALIZADAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = subtextColor,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            if (customPlaylists.isEmpty()) {
                item {
                    Text(
                        text = "Aún no has creado listas. Toca '+ Crear Lista' arriba para crear una.",
                        fontSize = 12.sp,
                        color = subtextColor,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(customPlaylists) { pl ->
                    val plSongs = pl.songIds.mapNotNull { id -> allSongs.firstOrNull { it.id == id } }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .clickable { activeDetailPlaylist = Pair(pl.name, plSongs) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(pl.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Text("${plSongs.size} canciones", fontSize = 11.sp, color = subtextColor)
                            }
                        }

                        IconButton(
                            onClick = {
                                sonoraPrefs.deletePlaylist(pl.id)
                                customPlaylists = sonoraPrefs.getCustomPlaylists()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = subtextColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // Create Playlist Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Nueva Lista de Reproducción", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Nombre de la lista...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                sonoraPrefs.createPlaylist(newPlaylistName.trim())
                                customPlaylists = sonoraPrefs.getCustomPlaylists()
                                newPlaylistName = ""
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text("Crear", fontWeight = FontWeight.Bold, color = textColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancelar", color = subtextColor)
                    }
                }
            )
        }
    }
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    cardBg: Color,
    textColor: Color,
    subtextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(subtitle, fontSize = 11.sp, color = subtextColor)
            }
        }

        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
    }
}
