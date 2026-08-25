package com.sonora.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.sonora.music.data.model.LyricLine
import com.sonora.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class MediaStoreRepository(private val context: Context) {

    suspend fun queryLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()
        val seenPaths = mutableSetOf<String>()
        val seenIds = mutableSetOf<Long>()

        val audioExtensions = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma", "alac", "aiff", "dsf", "dff", "ape", "mid", "midi")

        // 1. Primary Query: MediaStore.Audio.Media
        try {
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

            // Do not filter out DURATION = 0 or NULL to avoid dropping FLAC, WAV, M4A, OGG
            val selection = "${MediaStore.Audio.Media.SIZE} > 30000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

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
                    val filePath = cursor.getString(dataCol) ?: ""
                    val ext = if (filePath.isNotEmpty()) File(filePath).extension.lowercase() else ""

                    if (filePath.isNotEmpty() && !audioExtensions.contains(ext) && ext.isNotEmpty()) {
                        continue
                    }

                    var title = cursor.getString(titleCol) ?: ""
                    var artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" && it != "unknown" } ?: ""
                    var album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" && it != "unknown" } ?: ""
                    var durationMs = cursor.getLong(durationCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val year = cursor.getInt(yearCol)
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                    val dateModified = cursor.getLong(dateModifiedCol) * 1000L

                    // Fallback metadata extraction for FLAC/WAV/M4A with 0 duration or missing tags
                    if ((durationMs <= 0L || title.isBlank() || artist.isBlank()) && filePath.isNotEmpty() && File(filePath).exists()) {
                        try {
                            val mmr = MediaMetadataRetriever()
                            mmr.setDataSource(filePath)
                            val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            if (durStr != null) durationMs = durStr.toLongOrNull() ?: durationMs
                            if (title.isBlank()) {
                                val t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                if (!t.isNullOrBlank()) title = t
                            }
                            if (artist.isBlank()) {
                                val a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                if (!a.isNullOrBlank()) artist = a
                            }
                            if (album.isBlank()) {
                                val al = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                                if (!al.isNullOrBlank()) album = al
                            }
                            mmr.release()
                        } catch (_: Exception) {}
                    }

                    if (title.isBlank()) {
                        title = if (filePath.isNotEmpty()) File(filePath).nameWithoutExtension else "Canción $id"
                    }
                    if (artist.isBlank()) artist = "Artista desconocido"
                    if (album.isBlank()) album = "Álbum desconocido"
                    if (durationMs <= 0L) durationMs = 180000L

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val coverUri = if (albumId > 0) {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                    } else null

                    if (filePath.isNotEmpty()) seenPaths.add(filePath.lowercase())
                    seenIds.add(id)

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
                            lyrics = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SonoraMedia", "Error querying MediaStore Audio", e)
        }

        // 2. Secondary Query: MediaStore.Files for any unclassified FLAC, M4A, WAV, OGG, OPUS, AAC files
        try {
            val fileProjection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )
            val fileUri = MediaStore.Files.getContentUri("external")
            val selectionArgs = audioExtensions.map { "%.$it" }.toTypedArray()
            val selection = audioExtensions.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" }

            context.contentResolver.query(
                fileUri,
                fileProjection,
                "($selection) AND ${MediaStore.Files.FileColumns.SIZE} > 30000",
                selectionArgs,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val filePath = cursor.getString(dataCol) ?: ""
                    if (filePath.isEmpty() || seenPaths.contains(filePath.lowercase()) || seenIds.contains(id)) {
                        continue
                    }
                    val file = File(filePath)
                    if (!file.exists() || !file.isFile) continue

                    val sizeBytes = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                    val dateModified = cursor.getLong(dateModifiedCol) * 1000L

                    var title = file.nameWithoutExtension
                    var artist = "Artista desconocido"
                    var album = "Álbum desconocido"
                    var durationMs = 180000L

                    try {
                        val mmr = MediaMetadataRetriever()
                        mmr.setDataSource(filePath)
                        val t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        val a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        val al = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        val d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        if (!t.isNullOrBlank()) title = t
                        if (!a.isNullOrBlank()) artist = a
                        if (!al.isNullOrBlank()) album = al
                        if (!d.isNullOrBlank()) durationMs = d.toLongOrNull() ?: durationMs
                        mmr.release()
                    } catch (_: Exception) {}

                    val contentUri = Uri.fromFile(file)
                    seenPaths.add(filePath.lowercase())
                    seenIds.add(id)

                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            contentUri = contentUri,
                            filePath = filePath,
                            coverUri = null,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            sizeBytes = sizeBytes,
                            year = 0,
                            lyrics = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SonoraMedia", "Error querying MediaStore Files fallback", e)
        }

        android.util.Log.d("SonoraMedia", "Total scanned audio files: ${songsList.size}")
        songsList
    }

    private val lyricsCache = java.util.concurrent.ConcurrentHashMap<Long, List<LyricLine>>()

    suspend fun getLyricsForSong(song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        lyricsCache[song.id]?.let { return@withContext it }
        val lyrics = loadLyrics(song.filePath, song.durationMs)
        lyricsCache[song.id] = lyrics
        lyrics
    }

    fun loadLyrics(filePath: String, durationMs: Long): List<LyricLine> {
        if (filePath.isEmpty()) return emptyList()

        val file = File(filePath)
        if (!file.exists()) return emptyList()

        // 1. Check for accompanying .lrc file (e.g., Song.lrc in the same folder)
        val dotIndex = filePath.lastIndexOf('.')
        if (dotIndex != -1) {
            val lrcPath = filePath.substring(0, dotIndex) + ".lrc"
            val lrcFile = File(lrcPath)
            if (lrcFile.exists() && lrcFile.canRead()) {
                val parsed = parseLrcFile(lrcFile, durationMs)
                if (parsed.isNotEmpty()) return parsed
            }
        }

        // 2. Check for any .lrc file in the parent folder with matching name
        val parentDir = file.parentFile
        if (parentDir != null && parentDir.exists() && parentDir.isDirectory) {
            val baseName = file.nameWithoutExtension.lowercase()
            val matchingLrc = parentDir.listFiles { f ->
                f.isFile && f.extension.equals("lrc", ignoreCase = true) &&
                f.nameWithoutExtension.lowercase() == baseName
            }?.firstOrNull()

            if (matchingLrc != null && matchingLrc.canRead()) {
                val parsed = parseLrcFile(matchingLrc, durationMs)
                if (parsed.isNotEmpty()) return parsed
            }
        }

        // 3. Extract embedded lyrics tag (FLAC Vorbis Comment / MP3 USLT / M4A)
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(filePath)
            // Extract lyrics metadata key
            val embedded = mmr.extractMetadata(1000) // MediaMetadataRetriever.METADATA_KEY_LYRICS (or standard metadata tag)
            mmr.release()

            if (!embedded.isNullOrBlank()) {
                return parseLrcString(embedded, durationMs)
            }
        } catch (ignored: Exception) {}

        return emptyList()
    }

    private fun parseLrcFile(file: File, durationMs: Long): List<LyricLine> {
        return try {
            val content = file.readText(Charsets.UTF_8)
            parseLrcString(content, durationMs)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseLrcString(content: String, durationMs: Long): List<LyricLine> {
        val rawLines = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\](.*)""")

        var hasTimestamps = false
        for (raw in rawLines) {
            val match = regex.find(raw)
            if (match != null) {
                hasTimestamps = true
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
        }

        if (hasTimestamps && lines.isNotEmpty()) {
            return lines.sortedBy { it.timeMs }
        }

        // Fallback for unsynchronized plain text lyrics
        if (rawLines.isNotEmpty()) {
            val totalDuration = if (durationMs > 0) durationMs else 180000L
            val stepMs = (totalDuration / rawLines.size).coerceAtLeast(3000L)
            return rawLines.mapIndexed { idx, text ->
                LyricLine(timeMs = idx * stepMs, text = text)
            }
        }

        return emptyList()
    }
}
