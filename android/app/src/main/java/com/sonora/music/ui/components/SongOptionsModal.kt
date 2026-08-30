package com.sonora.music.ui.components

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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.music.ui.components.SonoraSongCover
import coil.compose.AsyncImage
import com.sonora.music.data.model.Playlist
import com.sonora.music.data.model.Song
import com.sonora.music.ui.theme.SonoraObsidianCard
import com.sonora.music.ui.theme.SonoraObsidianDark
import com.sonora.music.ui.theme.SonoraPaperBeige
import com.sonora.music.ui.theme.SonoraPaperCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsModal(
    song: Song,
    playlists: List<Playlist>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onBlacklistFolder: (String) -> Unit
) {
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val bgCard = if (isGlass) (if (isDark) Color(0x351E293B) else Color(0xCCFFFFFF)) else (if (isDark) Color(0xFF161513) else Color(0xFFF5F2EA))
    val cardBg = themeColors.subCardBg
    val borderCol = themeColors.borderCol
    val textColor = themeColors.textPrimary
    val subtextColor = themeColors.textSecondary

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgCard,
        contentColor = textColor,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color(0xFF3E3B35) else Color(0xFFC0BAB0))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header with song info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = song.coverUri ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=600&auto=format&fit=crop",
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.album}",
                        fontSize = 12.sp,
                        color = subtextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.audioQualityBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = borderCol, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            if (!showPlaylistPicker && !showDetailsDialog) {
                // Action options
                OptionRow(
                    icon = Icons.Default.QueueMusic,
                    title = "Reproducir siguiente",
                    onClick = {
                        onPlayNext()
                        onDismiss()
                    },
                    textColor = textColor
                )

                OptionRow(
                    icon = Icons.Default.PlaylistAdd,
                    title = "Agregar a lista de reproducción",
                    onClick = { showPlaylistPicker = true },
                    textColor = textColor
                )

                OptionRow(
                    icon = Icons.Default.Person,
                    title = "Ver artista (${song.artist})",
                    onClick = {
                        onNavigateToArtist(song.artist)
                        onDismiss()
                    },
                    textColor = textColor
                )

                OptionRow(
                    icon = Icons.Default.Album,
                    title = "Ver álbum (${song.album})",
                    onClick = {
                        onNavigateToAlbum(song.album)
                        onDismiss()
                    },
                    textColor = textColor
                )

                OptionRow(
                    icon = Icons.Default.Info,
                    title = "Detalles técnicos de archivo",
                    onClick = { showDetailsDialog = true },
                    textColor = textColor
                )

                val folderName = song.filePath.split("/").let { if (it.size > 1) it[it.size - 2] else song.album }
                OptionRow(
                    icon = Icons.Default.VisibilityOff,
                    title = "Ocultar carpeta ($folderName)",
                    onClick = {
                        onBlacklistFolder(folderName)
                        onDismiss()
                    },
                    textColor = Color(0xFFEF4444)
                )
            } else if (showPlaylistPicker) {
                // Select Playlist view
                Text(
                    text = "AGREGAR A LISTA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = textColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (playlists.isEmpty()) {
                    Text(
                        text = "No tienes listas personalizadas aún. Puedes crear una en la pestaña 'Listas ♡'.",
                        fontSize = 12.sp,
                        color = subtextColor,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(playlists) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onAddToPlaylist(pl.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = textColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(pl.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }
                }
            } else {
                // Technical Details View
                Text(
                    text = "DETALLES DEL ARCHIVO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = textColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val sizeMb = "%.2f MB".format(song.sizeBytes.toFloat() / (1024f * 1024f))
                TechnicalRow(label = "Calidad / Códec", value = song.audioQualityBadge, textColor = textColor, subtextColor = subtextColor)
                TechnicalRow(label = "Duración", value = song.durationFormatted, textColor = textColor, subtextColor = subtextColor)
                TechnicalRow(label = "Tamaño en disco", value = sizeMb, textColor = textColor, subtextColor = subtextColor)
                TechnicalRow(label = "Ruta de archivo", value = song.filePath, textColor = textColor, subtextColor = subtextColor)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = textColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
private fun TechnicalRow(
    label: String,
    value: String,
    textColor: Color,
    subtextColor: Color
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subtextColor)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}
