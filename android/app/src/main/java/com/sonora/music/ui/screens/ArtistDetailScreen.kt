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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonora.music.data.model.Song
import com.sonora.music.data.repository.ArtistImageRepository
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.components.Organic8PetalShape
import com.sonora.music.ui.components.SonoraSongCover
import com.sonora.music.ui.theme.SonoraObsidianCard
import com.sonora.music.ui.theme.SonoraObsidianDark
import com.sonora.music.ui.theme.SonoraPaperBeige
import com.sonora.music.ui.theme.SonoraPaperCard

@Composable
fun ArtistDetailScreen(
    artistName: String,
    allSongs: List<Song>,
    audioPlayer: SonoraAudioPlayer,
    isDark: Boolean,
    onBack: () -> Unit,
    onSongOptions: (Song) -> Unit,
    onOpenPlayer: () -> Unit = {}
) {
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val bgColor = if (isGlass) Color.Transparent else themeColors.bg
    val cardBg = themeColors.cardBg
    val textColor = themeColors.textPrimary
    val subtextColor = themeColors.textSecondary

    val artistSongs = remember(artistName, allSongs) {
        allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
    }
    val artistCover = artistSongs.firstOrNull()?.coverUri

    var artistPhotoUrl by remember(artistName) { mutableStateOf<String?>(ArtistImageRepository.getCachedArtistImageUrl(artistName)) }
    var artistBio by remember(artistName) { mutableStateOf<com.sonora.music.data.repository.ArtistBioInfo?>(null) }

    LaunchedEffect(artistName) {
        val photo = ArtistImageRepository.getArtistImageUrl(artistName)
        if (photo != null) {
            artistPhotoUrl = photo
        }
        val bio = ArtistImageRepository.getArtistBioInfo(artistName)
        if (bio != null) {
            artistBio = bio
        }
    }

    val currentSong by audioPlayer.currentSong.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isGlass) (if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg else com.sonora.music.ui.theme.SonoraGlassLightBg) else themeColors.bg)
    ) {
        if (isGlass) {
            com.sonora.music.ui.theme.LiquidGlassBackdrop(isDark = isDark)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = textColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ARTISTA",
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
            // Artist Hero Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(Organic8PetalShape(petalCount = 8, amplitude = 0.08f))
                            .background(cardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = artistPhotoUrl ?: artistCover ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=300&auto=format&fit=crop",
                            contentDescription = artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = artistName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artistSongs.size} canciones en biblioteca",
                        fontSize = 12.sp,
                        color = subtextColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play All & Shuffle Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (artistSongs.isNotEmpty()) {
                                    audioPlayer.playSong(artistSongs[0], artistSongs)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = textColor,
                                contentColor = bgColor
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (artistSongs.isNotEmpty()) {
                                    val shuffled = artistSongs.shuffled()
                                    audioPlayer.playSong(shuffled[0], shuffled)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cardBg,
                                contentColor = textColor
                            )
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aleatorio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Songs List
            items(artistSongs, key = { it.id }) { song ->
                val isCurrent = currentSong?.id == song.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isCurrent) textColor else cardBg)
                        .clickable {
                            if (isCurrent) {
                                onOpenPlayer()
                            } else {
                                audioPlayer.playSong(song, artistSongs)
                            }
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))) {
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
                                color = if (isCurrent) bgColor else textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.album,
                                fontSize = 11.sp,
                                color = if (isCurrent) bgColor.copy(alpha = 0.8f) else subtextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.durationFormatted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isCurrent) bgColor else subtextColor
                        )
                        IconButton(onClick = { onSongOptions(song) }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = if (isCurrent) bgColor else subtextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}
}
