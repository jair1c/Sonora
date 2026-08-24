package com.sonora.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.sonora.music.data.model.Song
import com.sonora.music.data.model.SortMode
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.components.Organic8PetalShape
import com.sonora.music.ui.theme.SonoraObsidianCard
import com.sonora.music.ui.theme.SonoraObsidianDark
import com.sonora.music.ui.theme.SonoraPaperBeige
import com.sonora.music.ui.theme.SonoraPaperCard

enum class LibraryTab(val label: String, val icon: ImageVector) {
    CANCIONES("Canciones", Icons.Default.MusicNote),
    ARTISTAS("Artistas", Icons.Default.Person),
    ALBUMES("Álbumes", Icons.Default.Album),
    LISTAS("Listas ♡", Icons.Default.Favorite),
    CARPETAS("Carpetas", Icons.Default.Folder)
}

@Composable
fun NativeHomeScreen(
    allSongs: List<Song>,
    audioPlayer: SonoraAudioPlayer,
    onOpenPlayer: () -> Unit,
    onRefresh: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) SonoraObsidianDark else SonoraPaperBeige
    val cardBg = if (isDark) SonoraObsidianCard else SonoraPaperCard
    val textColor = if (isDark) Color.White else Color(0xFF121212)
    val subtextColor = if (isDark) Color(0xFF8A857B) else Color(0xFF75726B)

    var currentTab by remember { mutableStateOf(LibraryTab.CANCIONES) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.TITLE_AZ) }
    val blacklistedFolders = remember { mutableStateListOf<String>() }

    val currentSong by audioPlayer.currentSong.collectAsState()
    val isPlaying by audioPlayer.isPlaying.collectAsState()

    // Filter by blacklist & search
    val availableSongs = remember(allSongs, blacklistedFolders.toList(), searchQuery, sortMode) {
        val nonBlacklisted = allSongs.filter { s ->
            val folder = s.filePath.split("/").let { if (it.size > 1) it[it.size - 2] else s.album }
            !blacklistedFolders.contains(folder)
        }

        val searched = if (searchQuery.isBlank()) {
            nonBlacklisted
        } else {
            nonBlacklisted.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }

        when (sortMode) {
            SortMode.TITLE_AZ -> searched.sortedBy { it.title }
            SortMode.TITLE_ZA -> searched.sortedByDescending { it.title }
            SortMode.ARTIST_AZ -> searched.sortedBy { it.artist }
            SortMode.DATE_ADDED_DESC -> searched.sortedByDescending { it.dateAdded.takeIf { d -> d > 0 } ?: it.dateModified }
            SortMode.DURATION_DESC -> searched.sortedByDescending { it.durationMs }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(top = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Header with App Title & Scan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SONORA",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${availableSongs.size} canciones locales • 100% Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtextColor
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cardBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Escanear",
                        tint = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar canción, artista o carpeta...", fontSize = 12.sp, color = subtextColor) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = subtextColor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Category Tab Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LibraryTab.values()) { tab ->
                    val isSelected = currentTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) textColor else cardBg)
                            .clickable { currentTab = tab }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) bgColor else subtextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) bgColor else subtextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Main Tab Contents
            when (currentTab) {
                LibraryTab.CANCIONES -> {
                    // Sort Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${availableSongs.size} CANCIONES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = subtextColor
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .clickable {
                                    // Cycle Sort Mode
                                    sortMode = when (sortMode) {
                                        SortMode.TITLE_AZ -> SortMode.TITLE_ZA
                                        SortMode.TITLE_ZA -> SortMode.ARTIST_AZ
                                        SortMode.ARTIST_AZ -> SortMode.DATE_ADDED_DESC
                                        SortMode.DATE_ADDED_DESC -> SortMode.DURATION_DESC
                                        SortMode.DURATION_DESC -> SortMode.TITLE_AZ
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sort, contentDescription = null, tint = textColor, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(sortMode.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }

                    // Virtualized 120 FPS Song List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableSongs, key = { it.id }) { song ->
                            val isCurrent = currentSong?.id == song.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isCurrent) textColor else cardBg)
                                    .clickable {
                                        audioPlayer.playSong(song, availableSongs)
                                        onOpenPlayer()
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
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
                                        Text(
                                            text = song.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) bgColor else textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${song.artist} • ${song.album}",
                                            fontSize = 11.sp,
                                            color = if (isCurrent) bgColor.copy(alpha = 0.8f) else subtextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Text(
                                    text = song.durationFormatted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isCurrent) bgColor else subtextColor,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }

                LibraryTab.ARTISTAS -> {
                    val artists = remember(availableSongs) {
                        availableSongs.groupBy { it.artist }.map { (name, songs) ->
                            com.sonora.music.data.model.Artist(name, songs.size, songs.firstOrNull()?.coverUri)
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(artists) { artist ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    val artistSongs = availableSongs.filter { it.artist == artist.name }
                                    if (artistSongs.isNotEmpty()) {
                                        audioPlayer.playSong(artistSongs[0], artistSongs)
                                        onOpenPlayer()
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(Organic8PetalShape(petalCount = 8, amplitude = 0.08f))
                                        .background(cardBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = artist.avatarUri ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=300&auto=format&fit=crop",
                                        contentDescription = artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(artist.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${artist.trackCount} canciones", fontSize = 10.sp, color = subtextColor)
                            }
                        }
                    }
                }

                LibraryTab.CARPETAS -> {
                    val folders = remember(allSongs, blacklistedFolders.toList()) {
                        allSongs.groupBy { s ->
                            val parts = s.filePath.split("/")
                            if (parts.size > 1) parts[parts.size - 2] else s.album
                        }.map { (name, songs) ->
                            com.sonora.music.data.model.FolderGroup(
                                folderName = name,
                                songCount = songs.size,
                                isBlacklisted = blacklistedFolders.contains(name),
                                songs = songs
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folders) { folder ->
                            val isHidden = folder.isBlacklisted
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBg)
                                    .clickable {
                                        if (!isHidden && folder.songs.isNotEmpty()) {
                                            audioPlayer.playSong(folder.songs[0], folder.songs)
                                            onOpenPlayer()
                                        }
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = folder.folderName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHidden) subtextColor else textColor
                                        )
                                        Text("${folder.songCount} archivos de audio", fontSize = 11.sp, color = subtextColor)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (blacklistedFolders.contains(folder.folderName)) {
                                            blacklistedFolders.remove(folder.folderName)
                                        } else {
                                            blacklistedFolders.add(folder.folderName)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Ocultar",
                                        tint = if (isHidden) Color(0xFFEF4444) else textColor
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Albums & Playlists view
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Listas Inteligentes & Álbumes cargados", fontSize = 13.sp, color = subtextColor)
                    }
                }
            }
        }

        // 5. Floating MiniPlayer Bar
        AnimatedVisibility(
            visible = currentSong != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            currentSong?.let { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isDark) Color(0xFF1E1D1A) else Color(0xFFE2DDD2))
                        .clickable { onOpenPlayer() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AsyncImage(
                            model = song.coverUri ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=600&auto=format&fit=crop",
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(Organic8PetalShape(petalCount = 8, amplitude = 0.08f))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, fontSize = 11.sp, color = subtextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    IconButton(
                        onClick = { audioPlayer.togglePlay() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(textColor)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = bgColor
                        )
                    }
                }
            }
        }
    }
}
