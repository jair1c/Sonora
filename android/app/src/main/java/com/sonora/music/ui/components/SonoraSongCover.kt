package com.sonora.music.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.sonora.music.data.model.Song
import com.sonora.music.data.repository.SongCoverRepository

@Composable
fun SonoraSongCover(
    song: Song?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackUrl: String = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=600&auto=format&fit=crop"
) {
    if (song == null) {
        AsyncImage(
            model = fallbackUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
        return
    }

    val localCover = song.coverUri
    var coverModel by remember(song.id) {
        mutableStateOf<Any?>(localCover ?: SongCoverRepository.getCachedCoverUrl(song))
    }

    LaunchedEffect(song.id) {
        if (localCover == null && coverModel == null) {
            val online = SongCoverRepository.getSongCoverUrl(song)
            if (online != null) {
                coverModel = online
            }
        }
    }

    AsyncImage(
        model = coverModel ?: fallbackUrl,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}
