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

    fun cleanTitle(rawTitle: String): String {
        var t = rawTitle.trim()
        if (t.isEmpty()) return ""

        val tags = listOf("official", "video", "audio", "lyric", "lyrics", "remaster", "hd", "4k", "hq", "remix", "extended", "club", "edit", "prod", "feat", "ft")
        for (tag in tags) {
            t = removeEnclosedMatching(t, '(', ')', tag)
            t = removeEnclosedMatching(t, '[', ']', tag)
        }

        t = t.trim { it <= ' ' || it == '"' || it == '\'' || it == '-' || it == ',' || it == ';' || it == '/' }
        return if (t.isNotEmpty()) t else rawTitle.trim()
    }

    private fun removeEnclosedMatching(text: String, open: Char, close: Char, keyword: String): String {
        var result = text
        var start = result.indexOf(open)
        while (start != -1) {
            val end = result.indexOf(close, start + 1)
            if (end != -1) {
                val inner = result.substring(start + 1, end).lowercase()
                if (inner.contains(keyword)) {
                    result = result.removeRange(start, end + 1).trim()
                    start = result.indexOf(open)
                } else {
                    start = result.indexOf(open, end + 1)
                }
            } else {
                break
            }
        }
        return result
    }

    fun getCachedCoverUrl(song: Song): String? {
        val key = "${song.artist}__${song.title}".lowercase()
        return cache[key]
    }

    suspend fun getSongCoverUrl(song: Song): String? {
        val key = "${song.artist}__${song.title}".lowercase()
        if (cache.containsKey(key)) {
            return cache[key]
        }

        val cleanT = cleanTitle(song.title)
        val primaryArtist = ArtistImageRepository.extractPrimaryArtist(song.artist)

        return withContext(Dispatchers.IO) {
            // 1. Search Deezer track API with clean title and primary artist
            var url = fetchFromDeezer(cleanT, primaryArtist)

            // 2. Search Deezer with simple full query if first attempt gave null
            if (url == null) {
                url = fetchSimpleDeezer("$cleanT $primaryArtist")
            }

            // 3. Search iTunes API
            if (url == null) {
                url = fetchFromITunes(cleanT, primaryArtist)
            }

            // 4. Fallback to Deezer album search if album name is available
            if (url == null && song.album.isNotBlank() && !song.album.equals("Álbum desconocido", ignoreCase = true) && !song.album.equals("<unknown>", ignoreCase = true)) {
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
                setRequestProperty("User-Agent", "SonoraMusicApp/3.8.0")
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

    private fun fetchSimpleDeezer(query: String): String? {
        return try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val apiUrl = "https://api.deezer.com/search?q=" + encoded + "&limit=3"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SonoraMusicApp/3.8.0")
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
                setRequestProperty("User-Agent", "SonoraMusicApp/3.8.0")
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
                setRequestProperty("User-Agent", "SonoraMusicApp/3.8.0")
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
