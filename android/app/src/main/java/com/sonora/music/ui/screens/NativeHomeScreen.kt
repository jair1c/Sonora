package com.sonora.music.ui.screens
import androidx.compose.ui.graphics.Brush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.sonora.music.data.repository.ArtistImageRepository
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.components.Organic8PetalShape
import com.sonora.music.ui.components.SleepTimerModal
import com.sonora.music.ui.components.StatsModal
import com.sonora.music.ui.components.SonoraSongCover
import com.sonora.music.data.repository.SongCoverRepository

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

    val headerBtnBrush = if (isGlass) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0x606080A8),
                    Color(0x35385070)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xEEFFFFFF),
                    Color(0xC0E2E8F0)
                )
            )
        }
    } else {
        SolidColor(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
    }

    val headerBtnBorderBrush = if (isGlass) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xB5FFFFFF),
                    Color(0x30FFFFFF)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0x8094A3B8)
                )
            )
        }
    } else {
        SolidColor(borderCol)
    }
    val searchBarBg = if (isGlass) (if (isDark) Color(0x1EFFFFFF) else Color(0x65FFFFFF)) else subCardBg
    val searchBarBorder = if (isGlass) (if (isDark) Color(0x38FFFFFF) else Color(0xB5FFFFFF)) else borderCol
    val inactivePillBg = if (isGlass) (if (isDark) Color(0x1CFFFFFF) else Color(0x60FFFFFF)) else subCardBg
    val inactivePillBorder = if (isGlass) (if (isDark) Color(0x38FFFFFF) else Color(0xB0FFFFFF)) else borderCol
    val activePillBgCol = if (isGlass) (if (isDark) Color(0xEEFFFFFF) else Color(0xFF0F172A)) else activePillBg
    val activePillTextCol = if (isGlass) (if (isDark) Color(0xFF0A0C10) else Color.White) else activePillText

    var currentTab by remember { mutableStateOf(LibraryTab.CANCIONES) }
    var searchQuery by remember { mutableStateOf("") }

    val prefsRevision by sonoraPrefs.prefsRevision.collectAsState()

    var sortMode by remember {
        val saved = sonoraPrefs.getSortMode()
        mutableStateOf(try { SortMode.valueOf(saved) } catch (e: Exception) { SortMode.TITLE_AZ })
    }
    var showSortDropdown by remember { mutableStateOf(false) }

    var showSleepModal by remember { mutableStateOf(false) }
    var showStatsModal by remember { mutableStateOf(false) }
    var navTabs by remember { mutableStateOf(sonoraPrefs.getNavTabs()) }

    val blacklistedFolders = remember { mutableStateListOf<String>() }
    val likedSongIds = remember { mutableStateListOf<Long>() }
    val selectedArtistMix = remember { mutableStateListOf<String>() }

    LaunchedEffect(prefsRevision) {
        val savedSort = sonoraPrefs.getSortMode()
        sortMode = try { SortMode.valueOf(savedSort) } catch (_: Exception) { SortMode.TITLE_AZ }

        blacklistedFolders.clear()
        blacklistedFolders.addAll(sonoraPrefs.getBlacklistedFolders())

        likedSongIds.clear()
        likedSongIds.addAll(sonoraPrefs.getLikedSongIds())

        navTabs = sonoraPrefs.getNavTabs()
    }

    val hazeState = com.sonora.music.ui.theme.LocalHazeState.current ?: remember { HazeState() }
    val topGlassStyle = HazeStyle(
        blurRadius = 20.dp,
        tint = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
        noiseFactor = 0.05f
    )
    val topGlassGlareBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.05f)
        )
    )
    val currentSong by audioPlayer.currentSong.collectAsState()
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val sleepTimerSeconds by audioPlayer.sleepTimerSecondsLeft.collectAsState()

    // Filter by blacklist, search & sort
    val availableSongs = remember(allSongs, blacklistedFolders.toList(), likedSongIds.toList(), searchQuery, sortMode) {
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
            SortMode.TITLE_AZ -> searched.sortedBy { it.title.lowercase() }
            SortMode.TITLE_ZA -> searched.sortedByDescending { it.title.lowercase() }
            SortMode.ARTIST_AZ -> searched.sortedBy { it.artist.lowercase() }
            SortMode.DATE_ADDED_DESC -> searched.sortedByDescending { if (it.dateAdded > 0) it.dateAdded else it.dateModified }
            SortMode.DURATION_DESC -> searched.sortedByDescending { it.durationMs }
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
        // 1. SCROLLABLE TAB CONTENT (THE REAL-TIME HAZE SOURCE)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isGlass) Modifier.haze(state = hazeState) else Modifier)
                .padding(horizontal = 20.dp)
        ) {
                when (currentTab) {
                    LibraryTab.CANCIONES -> {
                        // Songs List with Instant Scroll-to-Top on Sort Change
                        val songsListState = rememberLazyListState()
                        LaunchedEffect(sortMode) {
                            songsListState.scrollToItem(0)
                        }

                        LazyColumn(
                            state = songsListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 224.dp, bottom = if (currentSong != null) 210.dp else 140.dp)
                        ) {
                            // Subheader: X CANCIONES & SORT BUTTON (Scrolls smoothly with content)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
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

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = showSortDropdown,
                                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(expandFrom = Alignment.Top),
                                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top)
                                        ) {
                                            val sortModalShape = RoundedCornerShape(22.dp)
                                            val sortGlassGlareBorder = if (isDark) topGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x9094A3B8)))
                                            val sortGlassBg = if (isDark) Color(0xDC141D2B) else Color(0xEAFFFFFF)

                                            Column(
                                                modifier = Modifier
                                                    .padding(top = 34.dp)
                                                    .width(220.dp)
                                                    .clip(sortModalShape)
                                                    .then(
                                                        if (isGlass) {
                                                            Modifier
                                                                .hazeChild(state = hazeState, shape = sortModalShape, style = topGlassStyle)
                                                                .background(sortGlassBg)
                                                                .border(1.2.dp, sortGlassGlareBorder, sortModalShape)
                                                        } else {
                                                            Modifier
                                                                .background(cardBg)
                                                                .border(1.dp, borderCol, sortModalShape)
                                                        }
                                                    )
                                                    .padding(vertical = 6.dp, horizontal = 6.dp)
                                            ) {
                                                listOf(
                                                    Pair(SortMode.TITLE_AZ, "Nombre (A → Z)"),
                                                    Pair(SortMode.TITLE_ZA, "Nombre (Z → A)"),
                                                    Pair(SortMode.ARTIST_AZ, "Artista (A → Z)"),
                                                    Pair(SortMode.DATE_ADDED_DESC, "Fecha Más Reciente"),
                                                    Pair(SortMode.DURATION_DESC, "Mayor Duración")
                                                ).forEach { (mode, label) ->
                                                    val isSelected = sortMode == mode
                                                    val itemShape = RoundedCornerShape(14.dp)
                                                    val itemBg = if (isSelected) {
                                                        if (isGlass) {
                                                            if (isDark) Brush.linearGradient(listOf(Color.White, Color(0xFFE2E8F0))) else Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                                                        } else {
                                                            SolidColor(activePillBg)
                                                        }
                                                    } else SolidColor(Color.Transparent)

                                                    val itemTextColor = if (isSelected) {
                                                        if (isGlass) (if (isDark) Color(0xFF0F172A) else Color.White) else activePillText
                                                    } else textPrimary

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(itemShape)
                                                            .background(itemBg)
                                                            .then(
                                                                if (isSelected && isGlass) {
                                                                    Modifier.border(1.dp, Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.3f))), itemShape)
                                                                } else Modifier
                                                            )
                                                            .clickable {
                                                                sortMode = mode
                                                                sonoraPrefs.setSortMode(mode.name)
                                                                showSortDropdown = false
                                                            }
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = itemTextColor,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = itemTextColor,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                                items(availableSongs, key = { it.id }) { song ->
                                    val isCurrent = currentSong?.id == song.id
                                    val isLiked = likedSongIds.contains(song.id)

                                    val itemBg = if (isCurrent) {
                                        if (isGlass) (if (isDark) Color(0x38FFFFFF) else Color(0xBAFFFFFF)) else (if (isDark) Color.White else Color(0xFF121212))
                                    } else {
                                        cardBg
                                    }
                                    val itemBorder = if (isCurrent) {
                                        if (isGlass) (if (isDark) Color(0x80FFFFFF) else Color(0xE5FFFFFF)) else Color.Transparent
                                    } else {
                                        borderCol
                                    }
                                    val itemTitleColor = if (isCurrent) {
                                        if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else (if (isDark) Color(0xFF121212) else Color.White)
                                    } else {
                                        textPrimary
                                    }
                                    val itemSubColor = if (isCurrent) {
                                        if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else (if (isDark) Color(0xFF555555) else Color(0xFFCCCCCC))
                                    } else {
                                        textSecondary
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(itemBg)
                                            .border(
                                                if (isCurrent && isGlass) 1.5.dp else 1.dp,
                                                itemBorder,
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
                                                SonoraSongCover(
                                                    song = song,
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
                                contentPadding = PaddingValues(top = 224.dp, bottom = 180.dp)
                            ) {
                                items(artistsList) { artist ->
                                    val isSelected = selectedArtistMix.contains(artist.name)
                                    var artistPhotoUrl by remember(artist.name) { mutableStateOf<String?>(ArtistImageRepository.getCachedArtistImageUrl(artist.name)) }
                                    LaunchedEffect(artist.name) {
                                        val photo = ArtistImageRepository.getArtistImageUrl(artist.name)
                                        if (photo != null) {
                                            artistPhotoUrl = photo
                                        }
                                    }

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
                                                    model = artistPhotoUrl ?: artist.avatarUri,
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

                            // Floating Mix Button (Real-time Glass Backdrop Blur & Glare Border)
                            if (selectedArtistMix.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 168.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .then(
                                            if (isGlass) {
                                                Modifier
                                                    .hazeChild(state = hazeState, shape = RoundedCornerShape(100.dp), style = topGlassStyle)
                                                    .background(if (isDark) Color(0x401E293B) else Color(0x80FFFFFF))
                                                    .border(1.2.dp, topGlassGlareBorder, RoundedCornerShape(100.dp))
                                            } else {
                                                Modifier
                                                    .background(if (isDark) Color.White else Color(0xFF121212))
                                                    .border(1.dp, if (isDark) Color.Transparent else Color(0xFF333333), RoundedCornerShape(100.dp))
                                            }
                                        )
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
                            contentPadding = PaddingValues(top = 224.dp, bottom = 180.dp)
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
                                        val firstSongOfAlbum = remember(album.title) {
                                            availableSongs.firstOrNull { it.album.equals(album.title, ignoreCase = true) }
                                        }
                                        SonoraSongCover(
                                            song = firstSongOfAlbum,
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
                            contentPadding = PaddingValues(top = 224.dp, bottom = 180.dp)
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

        // 2. FLOATING TOP HEADER (ADAPTIVE TINTED GLASS & BOTTOM GLARE LINE ONLY)
        val topHeaderGlassStyle = HazeStyle(
            blurRadius = 26.dp,
            tint = if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg.copy(alpha = 0.35f) else com.sonora.music.ui.theme.SonoraGlassLightBg.copy(alpha = 0.20f),
            noiseFactor = 0.04f
        )
        val topHeaderBg = if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg.copy(alpha = 0.30f) else com.sonora.music.ui.theme.SonoraGlassLightBg.copy(alpha = 0.22f)
        val topBtnBg = if (isDark) Color(0x351E293B) else Color(0x75FFFFFF)
        val topBtnBorder = if (isDark) topGlassGlareBorder else Brush.verticalGradient(listOf(Color.White, Color(0x8094A3B8)))

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .then(
                    if (isGlass) {
                        Modifier
                            .hazeChild(state = hazeState, shape = androidx.compose.ui.graphics.RectangleShape, style = topHeaderGlassStyle)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        topHeaderBg,
                                        topHeaderBg.copy(alpha = if (isDark) 0.35f else 0.65f)
                                    )
                                )
                            )
                            .drawBehind {
                                val strokeWidth = 1.dp.toPx()
                                val y = size.height - strokeWidth / 2
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            (if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.60f)),
                                            (if (isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.80f)),
                                            (if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.60f)),
                                            Color.Transparent
                                        )
                                    ),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = strokeWidth
                                )
                            }
                    } else {
                        Modifier
                            .background(themeColors.bg)
                            .drawBehind {
                                val strokeWidth = 1.dp.toPx()
                                val y = size.height - strokeWidth / 2
                                drawLine(
                                    color = borderCol,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = strokeWidth
                                )
                            }
                    }
                )
                .padding(bottom = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. TOP APP BAR with (Music/Back), TU BIBLIOTECA LOCAL, and Right Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Brand / Back Circle Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (isGlass) {
                                Modifier
                                    .background(topBtnBg)
                                    .border(1.2.dp, topBtnBorder, CircleShape)
                            } else {
                                Modifier
                                    .background(headerBtnBrush)
                                    .border(1.dp, headerBtnBorderBrush, CircleShape)
                            }
                        )
                        .clickable {
                            if (currentTab != LibraryTab.CANCIONES) {
                                currentTab = LibraryTab.CANCIONES
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentTab != LibraryTab.CANCIONES) Icons.Default.ArrowBack else Icons.Default.MusicNote,
                        contentDescription = if (currentTab != LibraryTab.CANCIONES) "Regresar" else "Sonora",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
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

                // Right Circular Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Equalizer Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .then(
                                if (isGlass) {
                                    Modifier
                                        .background(topBtnBg)
                                        .border(1.2.dp, topBtnBorder, CircleShape)
                                } else {
                                    Modifier
                                        .background(headerBtnBrush)
                                        .border(1.dp, headerBtnBorderBrush, CircleShape)
                                }
                            )
                            .clickable { onOpenEqualizer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ecualizador",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 2. Sleep Timer Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .then(
                                if (isGlass) {
                                    Modifier
                                        .background(topBtnBg)
                                        .border(1.2.dp, if (sleepTimerSeconds != null) SolidColor(Color(0xFF10B981)) else topBtnBorder, CircleShape)
                                } else {
                                    Modifier
                                        .background(headerBtnBrush)
                                        .border(1.dp, if (sleepTimerSeconds != null) SolidColor(Color(0xFF10B981)) else headerBtnBorderBrush, CircleShape)
                                }
                            )
                            .clickable { showSleepModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Temporizador",
                            tint = if (sleepTimerSeconds != null) Color(0xFF10B981) else textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 3. Stats Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .then(
                                if (isGlass) {
                                    Modifier
                                        .background(topBtnBg)
                                        .border(1.2.dp, topBtnBorder, CircleShape)
                                } else {
                                    Modifier
                                        .background(headerBtnBrush)
                                        .border(1.dp, headerBtnBorderBrush, CircleShape)
                                }
                            )
                            .clickable { showStatsModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Estadísticas",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 4. Listas Button (Shown ONLY if NOT in bottom nav bar)
                    if (!navTabs.contains("listas")) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isGlass) {
                                        Modifier
                                            .background(topBtnBg)
                                            .border(1.2.dp, topBtnBorder, CircleShape)
                                    } else {
                                        Modifier
                                            .background(headerBtnBrush)
                                            .border(1.dp, headerBtnBorderBrush, CircleShape)
                                    }
                                )
                                .clickable { currentTab = LibraryTab.LISTAS },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Listas",
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 5. Settings Button (Shown ONLY if NOT in bottom nav bar)
                    if (!navTabs.contains("ajustes")) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isGlass) {
                                        Modifier
                                            .background(topBtnBg)
                                            .border(1.2.dp, topBtnBorder, CircleShape)
                                    } else {
                                        Modifier
                                            .background(headerBtnBrush)
                                            .border(1.dp, headerBtnBorderBrush, CircleShape)
                                    }
                                )
                                .clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes",
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. HERO TITLE & RE-SCAN BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    if (currentTab != LibraryTab.CANCIONES) {
                        Text(
                            text = "${availableSongs.size} canciones encontradas en el almacenamiento",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .then(
                            if (isGlass) {
                                Modifier
                                    .background(topBtnBg)
                                    .border(1.2.dp, topBtnBorder, RoundedCornerShape(100.dp))
                            } else {
                                Modifier
                                    .background(headerBtnBrush)
                                    .border(1.dp, headerBtnBorderBrush, RoundedCornerShape(100.dp))
                            }
                        )
                        .clickable {
                            onRescanLibrary()
                            Toast.makeText(context, "Biblioteca actualizada", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(14.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // 3. SEARCH BAR (Embedded Glass Pill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .then(
                        if (isGlass) {
                            Modifier
                                .background(topBtnBg)
                                .border(1.2.dp, topBtnBorder, RoundedCornerShape(100.dp))
                        } else {
                            Modifier
                                .background(searchBarBg)
                                .border(1.dp, searchBarBorder, RoundedCornerShape(100.dp))
                        }
                    )
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

            Spacer(modifier = Modifier.height(10.dp))

            // 4. CATEGORY HORIZONTAL PILLS (Edge-to-Edge with full right clearance)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 28.dp)
            ) {
                items(LibraryTab.values()) { tab ->
                    val isSelected = currentTab == tab
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .then(
                                if (isGlass) {
                                    if (isSelected) {
                                        Modifier
                                            .background(
                                                if (isDark) {
                                                    Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)))
                                                } else {
                                                    Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                                                }
                                            )
                                            .border(
                                                1.2.dp,
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.White,
                                                        Color.White.copy(alpha = if (isDark) 0.85f else 0.25f)
                                                    )
                                                ),
                                                RoundedCornerShape(100.dp)
                                            )
                                    } else {
                                        Modifier
                                            .background(topBtnBg)
                                            .border(1.2.dp, topBtnBorder, RoundedCornerShape(100.dp))
                                    }
                                } else {
                                    Modifier
                                        .background(if (isSelected) activePillBg else subCardBg)
                                        .border(1.dp, if (isSelected) Color.Transparent else borderCol, RoundedCornerShape(100.dp))
                                }
                            )
                            .clickable { currentTab = tab }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val pillActiveColor = if (isGlass && isSelected) (if (isDark) Color(0xFF0F172A) else Color.White) else (if (isSelected) activePillText else textPrimary)
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = pillActiveColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = pillActiveColor
                            )
                        }
                    }
                }
            }
        }

        // 6. FLOATING MINIPLAYER (Real-time Glass Backdrop Blur & Glare Border)
        if (currentSong != null) {
            val glassStyle = HazeStyle(
                blurRadius = 24.dp,
                tint = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                noiseFactor = 0.05f
            )
            val glassGlareBorder = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.05f)
                )
            )

            val miniPlayerTitle = if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else textPrimary
            val miniPlayerSub = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else textSecondary
            val miniPlayBtnBrush = if (isGlass) {
                if (isDark) {
                    Brush.linearGradient(listOf(Color(0x75FFFFFF), Color(0x40FFFFFF)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)))
                }
            } else {
                SolidColor(if (isDark) Color.White else Color(0xFF121212))
            }
            val miniPlayBtnBorderBrush = if (isGlass) {
                if (isDark) Brush.verticalGradient(listOf(Color(0x90FFFFFF), Color(0x30FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x90CBD5E1)))
            } else SolidColor(Color.Transparent)
            val miniPlayBtnIcon = if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else (if (isDark) Color.Black else Color.White)

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 86.dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(if (isGlass) 0.dp else 6.dp, RoundedCornerShape(22.dp), spotColor = if (isDark) Color(0x90000000) else Color(0x40000000))
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        if (isGlass) {
                            Modifier
                                .hazeChild(state = hazeState, shape = RoundedCornerShape(22.dp), style = glassStyle)
                                .background(if (isDark) Color.Black.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f))
                                .border(1.dp, glassGlareBorder, RoundedCornerShape(22.dp))
                        } else {
                            Modifier
                                .background(SolidColor(if (isDark) Color(0xFF1A1917) else Color(0xFFFAF7F0)))
                                .border(1.dp, SolidColor(borderCol), RoundedCornerShape(22.dp))
                        }
                    )
                    .clickable { onOpenPlayer() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
                            SonoraSongCover(
                                song = currentSong,
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
                            .background(miniPlayBtnBrush)
                            .border(if (isGlass) 1.dp else 0.dp, miniPlayBtnBorderBrush, CircleShape)
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

        // 7. BOTTOM NAVIGATION BAR (Real-time Glass Backdrop Blur & Glare Border)
        val navLabelMode = remember(sonoraPrefs) { sonoraPrefs.getNavLabelMode() }
        val glassStyle = HazeStyle(
            blurRadius = 24.dp,
            tint = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
            noiseFactor = 0.05f
        )
        val glassGlareBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.05f)
            )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                .fillMaxWidth()
                .height(62.dp)
                .shadow(if (isGlass) 0.dp else 8.dp, RoundedCornerShape(32.dp), spotColor = if (isDark) Color(0x99000000) else Color(0x45000000))
                .clip(RoundedCornerShape(32.dp))
                .then(
                    if (isGlass) {
                        Modifier
                            .hazeChild(state = hazeState, shape = RoundedCornerShape(32.dp), style = glassStyle)
                            .background(if (isDark) Color.Black.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, glassGlareBorder, RoundedCornerShape(32.dp))
                    } else {
                        Modifier
                            .background(SolidColor(if (isDark) Color(0xFF141312) else Color(0xFFFAF7F0)))
                            .border(1.dp, SolidColor(borderCol), RoundedCornerShape(32.dp))
                    }
                )
                .clickable(enabled = false) {} // Intercept taps
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val selectedColor = if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else (if (isDark) Color.White else Color(0xFF121212))
    val unselectedColor = if (isGlass) (if (isDark) Color(0x90FFFFFF) else Color(0x850F172A)) else (if (isDark) Color(0xFF9E998F) else Color(0xFF5A5852))
    val showLabel = when (navLabelMode) {
        "active_only" -> isSelected
        "always" -> true
        "never" -> false
        else -> isSelected
    }

    val activePillIndicator = if (isSelected && isGlass) {
        if (isDark) Color(0x30FFFFFF) else Color(0x250F172A)
    } else if (isSelected) {
        if (isDark) Color(0x20FFFFFF) else Color(0x15000000)
    } else Color.Transparent

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(activePillIndicator)
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