package com.sonora.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sonora.music.data.model.LyricLine
import com.sonora.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

class MediaStoreRepository(private val context: Context) {

    private val coversDir: File by lazy {
        File(context.cacheDir, "sonora_covers").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun queryLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        val selection = "${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI


        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Canción sin título"
                    val artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Artista desconocido"
                    val album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" } ?: "Álbum desconocido"
                    val durationMs = cursor.getLong(durationCol)
                    val filePath = cursor.getString(dataCol) ?: ""
                    val sizeBytes = cursor.getLong(sizeCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val year = cursor.getInt(yearCol)
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                    val dateModified = cursor.getLong(dateModifiedCol) * 1000L

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    
                    // Album Cover URI via standard MediaStore albumart Content URI
                    val coverUri = if (albumId > 0) {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                    } else null
                    
                    // Lazy synchronized .lrc or embedded lyrics
                    val lyrics = emptyList<LyricLine>()

                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            contentUri = contentUri,
                            filePath = filePath,
                            coverUri = coverUri,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            sizeBytes = sizeBytes,
                            year = year,
                            lyrics = lyrics
                        )
                    )
                }
            }
            android.util.Log.d("SonoraMedia", "Queried ${songsList.size} songs successfully from MediaStore")
        } catch (e: Exception) {
            android.util.Log.e("SonoraMedia", "Error querying MediaStore", e)
        }

        songsList
    }


    private fun loadLyrics(filePath: String, durationMs: Long): List<LyricLine> {
        if (filePath.isEmpty()) return emptyList()

        // 1. Check for accompanying .lrc file in the same directory
        val dotIndex = filePath.lastIndexOf('.')
        if (dotIndex != -1) {
            val lrcPath = filePath.substring(0, dotIndex) + ".lrc"
            val lrcFile = File(lrcPath)
            if (lrcFile.exists() && lrcFile.canRead()) {
                try {
                    val lines = mutableListOf<LyricLine>()
                    val regex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\](.*)""")
                    BufferedReader(InputStreamReader(FileInputStream(lrcFile))).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            val match = regex.find(line)
                            if (match != null) {
                                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                                val millisStr = match.groupValues[3].padEnd(3, '0').take(3)
                                val millis = millisStr.toLongOrNull() ?: 0L
                                val totalMs = (minutes * 60 + seconds) * 1000 + millis
                                val text = match.groupValues[4].trim()
                                if (text.isNotEmpty()) {
                                    lines.add(LyricLine(timeMs = totalMs, text = text))
                                }
                            }
                            line = reader.readLine()
                        }
                    }
                    if (lines.isNotEmpty()) return lines.sortedBy { it.timeMs }
                } catch (ignored: Exception) {}
            }
        }

        // 2. Check embedded lyrics tag
        if (File(filePath).exists()) {
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(filePath)
                val embedded = mmr.extractMetadata(1000) // METADATA_KEY_LYRICS
                mmr.release()
                if (!embedded.isNullOrBlank()) {
                    val rawLines = embedded.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    val stepMs = if (rawLines.isNotEmpty()) (durationMs / rawLines.size) else 3000L
                    return rawLines.mapIndexed { idx, text ->
                        LyricLine(timeMs = idx * stepMs, text = text)
                    }
                }
            } catch (ignored: Exception) {}
        }

        return emptyList()
    }
}
