package com.sonora.music.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object ArtistImageRepository {

    private val cache = ConcurrentHashMap<String, String?>()

    suspend fun getArtistImageUrl(artistName: String): String? {
        val cleanName = artistName.trim()
        if (cleanName.isEmpty() || cleanName.equals("<unknown>", ignoreCase = true)) {
            return null
        }

        if (cache.containsKey(cleanName)) {
            return cache[cleanName]
        }

        return withContext(Dispatchers.IO) {
            val url = fetchFromDeezer(cleanName) ?: fetchFromITunes(cleanName)
            if (url != null) {
                cache[cleanName] = url
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
                setRequestProperty("User-Agent", "SonoraMusicApp/3.7.0")
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
                        val picture = if (pictureBig.isNotEmpty()) pictureBig else pictureMedium

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
