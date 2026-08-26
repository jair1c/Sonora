package com.sonora.music.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonora.music.data.local.SonoraPreferences
import com.sonora.music.data.model.Album
import com.sonora.music.data.model.Artist
import com.sonora.music.data.model.FolderGroup
import com.sonora.music.data.model.Song
import com.sonora.music.data.model.SortMode
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.components.Organic8PetalShape
import com.sonora.music.ui.components.SleepTimerModal
import com.sonora.music.ui.components.StatsModal

enum class LibraryTab(val id: String, val label: String, val icon: ImageVector) {
    ARTISTAS("artistas", "Artistas", Icons.Default.Person),
    CANCIONES("canciones", "Canciones", Icons.Default.MusicNote),
    ALBUMES("albumes", "Álbumes", Icons.Default.Album),
    LISTAS("listas", "Listas ♡", Icons.Default.Favorite),
    CARPETAS("carpetas", "Carpetas", Icons.Default.Folder)
}

@Composable
fun NativeHomeScreen(
    allSongs: List<Song>,
    audioPlayer: SonoraAudioPlayer,
    sonoraPrefs: SonoraPreferences,
    isDark: Boolean,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenArtistDetail: (String) -> Unit,
    onOpenAlbumDetail: (String) -> Unit,
    onSongOptions: (Song) -> Unit,
    onRescanLibrary: () -> Unit
) {
    val context = LocalContext.current

    val bgColor = if (isDark) Color(0xFF0F0E0D) else Color(0xFFF5F2EA)
    val cardBg = if (isDark) Color(0xFF1A1917) else Color(0xFFEAE5DA)
    val subCardBg = if (isDark) Color(0xFF1F1D1A) else Color(0xFFECE7DC)
    val borderCol = if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)
    val textPrimary = if (isDark) Color(0xFFF5F2EA) else Color(0xFF121212)
    val textSecondary = if (isDark) Color(0xFF8A857B) else Color(0xFF75726B)
    val activePillBg = if (isDark) Color.White else Color(0xFF121212)
    val activePillText = if (isDark) Color.Black else Color.White

    var currentTab by remember { mutableStateOf(LibraryTab.CANCIONES) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember {
        val saved = sonoraPrefs.getSortMode()
        mutableStateOf(try { SortMode.valueOf(saved) } catch (e: Exception) { SortMode.TITLE_AZ })
    }
    var showSortDropdown by remember { mutableStateOf(false) }

    var showSleepModal by remember { mutableStateOf(false) }
    var showStatsModal by remember { mutableStateOf(false) }
    val navTabs = remember { sonoraPrefs.getNavTabs() }

    val blacklistedFolders = remember { mutableStateListOf<String>().apply { addAll(sonoraPrefs.getBlacklistedFolders()) } }
    val likedSongIds = remember { mutableStateListOf<Long>().apply { addAll(sonoraPrefs.getLikedSongIds()) } }
    val selectedArtistMix = remember { mutableStateListOf<String>() }
    var activeFilterChip by remember { mutableStateOf("ALL") }

    val currentSong by audioPlayer.currentSong.collectAsState()
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val sleepTimerSeconds by audioPlayer.sleepTimerSecondsLeft.collectAsState()

    // Filter by blacklist, search & chip
    val availableSongs = remember(allSongs, blacklistedFolders.toList(), likedSongIds.toList(), searchQuery, sortMode, activeFilterChip) {
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

        val chipFiltered = when (activeFilterChip) {
            "HI_RES" -> searched.filter {
                val p = it.filePath.lowercase()
                p.endsWith(".flac") || p.endsWith(".wav") || p.endsWith(".alac") || p.endsWith(".aiff") || p.endsWith(".dsd") || p.endsWith(".opus")
            }
            "FAVORITES" -> searched.filter { likedSongIds.contains(it.id) }
            "LONG" -> searched.filter { it.durationMs >= 300000L }
            "NEW" -> searched.sortedByDescending { if (it.dateAdded > 0) it.dateAdded else it.dateModified }.take(50)
            else -> searched
        }

        when (sortMode) {
            SortMode.TITLE_AZ -> chipFiltered.sortedBy { it.title.lowercase() }
            SortMode.TITLE_ZA -> chipFiltered.sortedByDescending { it.title.lowercase() }
            SortMode.ARTIST_AZ -> chipFiltered.sortedBy { it.artist.lowercase() }
            SortMode.DATE_ADDED_DESC -> chipFiltered.sortedByDescending { if (it.dateAdded > 0) it.dateAdded else it.dateModified }
            SortMode.DURATION_DESC -> chipFiltered.sortedByDescending { it.durationMs }
        }
    }

    // Dynamic Title
    val headerTitle = when (currentTab) {
        LibraryTab.CANCIONES -> "Todas las Canciones"
        LibraryTab.ARTISTAS -> "Elige tus Artistas"
        LibraryTab.ALBUMES -> "Tus Álbumes"
        LibraryTab.LISTAS -> "Listas de Reproducción"
        LibraryTab.CARPETAS -> "Carpetas Locales"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // 1. TOP APP BAR with (←), TU BIBLIOTECA LOCAL, and 4 Right Circle Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Circle Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
                        .border(1.dp, borderCol, CircleShape)
                        .clickable { currentTab = LibraryTab.CANCIONES },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Regresar",
                        tint = textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Centered Title
                Text(
                    text = "TU BIBLIOTECA LOCAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = textPrimary
                )

                // Right Circular Action Buttons (Intelligent Detection)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Equalizer Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
                            .border(1.dp, borderCol, CircleShape)
                            .clickable { onOpenEqualizer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ecualizador",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 2. Sleep Timer Button (Moon)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
                            .border(1.dp, if (sleepTimerSeconds != null) Color(0xFF10B981) else borderCol, CircleShape)
                            .clickable { showSleepModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Temporizador",
                            tint = if (sleepTimerSeconds != null) Color(0xFF10B981) else textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 3. Stats Button (BarChart)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
                            .border(1.dp, borderCol, CircleShape)
                            .clickable { showStatsModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Estadísticas",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 4. Listas Button (Shown ONLY if NOT in bottom nav bar)
                    if (!navTabs.contains("listas")) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
                                .border(1.dp, borderCol, CircleShape)
                                .clickable { currentTab = LibraryTab.LISTAS },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Listas",
                                tint = textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 5. Settings Button (Shown ONLY if NOT in bottom nav bar)
                    if (!navTabs.contains("ajustes")) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
                                .border(1.dp, borderCol, CircleShape)
                                .clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes",
                                tint = textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. HERO TITLE & RE-SCAN BUTTON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "${availableSongs.size} canciones encontradas en el almacenamiento",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(subCardBg)
                        .border(1.dp, borderCol, RoundedCornerShape(100.dp))
                        .clickable {
                            onRescanLibrary()
                            Toast.makeText(context, "Biblioteca actualizada", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Escanear",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. SEARCH BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(subCardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(100.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(textPrimary),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar por artista, canción o álbum...",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar",
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. CATEGORY HORIZONTAL PILLS
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LibraryTab.values()) { tab ->
                    val isSelected = currentTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelected) activePillBg else subCardBg)
                            .border(1.dp, if (isSelected) Color.Transparent else borderCol, RoundedCornerShape(100.dp))
                            .clickable { currentTab = tab }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) activePillText else textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activePillText else textPrimary
                            )
                        }
                    }
                }
            }

            // Quick Filter Chips (All, Hi-Res, Favorites, +5 Min, Recent)
            if (currentTab == LibraryTab.CANCIONES || searchQuery.isNotEmpty()) {
                val filterChips = listOf(
                    Pair("ALL", "Todos"),
                    Pair("HI_RES", "💎 Hi-Res / FLAC"),
                    Pair("FAVORITES", "❤️ Favoritas"),
                    Pair("LONG", "⏱️ +5 Min"),
                    Pair("NEW", "⚡ Recientes")
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filterChips) { (id, label) ->
                        val isChipSelected = activeFilterChip == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (isChipSelected) activePillBg else subCardBg.copy(alpha = 0.7f))
                                .border(1.dp, if (isChipSelected) Color.Transparent else borderCol, RoundedCornerShape(100.dp))
                                .clickable { activeFilterChip = id }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChipSelected) activePillText else textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. TAB CONTENT
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    LibraryTab.CANCIONES -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Subheader: X CANCIONES & SORT BUTTON
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${availableSongs.size} CANCIONES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = textSecondary
                                )

                                Box {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(subCardBg)
                                            .border(1.dp, borderCol, RoundedCornerShape(100.dp))
                                            .clickable { showSortDropdown = !showSortDropdown }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sort,
                                                contentDescription = null,
                                                tint = textPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = when (sortMode) {
                                                    SortMode.TITLE_AZ -> "Nombre (A → Z)"
                                                    SortMode.TITLE_ZA -> "Nombre (Z → A)"
                                                    SortMode.ARTIST_AZ -> "Artista (A → Z)"
                                                    SortMode.DATE_ADDED_DESC -> "Fecha Más Reciente"
                                                    SortMode.DURATION_DESC -> "Mayor Duración"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showSortDropdown,
                                        onDismissRequest = { showSortDropdown = false },
                                        modifier = Modifier.background(cardBg)
                                    ) {
                                        listOf(
                                            Pair(SortMode.TITLE_AZ, "Nombre (A → Z)"),
                                            Pair(SortMode.TITLE_ZA, "Nombre (Z → A)"),
                                            Pair(SortMode.ARTIST_AZ, "Artista (A → Z)"),
                                            Pair(SortMode.DATE_ADDED_DESC, "Fecha Más Reciente"),
                                            Pair(SortMode.DURATION_DESC, "Mayor Duración")
                                        ).forEach { (mode, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label, color = textPrimary, fontSize = 12.sp) },
                                                onClick = {
                                                    sortMode = mode
                                                    sonoraPrefs.setSortMode(mode.name)
                                                    showSortDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Songs List with Instant Scroll-to-Top on Sort Change
                            val songsListState = rememberLazyListState()
                            LaunchedEffect(sortMode) {
                                songsListState.scrollToItem(0)
                            }

                            LazyColumn(
                                state = songsListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 160.dp)
                            ) {
                                items(availableSongs, key = { it.id }) { song ->
                                    val isCurrent = currentSong?.id == song.id
                                    val isLiked = likedSongIds.contains(song.id)

                                    // Active Card: Solid White in Dark, Solid Black in Light
                                    // Inactive Card: #1A1917 in Dark, #EAE5DA in Light
                                    val itemBg = if (isCurrent) {
                                        if (isDark) Color.White else Color(0xFF121212)
                                    } else {
                                        cardBg
                                    }

                                    val itemTitleColor = if (isCurrent) {
                                        if (isDark) Color(0xFF121212) else Color.White
                                    } else {
                                        textPrimary
                                    }

                                    val itemSubColor = if (isCurrent) {
                                        if (isDark) Color(0xFF555555) else Color(0xFFCCCCCC)
                                    } else {
                                        textSecondary
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(itemBg)
                                            .border(
                                                1.dp,
                                                if (isCurrent) Color.Transparent else borderCol,
                                                RoundedCornerShape(18.dp)
                                            )
                                            .clickable {
                                                if (isCurrent) {
                                                    onOpenPlayer()
                                                } else {
                                                    audioPlayer.playSong(song, availableSongs)
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.Gray.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = song.coverUri,
                                                    contentDescription = song.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                if (isCurrent && isPlaying) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.4f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = song.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = itemTitleColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${song.artist} • ${song.album}",
                                                    fontSize = 11.sp,
                                                    color = itemSubColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = song.durationFormatted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = itemSubColor
                                            )

                                            IconButton(
                                                onClick = {
                                                    val isNowLiked = sonoraPrefs.toggleLikeSong(song.id)
                                                    if (isNowLiked) likedSongIds.add(song.id) else likedSongIds.remove(song.id)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "Favorito",
                                                    tint = if (isLiked) Color.Red else itemSubColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onSongOptions(song) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Opciones",
                                                    tint = itemSubColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LibraryTab.ARTISTAS -> {
                        val artistsList = remember(availableSongs) {
                            availableSongs.groupBy { it.artist }.map { (name, list) ->
                                Artist(name, list.size, list.firstOrNull()?.coverUri)
                            }.sortedBy { it.name.lowercase() }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
                            ) {
                                items(artistsList) { artist ->
                                    val isSelected = selectedArtistMix.contains(artist.name)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                if (isSelected) {
                                                    selectedArtistMix.remove(artist.name)
                                                } else {
                                                    selectedArtistMix.add(artist.name)
                                                }
                                            }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(92.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Floral Organic Avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(88.dp)
                                                    .clip(Organic8PetalShape(8, 0.12f))
                                                    .background(cardBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = artist.avatarUri,
                                                    contentDescription = artist.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.25f))
                                                    )
                                                }
                                            }

                                            // Unclipped Selected Check Badge Floating at Bottom End
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .align(Alignment.BottomEnd)
                                                        .clip(CircleShape)
                                                        .background(if (isDark) Color.White else Color(0xFF121212))
                                                        .border(2.dp, if (isDark) Color.Black else Color.White, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (isDark) Color.Black else Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = artist.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${artist.trackCount} ${if (artist.trackCount == 1) "canción" else "canciones"}",
                                            fontSize = 10.sp,
                                            color = textSecondary
                                        )
                                    }
                                }
                            }

                            // Floating Mix Button
                            if (selectedArtistMix.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 130.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(if (isDark) Color.White else Color(0xFF121212))
                                        .border(1.dp, if (isDark) Color.Transparent else Color(0xFF333333), RoundedCornerShape(100.dp))
                                        .clickable {
                                            val mixSongs = availableSongs.filter { selectedArtistMix.contains(it.artist) }.shuffled()
                                            if (mixSongs.isNotEmpty()) {
                                                audioPlayer.playSong(mixSongs.first(), mixSongs)
                                                onOpenPlayer()
                                                selectedArtistMix.clear()
                                            }
                                        }
                                        .padding(horizontal = 22.dp, vertical = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isDark) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Reproducir Mix (${selectedArtistMix.size} ${if (selectedArtistMix.size == 1) "artista" else "artistas"})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDark) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LibraryTab.ALBUMES -> {
                        val albumsList = remember(availableSongs) {
                            availableSongs.groupBy { it.album }.map { (title, list) ->
                                Album(title, list.firstOrNull()?.artist ?: "Desconocido", list.size, list.firstOrNull()?.coverUri)
                            }.sortedBy { it.title.lowercase() }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
                        ) {
                            items(albumsList) { album ->
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(cardBg)
                                        .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                                        .clickable { onOpenAlbumDetail(album.title) }
                                        .padding(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Gray.copy(alpha = 0.2f))
                                    ) {
                                        AsyncImage(
                                            model = album.coverUri,
                                            contentDescription = album.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = album.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${album.artist} • ${album.trackCount} canciones",
                                        fontSize = 10.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    LibraryTab.LISTAS -> {
                        PlaylistsScreen(
                            allSongs = availableSongs,
                            audioPlayer = audioPlayer,
                            sonoraPrefs = sonoraPrefs,
                            isDark = isDark,
                            onOpenPlayer = onOpenPlayer
                        )
                    }

                    LibraryTab.CARPETAS -> {
                        val folders = remember(allSongs, blacklistedFolders.toList()) {
                            allSongs.groupBy { s ->
                                val parts = s.filePath.split("/")
                                if (parts.size > 1) parts[parts.size - 2] else "Raíz"
                            }.map { (name, songs) ->
                                FolderGroup(name, songs.size, blacklistedFolders.contains(name))
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 180.dp)
                        ) {
                            items(folders) { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(cardBg)
                                        .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = textPrimary, modifier = Modifier.size(24.dp))
                                        Column {
                                            Text(folder.folderName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                            Text("${folder.songCount} canciones", fontSize = 11.sp, color = textSecondary)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            if (folder.isBlacklisted) {
                                                sonoraPrefs.unblacklistFolder(folder.folderName)
                                                blacklistedFolders.remove(folder.folderName)
                                            } else {
                                                sonoraPrefs.blacklistFolder(folder.folderName)
                                                blacklistedFolders.add(folder.folderName)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (folder.isBlacklisted) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Visibilidad",
                                            tint = if (folder.isBlacklisted) Color.Red else textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. FLOATING MINIPLAYER (At bottom above nav bar)
        if (currentSong != null) {
            val miniPlayerBg = if (isDark) Color(0xFF1A1917) else Color(0xFFFAF7F0)
            val miniPlayerBorder = if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)
            val miniPlayerTitle = if (isDark) Color.White else Color(0xFF121212)
            val miniPlayerSub = if (isDark) Color(0xFFA19C93) else Color(0xFF75726B)
            val miniPlayBtnBg = if (isDark) Color.White else Color(0xFF121212)
            val miniPlayBtnIcon = if (isDark) Color.Black else Color.White

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 72.dp)
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(miniPlayerBg)
                    .border(1.dp, miniPlayerBorder, RoundedCornerShape(100.dp))
                    .clickable { onOpenPlayer() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.DarkGray else Color(0xFFEAE5DA))
                        ) {
                            AsyncImage(
                                model = currentSong!!.coverUri,
                                contentDescription = currentSong!!.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSong!!.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = miniPlayerTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong!!.artist,
                                fontSize = 10.sp,
                                color = miniPlayerSub,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Play / Pause Circle Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(miniPlayBtnBg)
                            .clickable {
                                if (isPlaying) audioPlayer.pause() else audioPlayer.resume()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                            tint = miniPlayBtnIcon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 7. BOTTOM NAVIGATION BAR (Fixed)
        val navLabelMode = remember(sonoraPrefs) { sonoraPrefs.getNavLabelMode() }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
                .background(bgColor)
                .border(1.dp, borderCol.copy(alpha = 0.5f))
                .clickable(enabled = false) {} // Intercept taps to prevent triggering underlying song items
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (tabId in navTabs) {
                    when (tabId) {
                        "canciones", "biblioteca" -> BottomNavItem(
                            icon = Icons.Default.MusicNote,
                            label = "Canciones",
                            isSelected = currentTab == LibraryTab.CANCIONES,
                            isDark = isDark,
                            navLabelMode = navLabelMode,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { currentTab = LibraryTab.CANCIONES }
                        )
                        "artistas" -> BottomNavItem(
                            icon = Icons.Default.Person,
                            label = "Artistas",
                            isSelected = currentTab == LibraryTab.ARTISTAS,
                            isDark = isDark,
                            navLabelMode = navLabelMode,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { currentTab = LibraryTab.ARTISTAS }
                        )
                        "albumes" -> BottomNavItem(
                            icon = Icons.Default.Album,
                            label = "Álbumes",
                            isSelected = currentTab == LibraryTab.ALBUMES,
                            isDark = isDark,
                            navLabelMode = navLabelMode,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { currentTab = LibraryTab.ALBUMES }
                        )
                        "listas" -> BottomNavItem(
                            icon = Icons.Default.Favorite,
                            label = "Listas ♡",
                            isSelected = currentTab == LibraryTab.LISTAS,
                            isDark = isDark,
                            navLabelMode = navLabelMode,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { currentTab = LibraryTab.LISTAS }
                        )
                        "carpetas" -> BottomNavItem(
                            icon = Icons.Default.Folder,
                            label = "Carpetas",
                            isSelected = currentTab == LibraryTab.CARPETAS,
                            isDark = isDark,
                            navLabelMode = navLabelMode,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { currentTab = LibraryTab.CARPETAS }
                        )
                        "reproductor" -> BottomNavItem(
                            icon = Icons.Default.PlayCircle,
                            label = "Reproductor",
                            isSelected = false,
                            isDark = isDark,
                            navLabelMode = navLabelMode,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = onOpenPlayer
                        )
                        "ajustes" -> BottomNavItem(
                            icon = Icons.Default.Tune,
                            label = "Ajustes",
                            isSelected = false,
                            isDark = isDark,
                            navLabelMode = navLabelMode,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = onOpenSettings
                        )
                    }
                }
            }
        }
    }

    val sleepTimerFinishSong by audioPlayer.sleepTimerFinishSong.collectAsState()

    // Modals
    SleepTimerModal(
        isOpen = showSleepModal,
        onClose = { showSleepModal = false },
        currentMinutesRemaining = if (sleepTimerSeconds == -1) -1 else sleepTimerSeconds?.let { it / 60 },
        currentFinishSong = sleepTimerFinishSong,
        onSetTimer = { mins, finishSong ->
            if (mins == null) {
                audioPlayer.cancelSleepTimer()
            } else {
                audioPlayer.startSleepTimer(mins, finishSong)
            }
        },
        isDark = isDark
    )

    StatsModal(
        isOpen = showStatsModal,
        onClose = { showStatsModal = false },
        songs = allSongs,
        isDark = isDark,
        sonoraPrefs = sonoraPrefs,
        onPlaySong = { song ->
            audioPlayer.playSong(song, allSongs)
        }
    )
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    navLabelMode: String = "active_only",
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selectedColor = if (isDark) Color.White else Color(0xFF121212)
    val unselectedColor = if (isDark) Color(0xFF9E998F) else Color(0xFF5A5852)
    val showLabel = when (navLabelMode) {
        "active_only" -> isSelected
        "always" -> true
        "never" -> false
        else -> isSelected
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) selectedColor else unselectedColor,
                modifier = Modifier.size(24.dp)
            )
            if (showLabel) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) selectedColor else unselectedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
