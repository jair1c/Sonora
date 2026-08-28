package com.sonora.music.data.local

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import com.sonora.music.data.model.Playlist
import com.sonora.music.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class SonoraPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sonora_native_prefs", Context.MODE_PRIVATE)

    private val _prefsRevision = MutableStateFlow(0)
    val prefsRevision = _prefsRevision.asStateFlow()

    fun notifyPrefsChanged() {
        _prefsRevision.value++
    }

    companion object {
        private const val KEY_LIKED_IDS = "sonora_liked_song_ids"
        private const val KEY_RECENT_IDS = "sonora_recent_song_ids"
        private const val KEY_PLAY_COUNTS = "sonora_play_counts_json"
        private const val KEY_TOTAL_LISTENING_SECONDS = "sonora_total_listening_seconds"
        private const val KEY_BLACKLISTED_FOLDERS = "sonora_blacklisted_folders"
        private const val KEY_CUSTOM_PLAYLISTS = "sonora_custom_playlists_json"
        private const val KEY_LAST_SONG_ID = "sonora_last_song_id"
        private const val KEY_LAST_POSITION_MS = "sonora_last_position_ms"
        private const val KEY_LAST_QUEUE_IDS = "sonora_last_queue_ids"
        private const val KEY_THEME_MODE = "sonora_theme_mode" // "system", "dark", "light"
        private const val KEY_SORT_MODE = "sonora_sort_mode"
        private const val KEY_EQ_PRESET = "sonora_eq_preset"
        private const val KEY_BASS_BOOST = "sonora_bass_boost_level"
        private const val KEY_PRE_AMP_GAIN = "sonora_pre_amp_gain_db"
        private const val KEY_AUTO_VOLUME_LEVELING = "sonora_auto_volume_leveling"
        private const val KEY_HAS_SEEN_WELCOME = "sonora_has_seen_welcome"
        private const val KEY_PETAL_ROUNDNESS = "sonora_petal_roundness"
        private const val KEY_CROSSFADE_SECONDS = "sonora_crossfade_seconds"
        private const val KEY_PLAYBACK_SPEED = "sonora_playback_speed"
        private const val KEY_NAV_TABS = "sonora_nav_tabs_json"
        private const val KEY_NAV_LABEL_MODE = "sonora_nav_label_mode" // "active_only", "always", "never"
        private const val KEY_PLAYER_CONTROLS_STYLE = "sonora_player_controls_style" // "dock", "circles", "organic", "squircle", "waveform"
    }

    // --- SORT MODE ---
    fun getSortMode(): String = prefs.getString(KEY_SORT_MODE, "TITLE_AZ") ?: "TITLE_AZ"
    fun setSortMode(mode: String) { prefs.edit().putString(KEY_SORT_MODE, mode).apply(); notifyPrefsChanged() }

    // --- TOOLS & PREFERENCES ---
    fun getPlayerControlsStyle(): String = prefs.getString(KEY_PLAYER_CONTROLS_STYLE, "dock") ?: "dock"
    fun setPlayerControlsStyle(style: String) = prefs.edit().putString(KEY_PLAYER_CONTROLS_STYLE, style).apply()

    fun getNavLabelMode(): String = prefs.getString(KEY_NAV_LABEL_MODE, "active_only") ?: "active_only"
    fun setNavLabelMode(mode: String) = prefs.edit().putString(KEY_NAV_LABEL_MODE, mode).apply()

    fun getPetalRoundness(): Int = prefs.getInt(KEY_PETAL_ROUNDNESS, 30)
    fun setPetalRoundness(value: Int) = prefs.edit().putInt(KEY_PETAL_ROUNDNESS, value).apply()

    fun getCrossfadeSeconds(): Int = prefs.getInt(KEY_CROSSFADE_SECONDS, 0)
    fun setCrossfadeSeconds(sec: Int) = prefs.edit().putInt(KEY_CROSSFADE_SECONDS, sec).apply()

    fun getPlaybackSpeed(): Float = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
    fun setPlaybackSpeed(speed: Float) = prefs.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()

    fun getNavTabs(): List<String> {
        val json = prefs.getString(KEY_NAV_TABS, "[\"canciones\",\"listas\",\"ajustes\"]") ?: "[\"canciones\",\"listas\",\"ajustes\"]"
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (_: Exception) {}
        return if (list.isEmpty()) listOf("canciones", "listas", "ajustes") else list
    }

    fun setNavTabs(tabs: List<String>) {
        val arr = JSONArray()
        tabs.forEach { arr.put(it) }
        prefs.edit().putString(KEY_NAV_TABS, arr.toString()).apply()
    }

    // --- FAVORITES ---
    fun getLikedSongIds(): Set<Long> {
        val set = prefs.getStringSet(KEY_LIKED_IDS, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun toggleLikeSong(songId: Long): Boolean {
        val current = getLikedSongIds().toMutableSet()
        val isNowLiked = if (current.contains(songId)) {
            current.remove(songId)
            false
        } else {
            current.add(songId)
            true
        }
        prefs.edit().putStringSet(KEY_LIKED_IDS, current.map { it.toString() }.toSet()).apply()
        return isNowLiked
    }

    fun isSongLiked(songId: Long): Boolean {
        return getLikedSongIds().contains(songId)
    }

    // --- PLAY COUNTS & REAL-TIME LISTENING STATS ---
    fun incrementPlayCount(songId: Long) {
        // Update Play Count
        val countsJson = prefs.getString(KEY_PLAY_COUNTS, "{}") ?: "{}"
        val jsonObj = try { JSONObject(countsJson) } catch (_: Exception) { JSONObject() }
        val currentCount = jsonObj.optInt(songId.toString(), 0)
        jsonObj.put(songId.toString(), currentCount + 1)
        prefs.edit().putString(KEY_PLAY_COUNTS, jsonObj.toString()).apply()

        // Update Recents
        val recentList = getRecentSongIds().toMutableList()
        recentList.remove(songId)
        recentList.add(0, songId)
        if (recentList.size > 50) {
            recentList.removeAt(recentList.lastIndex)
        }
        val arr = JSONArray()
        recentList.forEach { arr.put(it) }
        prefs.edit().putString(KEY_RECENT_IDS, arr.toString()).apply()
    }

    fun addListeningSeconds(seconds: Long) {
        if (seconds <= 0L) return
        val currentTotalSec = prefs.getLong(KEY_TOTAL_LISTENING_SECONDS, 0L)
        prefs.edit().putLong(KEY_TOTAL_LISTENING_SECONDS, currentTotalSec + seconds).apply()
    }

    fun recordPlay(songId: Long, durationMs: Long = 0L) {
        incrementPlayCount(songId)
    }

    fun getTotalListeningSeconds(): Long = prefs.getLong(KEY_TOTAL_LISTENING_SECONDS, 0L)

    fun getTotalListeningMinutes(): Int {
        val totalSec = getTotalListeningSeconds()
        return (totalSec / 60L).toInt()
    }

    fun getPlayCounts(): Map<Long, Int> {
        val countsJson = prefs.getString(KEY_PLAY_COUNTS, "{}") ?: "{}"
        val map = mutableMapOf<Long, Int>()
        try {
            val jsonObj = JSONObject(countsJson)
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val id = k.toLongOrNull()
                if (id != null) {
                    map[id] = jsonObj.optInt(k, 0)
                }
            }
        } catch (_: Exception) {}
        return map
    }

    fun getRecentSongIds(): List<Long> {
        val jsonStr = prefs.getString(KEY_RECENT_IDS, "[]") ?: "[]"
        val list = mutableListOf<Long>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(arr.getLong(i))
            }
        } catch (_: Exception) {}
        return list
    }

    // --- BLACKLISTED FOLDERS ---
    fun getBlacklistedFolders(): Set<String> {
        val raw = prefs.getStringSet(KEY_BLACKLISTED_FOLDERS, emptySet()) ?: emptySet()
        return HashSet(raw)
    }

    fun toggleBlacklistFolder(folderName: String): Boolean {
        val current = HashSet(getBlacklistedFolders())
        val isBlocked = if (current.contains(folderName)) {
            current.remove(folderName)
            false
        } else {
            current.add(folderName)
            true
        }
        prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, current).apply()
        notifyPrefsChanged()
        return isBlocked
    }

    fun blacklistFolder(folderName: String) {
        val current = HashSet(getBlacklistedFolders())
        current.add(folderName)
        prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, current).apply()
        notifyPrefsChanged()
    }

    fun unblacklistFolder(folderName: String) {
        val current = HashSet(getBlacklistedFolders())
        current.remove(folderName)
        prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, current).apply()
        notifyPrefsChanged()
    }

    // --- CUSTOM PLAYLISTS ---
    fun createCustomPlaylist(name: String): Playlist = createPlaylist(name)

    fun deleteCustomPlaylist(playlistId: String) {
        val list = getCustomPlaylists().filter { it.id != playlistId }
        saveCustomPlaylists(list)
    }

    fun getCustomPlaylists(): List<Playlist> {
        val jsonStr = prefs.getString(KEY_CUSTOM_PLAYLISTS, "[]") ?: "[]"
        val list = mutableListOf<Playlist>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val songIdsArr = obj.getJSONArray("songIds")
                val songIds = mutableListOf<Long>()
                for (j in 0 until songIdsArr.length()) {
                    songIds.add(songIdsArr.getLong(j))
                }
                list.add(Playlist(id = id, name = name, songIds = songIds))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveCustomPlaylists(playlists: List<Playlist>) {
        val arr = JSONArray()
        for (pl in playlists) {
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            val idsArr = JSONArray()
            pl.songIds.forEach { idsArr.put(it) }
            obj.put("songIds", idsArr)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_PLAYLISTS, arr.toString()).apply()
    }

    fun createPlaylist(name: String): Playlist {
        val newPl = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name,
            songIds = emptyList()
        )
        val list = getCustomPlaylists().toMutableList()
        list.add(newPl)
        saveCustomPlaylists(list)
        return newPl
    }

    fun addSongToPlaylist(playlistId: String, songId: Long) {
        val list = getCustomPlaylists().toMutableList()
        val idx = list.indexOfFirst { it.id == playlistId }
        if (idx != -1) {
            val pl = list[idx]
            if (!pl.songIds.contains(songId)) {
                val updatedIds = pl.songIds + songId
                list[idx] = pl.copy(songIds = updatedIds)
                saveCustomPlaylists(list)
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        val list = getCustomPlaylists().toMutableList()
        val idx = list.indexOfFirst { it.id == playlistId }
        if (idx != -1) {
            val pl = list[idx]
            val updatedIds = pl.songIds.filter { it != songId }
            list[idx] = pl.copy(songIds = updatedIds)
            saveCustomPlaylists(list)
        }
    }

    fun deletePlaylist(playlistId: String) {
        val list = getCustomPlaylists().filter { it.id != playlistId }
        saveCustomPlaylists(list)
    }

    // --- LAST PLAYBACK STATE ---
    fun saveLastPlayback(songId: Long, positionMs: Long, queueIds: List<Long> = emptyList()) {
        val editor = prefs.edit()
            .putLong(KEY_LAST_SONG_ID, songId)
            .putLong(KEY_LAST_POSITION_MS, positionMs)
        if (queueIds.isNotEmpty()) {
            val arr = JSONArray()
            queueIds.forEach { arr.put(it) }
            editor.putString(KEY_LAST_QUEUE_IDS, arr.toString())
        }
        editor.apply()
    }

    fun setLastSongId(songId: Long) = prefs.edit().putLong(KEY_LAST_SONG_ID, songId).apply()
    fun setLastPositionMs(pos: Long) = prefs.edit().putLong(KEY_LAST_POSITION_MS, pos).apply()
    fun setLastQueueIds(queueIds: List<Long>) {
        val arr = JSONArray()
        queueIds.forEach { arr.put(it) }
        prefs.edit().putString(KEY_LAST_QUEUE_IDS, arr.toString()).apply()
    }

    fun getLastSongId(): Long = prefs.getLong(KEY_LAST_SONG_ID, -1L)
    fun getLastPositionMs(): Long = prefs.getLong(KEY_LAST_POSITION_MS, 0L)
    fun getLastQueueIds(): List<Long> {
        val jsonStr = prefs.getString(KEY_LAST_QUEUE_IDS, "[]") ?: "[]"
        val list = mutableListOf<Long>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(arr.getLong(i))
            }
        } catch (_: Exception) {}
        return list
    }

    // --- SETTINGS (Theme, Equalizer, Welcome) ---
    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
    fun setThemeMode(mode: String) = prefs.edit().putString(KEY_THEME_MODE, mode).apply()

    fun getEqualizerPreset(): Int = prefs.getInt(KEY_EQ_PRESET, 0)
    fun setEqualizerPreset(preset: Int) = prefs.edit().putInt(KEY_EQ_PRESET, preset).apply()

    fun getBassBoost(): Int = prefs.getInt(KEY_BASS_BOOST, 0)
    fun setBassBoost(level: Int) = prefs.edit().putInt(KEY_BASS_BOOST, level).apply()

    fun hasSeenWelcome(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)
    fun setHasSeenWelcome(seen: Boolean) = prefs.edit().putBoolean(KEY_HAS_SEEN_WELCOME, seen).apply()

    // --- BACKUP & RESTORE ---
    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        // Appearance
        root.put("themeMode", getThemeMode())
        root.put("petalRoundness", getPetalRoundness())
        // Playback
        root.put("crossfadeSeconds", getCrossfadeSeconds())
        root.put("playbackSpeed", getPlaybackSpeed())
        root.put("sortMode", getSortMode())
        // Navigation & UI
        root.put("navLabelMode", getNavLabelMode())
        root.put("playerControlsStyle", getPlayerControlsStyle())
        // Equalizer / DSP
        root.put("eqPreset", getEqualizerPreset())
        root.put("bassBoost", getBassBoost())
        root.put("preAmpGain", getPreAmpGain())
        root.put("autoVolumeLeveling", isAutoVolumeLeveling())
        // Stats
        root.put("totalListeningSeconds", getTotalListeningSeconds())

        val likedArr = JSONArray()
        getLikedSongIds().forEach { likedArr.put(it) }
        root.put("likedSongIds", likedArr)

        val recentArr = JSONArray()
        getRecentSongIds().forEach { recentArr.put(it) }
        root.put("recentSongIds", recentArr)

        val countsJson = prefs.getString(KEY_PLAY_COUNTS, "{}") ?: "{}"
        root.put("playCounts", JSONObject(countsJson))

        val blacklistArr = JSONArray()
        getBlacklistedFolders().forEach { blacklistArr.put(it) }
        root.put("blacklistedFolders", blacklistArr)

        val playlistsArr = JSONArray()
        for (pl in getCustomPlaylists()) {
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            val ids = JSONArray()
            pl.songIds.forEach { ids.put(it) }
            obj.put("songIds", ids)
            playlistsArr.put(obj)
        }
        root.put("customPlaylists", playlistsArr)

        val navArr = JSONArray()
        getNavTabs().forEach { navArr.put(it) }
        root.put("navTabs", navArr)

        return root.toString(2)
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val editor = prefs.edit()

            // Appearance
            if (root.has("themeMode")) editor.putString(KEY_THEME_MODE, root.getString("themeMode"))
            if (root.has("petalRoundness")) editor.putInt(KEY_PETAL_ROUNDNESS, root.getInt("petalRoundness"))
            // Playback
            if (root.has("crossfadeSeconds")) editor.putInt(KEY_CROSSFADE_SECONDS, root.getInt("crossfadeSeconds"))
            if (root.has("playbackSpeed")) editor.putFloat(KEY_PLAYBACK_SPEED, root.getDouble("playbackSpeed").toFloat())
            if (root.has("sortMode")) editor.putString(KEY_SORT_MODE, root.getString("sortMode"))
            // Navigation & UI
            if (root.has("navLabelMode")) editor.putString(KEY_NAV_LABEL_MODE, root.getString("navLabelMode"))
            if (root.has("playerControlsStyle")) editor.putString(KEY_PLAYER_CONTROLS_STYLE, root.getString("playerControlsStyle"))
            // Equalizer / DSP
            if (root.has("eqPreset")) editor.putInt(KEY_EQ_PRESET, root.getInt("eqPreset"))
            if (root.has("bassBoost")) editor.putInt(KEY_BASS_BOOST, root.getInt("bassBoost"))
            if (root.has("preAmpGain")) editor.putFloat(KEY_PRE_AMP_GAIN, root.getDouble("preAmpGain").toFloat())
            if (root.has("autoVolumeLeveling")) editor.putBoolean(KEY_AUTO_VOLUME_LEVELING, root.getBoolean("autoVolumeLeveling"))
            // Stats
            if (root.has("totalListeningSeconds")) editor.putLong(KEY_TOTAL_LISTENING_SECONDS, root.getLong("totalListeningSeconds"))

            // Liked songs
            if (root.has("likedSongIds")) {
                val arr = root.getJSONArray("likedSongIds")
                val set = HashSet<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getLong(i).toString())
                }
                editor.putStringSet(KEY_LIKED_IDS, set)
            }

            // Recent songs
            if (root.has("recentSongIds")) {
                val arr = root.getJSONArray("recentSongIds")
                val recentList = mutableListOf<Long>()
                for (i in 0 until arr.length()) {
                    recentList.add(arr.getLong(i))
                }
                val recentArr = JSONArray()
                recentList.forEach { recentArr.put(it) }
                editor.putString(KEY_RECENT_IDS, recentArr.toString())
            }

            // Play counts
            if (root.has("playCounts")) {
                editor.putString(KEY_PLAY_COUNTS, root.getJSONObject("playCounts").toString())
            }

            // Blacklisted folders
            if (root.has("blacklistedFolders")) {
                val arr = root.getJSONArray("blacklistedFolders")
                val set = HashSet<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                editor.putStringSet(KEY_BLACKLISTED_FOLDERS, set)
            }

            // Custom Playlists
            if (root.has("customPlaylists")) {
                val arr = root.getJSONArray("customPlaylists")
                editor.putString(KEY_CUSTOM_PLAYLISTS, arr.toString())
            }

            // Navigation tabs order
            if (root.has("navTabs")) {
                editor.putString(KEY_NAV_TABS, root.getJSONArray("navTabs").toString())
            }

            val committed = editor.commit()
            notifyPrefsChanged()
            committed
        } catch (_: Exception) {
            false
        }
    }

    // --- INSTANT STARTUP CACHED SONGS ---
    fun getCachedSongs(): List<Song> {
        val cacheFile = File(context.filesDir, "sonora_songs_cache.json")
        if (!cacheFile.exists()) return emptyList()
        return try {
            val json = cacheFile.readText()
            val arr = JSONArray(json)
            val list = ArrayList<Song>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getLong("id")
                val albumId = obj.optLong("albumId", 0L)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val coverUri = if (albumId > 0) {
                    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                } else null

                list.add(
                    Song(
                        id = id,
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.getString("album"),
                        durationMs = obj.getLong("durationMs"),
                        contentUri = contentUri,
                        filePath = obj.optString("filePath", ""),
                        coverUri = coverUri,
                        dateAdded = obj.optLong("dateAdded", 0L),
                        dateModified = obj.optLong("dateModified", 0L),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        year = obj.optInt("year", 0),
                        lyrics = emptyList()
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setCachedSongs(songs: List<Song>) {
        try {
            val cacheFile = File(context.filesDir, "sonora_songs_cache.json")
            val arr = JSONArray()
            songs.forEach { song ->
                val obj = JSONObject()
                obj.put("id", song.id)
                obj.put("title", song.title)
                obj.put("artist", song.artist)
                obj.put("album", song.album)
                obj.put("durationMs", song.durationMs)
                obj.put("filePath", song.filePath)
                obj.put("dateAdded", song.dateAdded)
                obj.put("dateModified", song.dateModified)
                obj.put("sizeBytes", song.sizeBytes)
                obj.put("year", song.year)
                val albumId = song.coverUri?.lastPathSegment?.toLongOrNull() ?: 0L
                obj.put("albumId", albumId)
                arr.put(obj)
            }
            cacheFile.writeText(arr.toString())
        } catch (_: Exception) {}
    }

    // --- PRE-AMP GAIN (dB) ---
    fun getPreAmpGain(): Float = prefs.getFloat(KEY_PRE_AMP_GAIN, 0f)
    fun setPreAmpGain(gainDb: Float) = prefs.edit().putFloat(KEY_PRE_AMP_GAIN, gainDb).apply()

    // --- AUTO VOLUME LEVELING ---
    fun isAutoVolumeLeveling(): Boolean = prefs.getBoolean(KEY_AUTO_VOLUME_LEVELING, false)
    fun setAutoVolumeLeveling(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_VOLUME_LEVELING, enabled).apply()
}
