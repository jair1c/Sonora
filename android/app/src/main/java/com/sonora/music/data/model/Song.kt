package com.sonora.music.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val filePath: String,
    val coverUri: Uri?,
    val dateAdded: Long,
    val dateModified: Long,
    val sizeBytes: Long = 0,
    val year: Int = 0,
    val isLiked: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val lyrics: List<LyricLine> = emptyList()
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val audioQualityBadge: String
        get() {
            val pathLower = filePath.lowercase()
            return when {
                pathLower.endsWith(".flac") -> "FLAC • 24-bit Hi-Res"
                pathLower.endsWith(".wav") -> "WAV • Lossless"
                pathLower.endsWith(".m4a") || pathLower.endsWith(".aac") -> "AAC • 256 kbps"
                pathLower.endsWith(".ogg") || pathLower.endsWith(".opus") -> "OGG • 320 kbps"
                pathLower.endsWith(".mp3") -> "MP3 • 320 kbps"
                else -> "Hi-Fi Audio"
            }
        }
}

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val avatarUri: Uri? = null
)

data class Album(
    val title: String,
    val artist: String,
    val trackCount: Int,
    val coverUri: Uri? = null,
    val songs: List<Song> = emptyList()
)

data class FolderGroup(
    val folderName: String,
    val songCount: Int,
    val isBlacklisted: Boolean = false,
    val songs: List<Song> = emptyList()
)

enum class SortMode(val label: String) {
    TITLE_AZ("Nombre (A → Z)"),
    TITLE_ZA("Nombre (Z → A)"),
    ARTIST_AZ("Artista (A → Z)"),
    DATE_ADDED_DESC("Fecha de Adición (Más Recientes)"),
    DURATION_DESC("Mayor Duración")
}

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long> = emptyList(),
    val icon: String = "playlist"
)

