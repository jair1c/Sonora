package com.sonora.music.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class ArtistBioInfo(
    val name: String,
    val fans: Int = 0,
    val topTracks: List<String> = emptyList()
)

object ArtistImageRepository {

    private val cache = ConcurrentHashMap<String, String?>()
    private val bioCache = ConcurrentHashMap<String, ArtistBioInfo?>()

    suspend fun getArtistBioInfo(artistName: String): ArtistBioInfo? {
        val clean = extractPrimaryArtist(artistName)
        if (clean.isEmpty()) return null
        if (bioCache.containsKey(clean)) return bioCache[clean]

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(clean, "UTF-8")
                val apiUrl = "https://api.deezer.com/search/artist/?q=" + encoded + "&limit=1"
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "SonoraMusicApp/3.8.2")
                }

                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(jsonStr)
                    val data = root.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        val first = data.getJSONObject(0)
                        val id = first.optLong("id", 0L)
                        val name = first.optString("name", clean)
                        val fans = first.optInt("nb_fan", 0)

                        val topList = mutableListOf<String>()
                        if (id > 0) {
                            try {
                                val topUrl = "https://api.deezer.com/artist/" + id + "/top?limit=5"
                                val topConn = (URL(topUrl).openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 3000
                                    readTimeout = 3000
                                    requestMethod = "GET"
                                    setRequestProperty("User-Agent", "SonoraMusicApp/3.8.2")
                                }
                                if (topConn.responseCode == 200) {
                                    val topJson = topConn.inputStream.bufferedReader().use { it.readText() }
                                    val topRoot = JSONObject(topJson)
                                    val topData = topRoot.optJSONArray("data")
                                    if (topData != null) {
                                        for (j in 0 until topData.length()) {
                                            val track = topData.getJSONObject(j)
                                            val tTitle = track.optString("title", "")
                                            if (tTitle.isNotEmpty()) topList.add(tTitle)
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }

                        val info = ArtistBioInfo(name = name, fans = fans, topTracks = topList)
                        bioCache[clean] = info
                        return@withContext info
                    }
                }
                bioCache[clean] = null
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    fun extractPrimaryArtist(rawName: String): String {
        var name = rawName.trim()
        if (name.isEmpty() || name.equals("<unknown>", ignoreCase = true) || name.equals("unknown", ignoreCase = true) || name.equals("Artista desconocido", ignoreCase = true)) {
            return ""
        }

        // 1. Remove parenthesized or bracketed feat/ft/with/prod
        name = removeEnclosedFeat(name, '(', ')')
        name = removeEnclosedFeat(name, '[', ']')

        // 2. Split by standard delimiters and collaboration keywords
        val delimiters = arrayOf(
            " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ", " vs. ", " vs ",
            " & ", " / ", " | ", " x ", " X ", " + ", ",", ";", "/", "\\"
        )
        for (d in delimiters) {
            val idx = name.indexOf(d, ignoreCase = true)
            if (idx > 0) {
                name = name.substring(0, idx).trim()
            }
        }

        // 3. Clean trailing / leading punctuation
        val cleaned = name.trim { it <= ' ' || it == '"' || it == '\'' || it == '-' || it == ',' || it == ';' || it == '&' || it == '/' || it == '\\' }
        return if (cleaned.isNotEmpty()) cleaned else rawName.trim()
    }

    private fun removeEnclosedFeat(text: String, open: Char, close: Char): String {
        var result = text
        var start = result.indexOf(open)
        while (start != -1) {
            val end = result.indexOf(close, start + 1)
            if (end != -1) {
                val inner = result.substring(start + 1, end).lowercase()
                if (inner.contains("feat") || inner.contains("ft") || inner.contains("with") || inner.contains("prod")) {
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

    fun getCachedArtistImageUrl(artistName: String): String? {
        val clean = artistName.trim()
        if (cache.containsKey(clean)) return cache[clean]
        val primary = extractPrimaryArtist(clean)
        if (primary.isNotEmpty() && cache.containsKey(primary)) return cache[primary]
        return null
    }

    suspend fun getArtistImageUrl(artistName: String): String? {
        val cleanName = artistName.trim()
        if (cleanName.isEmpty() || cleanName.equals("<unknown>", ignoreCase = true) || cleanName.equals("unknown", ignoreCase = true) || cleanName.equals("Artista desconocido", ignoreCase = true)) {
            return null
        }

        val primaryName = extractPrimaryArtist(cleanName)

        // Check memory cache
        if (cache.containsKey(cleanName)) {
            return cache[cleanName]
        }
        if (primaryName.isNotEmpty() && cache.containsKey(primaryName)) {
            return cache[primaryName]
        }

        return withContext(Dispatchers.IO) {
            // 1. Try primary artist on Deezer first (highest quality artist profile photo)
            var url = if (primaryName.isNotEmpty()) fetchFromDeezer(primaryName) else null

            // 2. If no result, try full clean artist name on Deezer
            if (url == null && cleanName != primaryName) {
                url = fetchFromDeezer(cleanName)
            }

            // 3. Fallback to iTunes with primary artist
            if (url == null && primaryName.isNotEmpty()) {
                url = fetchFromITunes(primaryName)
            }

            // 4. Fallback to iTunes with full name
            if (url == null && cleanName != primaryName) {
                url = fetchFromITunes(cleanName)
            }

            if (url != null) {
                cache[cleanName] = url
                if (primaryName.isNotEmpty()) {
                    cache[primaryName] = url
                }
            }
            url
        }
    }

    private fun fetchFromDeezer(artistName: String): String? {
        return try {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val apiUrl = "https://api.deezer.com/search/artist/?q=" + encoded + "&limit=3"
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
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val pictureBig = item.optString("picture_big", "")
                        val pictureMedium = item.optString("picture_medium", "")
                        val pictureXl = item.optString("picture_xl", "")
                        val picture = if (pictureXl.isNotEmpty()) pictureXl else if (pictureBig.isNotEmpty()) pictureBig else pictureMedium

                        if (picture.isNotEmpty() && !picture.contains("default") && !picture.contains("d41d8cd98f00b204e9800998ecf8427e")) {
                            return picture
                        }
                    }
                    val firstItem = data.getJSONObject(0)
                    val pic = firstItem.optString("picture_big", firstItem.optString("picture_medium", ""))
                    if (pic.isNotEmpty()) return pic
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromITunes(artistName: String): String? {
        return try {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val apiUrl = "https://itunes.apple.com/search?term=" + encoded + "&entity=album&limit=1&media=music"
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
