package com.sonora.music.data.repository

import com.sonora.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object SongCoverRepository {

    private val cache = ConcurrentHashMap<String, String?>()

    private fun cleanTitle(rawTitle: String): String {
        var t = rawTitle.trim()
        t = t.replace(Regex("(?i)\\s*[\\(\\[](?:official|video|audio|lyrics?|lyric|remaster(?:ed)?|hd|4k|hq|remix|extended|club|edit|prod|feat|ft)[^\\)\\]]*[\\)\\]]"), "").trim()
        t = t.trim('"', '\'', ' ', ',', ';', '-', '/')
        return if (t.isNotEmpty()) t else rawTitle.trim()
    }

    fun getCachedCoverUrl(song: Song): String? {
        val key = "${song.artist}__${song.title}".lowercase()
        return cache[key]
    }

    suspend fun getSongCoverUrl(song: Song): String? {
        if (song.coverUri != null) {
            return song.coverUri.toString()
        }

        val key = "${song.artist}__${song.title}".lowercase()
        if (cache.containsKey(key)) {
            return cache[key]
        }

        val cleanT = cleanTitle(song.title)
        val primaryArtist = ArtistImageRepository.extractPrimaryArtist(song.artist)

        return withContext(Dispatchers.IO) {
            var url = fetchFromDeezer(cleanT, primaryArtist)

            if (url == null) {
                url = fetchFromITunes(cleanT, primaryArtist)
            }

            if (url == null && song.album.isNotBlank() && !song.album.equals("Álbum desconocido", ignoreCase = true)) {
                url = fetchAlbumFromDeezer(song.album, primaryArtist)
            }

            if (url != null) {
                cache[key] = url
            }
            url
        }
    }

    private fun fetchFromDeezer(title: String, artist: String): String? {
        return try {
            val query = if (artist.isNotEmpty()) "track:\"$title\" artist:\"$artist\"" else title
            val encoded = URLEncoder.encode(query, "UTF-8")
            val apiUrl = "https://api.deezer.com/search?q=" + encoded + "&limit=3"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SonoraMusicApp/3.7.0")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val data = root.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val firstItem = data.getJSONObject(0)
                    val album = firstItem.optJSONObject("album")
                    if (album != null) {
                        val coverXl = album.optString("cover_xl", "")
                        val coverBig = album.optString("cover_big", "")
                        val coverMed = album.optString("cover_medium", "")
                        val cover = if (coverXl.isNotEmpty()) coverXl else if (coverBig.isNotEmpty()) coverBig else coverMed
                        if (cover.isNotEmpty() && !cover.contains("default") && !cover.contains("d41d8cd98f00b204e9800998ecf8427e")) {
                            return cover
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchAlbumFromDeezer(albumName: String, artist: String): String? {
        return try {
            val query = if (artist.isNotEmpty()) "album:\"$albumName\" artist:\"$artist\"" else albumName
            val encoded = URLEncoder.encode(query, "UTF-8")
            val apiUrl = "https://api.deezer.com/search/album?q=" + encoded + "&limit=1"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SonoraMusicApp/3.7.0")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val data = root.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val firstItem = data.getJSONObject(0)
                    val coverXl = firstItem.optString("cover_xl", "")
                    val coverBig = firstItem.optString("cover_big", "")
                    val coverMed = firstItem.optString("cover_medium", "")
                    val cover = if (coverXl.isNotEmpty()) coverXl else if (coverBig.isNotEmpty()) coverBig else coverMed
                    if (cover.isNotEmpty()) return cover
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromITunes(title: String, artist: String): String? {
        return try {
            val term = "$title $artist".trim()
            val encoded = URLEncoder.encode(term, "UTF-8")
            val apiUrl = "https://itunes.apple.com/search?term=" + encoded + "&entity=song&limit=1&media=music"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SonoraMusicApp/3.7.0")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val results = root.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val artwork100 = first.optString("artworkUrl100", "")
                    if (artwork100.isNotEmpty()) {
                        return artwork100.replace("100x100bb", "600x600bb")
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
