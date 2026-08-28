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

    var loadFailed by remember(song.id) { mutableStateOf(false) }
    var onlineCoverUrl by remember(song.id) { mutableStateOf(SongCoverRepository.getCachedCoverUrl(song)) }

    // If local cover is null OR if loading local cover failed, fetch online!
    LaunchedEffect(song.id, loadFailed) {
        if ((song.coverUri == null || loadFailed) && onlineCoverUrl == null) {
            val url = SongCoverRepository.getSongCoverUrl(song)
            if (url != null) {
                onlineCoverUrl = url
            }
        }
    }

    val currentModel = if (!loadFailed && song.coverUri != null) {
        song.coverUri
    } else {
        onlineCoverUrl ?: fallbackUrl
    }

    AsyncImage(
        model = currentModel,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        onError = {
            if (!loadFailed) {
                loadFailed = true
            }
        }
    )
}
