package com.sonora.music.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sonora.music.data.local.SonoraPreferences
import com.sonora.music.data.model.Playlist
import com.sonora.music.data.model.Song
import com.sonora.music.service.SonoraAudioPlayer

@Composable
fun PlaylistsScreen(
    allSongs: List<Song>,
    audioPlayer: SonoraAudioPlayer,
    sonoraPrefs: SonoraPreferences,
    isDark: Boolean,
    onOpenPlayer: () -> Unit
) {
    val context = LocalContext.current

    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val bgColor = if (isGlass) Color.Transparent else themeColors.bg
    val cardBg = themeColors.cardBg
    val subCardBg = themeColors.subCardBg
    val borderCol = themeColors.borderCol
    val textPrimary = themeColors.textPrimary
    val textSecondary = themeColors.textSecondary
    val activePillBg = themeColors.activePillBg
    val activePillText = themeColors.activePillText

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var customPlaylists by remember { mutableStateOf<List<Playlist>>(sonoraPrefs.getCustomPlaylists()) }

    // Smart playlists data
    val likedIds = sonoraPrefs.getLikedSongIds()
    val favoriteSongs = remember(allSongs, likedIds) {
        allSongs.filter { likedIds.contains(it.id) }
    }

    val playCounts = sonoraPrefs.getPlayCounts()
    val top25Songs = remember(allSongs, playCounts) {
        allSongs.filter { (playCounts[it.id] ?: 0) > 0 }
            .sortedByDescending { playCounts[it.id] ?: 0 }
            .take(25)
    }

    val recentIds = sonoraPrefs.getRecentSongIds()
    val recentSongs = remember(allSongs, recentIds) {
        recentIds.mapNotNull { id -> allSongs.firstOrNull { it.id == id } }
    }

    val hiResSongs = remember(allSongs) {
        allSongs.filter { song ->
            val path = song.filePath.lowercase()
            path.endsWith(".flac") || path.endsWith(".wav") || path.endsWith(".alac") ||
            path.endsWith(".aiff") || path.endsWith(".dsd") || path.endsWith(".opus")
        }
    }

    val newlyAddedSongs = remember(allSongs) {
        allSongs.sortedByDescending { it.dateAdded }.take(30)
    }

    val forgottenSongs = remember(allSongs, playCounts) {
        allSongs.filter { (playCounts[it.id] ?: 0) == 0 }.shuffled().take(25)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
    ) {
        // Section Header: TUS LISTAS DE REPRODUCCIÓN + [ + Nueva Lista ]
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TUS LISTAS DE REPRODUCCIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = textSecondary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(subCardBg)
                        .border(1.dp, borderCol, RoundedCornerShape(100.dp))
                        .clickable { showCreateDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+ Nueva Lista",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }
        }

        // 1. SMART PLAYLIST: Favoritos ♡
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                    .clickable {
                        if (favoriteSongs.isNotEmpty()) {
                            audioPlayer.playSong(favoriteSongs[0], favoriteSongs)
                            onOpenPlayer()
                        } else {
                            Toast.makeText(context, "Aún no tienes canciones favoritas", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favoritos",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Favoritos ♡",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${favoriteSongs.size} canciones",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                // Play circle button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. SMART PLAYLIST: Top 25
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                    .clickable {
                        if (top25Songs.isNotEmpty()) {
                            audioPlayer.playSong(top25Songs[0], top25Songs)
                            onOpenPlayer()
                        } else {
                            Toast.makeText(context, "Escucha canciones para generar tu Top 25", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Top 25",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Top 25 Más Escuchadas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${top25Songs.size} temas más reproducidos",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 3. SMART PLAYLIST: Recientes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                    .clickable {
                        if (recentSongs.isNotEmpty()) {
                            audioPlayer.playSong(recentSongs[0], recentSongs)
                            onOpenPlayer()
                        } else {
                            Toast.makeText(context, "Aún no hay historial de reproducción reciente", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recientes",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Recientes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${recentSongs.size} canciones en historial",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. SMART PLAYLIST: Joyas Hi-Res / FLAC
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                    .clickable {
                        if (hiResSongs.isNotEmpty()) {
                            audioPlayer.playSong(hiResSongs[0], hiResSongs)
                            onOpenPlayer()
                        } else {
                            Toast.makeText(context, "No se encontraron archivos FLAC/Hi-Res en tu dispositivo", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0D9488)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Joyas Hi-Res",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Joyas Hi-Res & FLAC",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${hiResSongs.size} pistas sin pérdida de alta fidelidad",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 5. SMART PLAYLIST: Añadidas Recientemente
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                    .clickable {
                        if (newlyAddedSongs.isNotEmpty()) {
                            audioPlayer.playSong(newlyAddedSongs[0], newlyAddedSongs)
                            onOpenPlayer()
                        } else {
                            Toast.makeText(context, "Biblioteca vacía", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF6366F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Añadidas Recientemente",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Añadidas Recientemente",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${newlyAddedSongs.size} temas nuevos",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 6. SMART PLAYLIST: Olvidadas / Redescubrir
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                    .clickable {
                        if (forgottenSongs.isNotEmpty()) {
                            audioPlayer.playSong(forgottenSongs[0], forgottenSongs)
                            onOpenPlayer()
                        } else {
                            Toast.makeText(context, "¡Ya has escuchado todas las canciones de tu biblioteca!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Redescubrir",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Redescubrir / Olvidadas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${forgottenSongs.size} canciones nunca escuchadas",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. CUSTOM PLAYLISTS
        if (customPlaylists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LISTAS PERSONALIZADAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
            }

            items(customPlaylists) { playlist ->
                val plSongs = allSongs.filter { playlist.songIds.contains(it.id) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = playlist.name.firstOrNull()?.toString()?.uppercase() ?: "P",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = playlist.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${plSongs.size} canciones",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Play button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .clickable {
                                    if (plSongs.isNotEmpty()) {
                                        audioPlayer.playSong(plSongs[0], plSongs)
                                        onOpenPlayer()
                                    } else {
                                        Toast.makeText(context, "Esta lista está vacía", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Delete button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(subCardBg)
                                .clickable {
                                    sonoraPrefs.deleteCustomPlaylist(playlist.id)
                                    customPlaylists = sonoraPrefs.getCustomPlaylists()
                                    Toast.makeText(context, "Lista eliminada", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Custom Organic Luxury Create Playlist Dialog
    if (showCreateDialog) {
        Dialog(
            onDismissRequest = { showCreateDialog = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (isDark) Color(0xFF161513) else Color(0xFFF5F2EA))
                    .border(1.dp, borderCol, RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Circular Icon Badge
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF22201C) else Color(0xFFE6E1D5))
                            .border(1.dp, borderCol, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NUEVA LISTA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Crea una lista personalizada para tus canciones",
                            fontSize = 11.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Luxury Styled Capsule TextField
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Nombre de la lista...", color = textSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF1E1C19) else Color(0xFFEBE6DC),
                            unfocusedContainerColor = if (isDark) Color(0xFF1E1C19) else Color(0xFFEBE6DC),
                            focusedBorderColor = textPrimary,
                            unfocusedBorderColor = borderCol,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (isDark) Color(0xFF1E1C19) else Color(0xFFE6E1D5))
                                .border(1.dp, borderCol, RoundedCornerShape(100.dp))
                                .clickable {
                                    newPlaylistName = ""
                                    showCreateDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancelar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }

                        // Create Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (isDark) Color.White else Color(0xFF121212))
                                .clickable {
                                    if (newPlaylistName.isNotBlank()) {
                                        sonoraPrefs.createCustomPlaylist(newPlaylistName.trim())
                                        customPlaylists = sonoraPrefs.getCustomPlaylists()
                                        newPlaylistName = ""
                                        showCreateDialog = false
                                        Toast.makeText(context, "Lista creada con éxito", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Crear Lista",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
