package com.sonora.music.data.repository

import com.sonora.music.data.model.LyricLine
import com.sonora.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object LrclibRepository {

    private val cache = ConcurrentHashMap<Long, List<LyricLine>>()

    suspend fun getSyncedLyrics(song: Song): List<LyricLine> {
        cache[song.id]?.let { return it }

        return withContext(Dispatchers.IO) {
            val cleanTitle = SongCoverRepository.cleanTitle(song.title)
            val primaryArtist = ArtistImageRepository.extractPrimaryArtist(song.artist)
            val durationSec = (song.durationMs / 1000L).toInt()

            // 1. Try exact get endpoint
            var result = fetchExact(cleanTitle, primaryArtist, durationSec)

            // 2. Fallback to search endpoint
            if (result.isEmpty()) {
                result = fetchSearch(cleanTitle + " " + primaryArtist)
            }

            if (result.isNotEmpty()) {
                cache[song.id] = result
            }
            result
        }
    }

    private fun fetchExact(title: String, artist: String, durationSec: Int): List<LyricLine> {
        return try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            var apiUrl = "https://lrclib.net/api/get?track_name=" + encodedTitle + "&artist_name=" + encodedArtist
            if (durationSec > 0) {
                apiUrl += "&duration=" + durationSec
            }

            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SonoraMusicApp/3.8.2 (https://github.com/jair1c/Sonora)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val synced = root.optString("syncedLyrics", "")
                if (synced.isNotBlank()) {
                    return parseLrcString(synced)
                }
                val plain = root.optString("plainLyrics", "")
                if (plain.isNotBlank()) {
                    return parsePlainString(plain, durationSec * 1000L)
                }
            }
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchSearch(query: String): List<LyricLine> {
        return try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val apiUrl = "https://lrclib.net/api/search?q=" + encoded

            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SonoraMusicApp/3.8.2 (https://github.com/jair1c/Sonora)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonStr)
                if (array.length() > 0) {
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val synced = item.optString("syncedLyrics", "")
                        if (synced.isNotBlank()) {
                            return parseLrcString(synced)
                        }
                    }
                    val first = array.getJSONObject(0)
                    val plain = first.optString("plainLyrics", "")
                    if (plain.isNotBlank()) {
                        val dur = first.optLong("duration", 180L) * 1000L
                        return parsePlainString(plain, dur)
                    }
                }
            }
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseLrcString(lrcContent: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        val lines = lrcContent.split("\n")

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (!line.startsWith("[") || !line.contains("]")) continue
            val closeBracket = line.indexOf(']')
            if (closeBracket <= 1) continue
            val timeTag = line.substring(1, closeBracket).trim()
            val text = line.substring(closeBracket + 1).trim()

            val parts = timeTag.split(":")
            if (parts.size == 2) {
                val min = parts[0].toLongOrNull() ?: 0L
                val secStr = parts[1]
                val secParts = if (secStr.contains(".")) secStr.split(".") else secStr.split(":")
                val sec = secParts[0].toLongOrNull() ?: 0L
                val ms = if (secParts.size > 1) {
                    val fracStr = secParts[1]
                    when (fracStr.length) {
                        1 -> (fracStr.toLongOrNull() ?: 0L) * 100
                        2 -> (fracStr.toLongOrNull() ?: 0L) * 10
                        3 -> fracStr.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                } else 0L
                val timeMs = (min * 60 * 1000) + (sec * 1000) + ms
                result.add(LyricLine(timeMs, text))
            }
        }
        return result.sortedBy { it.timeMs }
    }

    private fun parsePlainString(plainContent: String, durationMs: Long): List<LyricLine> {
        val rawLines = plainContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) return emptyList()
        val intervalMs = if (durationMs > 0) durationMs / rawLines.size else 4000L
        return rawLines.mapIndexed { index, text ->
            LyricLine(timeMs = index * intervalMs, text = text)
        }
    }
}
