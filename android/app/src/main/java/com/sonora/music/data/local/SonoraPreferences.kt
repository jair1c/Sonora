package com.sonora.music.data.local

import android.content.Context
import android.content.SharedPreferences
import com.sonora.music.data.model.Playlist
import com.sonora.music.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

class SonoraPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sonora_native_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LIKED_IDS = "sonora_liked_song_ids"
        private const val KEY_RECENT_IDS = "sonora_recent_song_ids"
        private const val KEY_PLAY_COUNTS = "sonora_play_counts_json"
        private const val KEY_BLACKLISTED_FOLDERS = "sonora_blacklisted_folders"
        private const val KEY_CUSTOM_PLAYLISTS = "sonora_custom_playlists_json"
        private const val KEY_LAST_SONG_ID = "sonora_last_song_id"
        private const val KEY_LAST_POSITION_MS = "sonora_last_position_ms"
        private const val KEY_THEME_MODE = "sonora_theme_mode" // "dark", "light", "system"
        private const val KEY_EQ_PRESET = "sonora_eq_preset"
        private const val KEY_BASS_BOOST = "sonora_bass_boost_level"
        private const val KEY_HAS_SEEN_WELCOME = "sonora_has_seen_welcome"
        private const val KEY_PETAL_ROUNDNESS = "sonora_petal_roundness"
        private const val KEY_CROSSFADE_SECONDS = "sonora_crossfade_seconds"
        private const val KEY_PLAYBACK_SPEED = "sonora_playback_speed"
        private const val KEY_NAV_TABS = "sonora_nav_tabs_json"
        private const val KEY_NAV_LABEL_MODE = "sonora_nav_label_mode" // "active_only", "always", "never"
        private const val KEY_PLAYER_CONTROLS_STYLE = "sonora_player_controls_style" // "dock", "circles", "organic", "squircle", "waveform"
    }

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

    // --- PLAY COUNTS (Top 25) ---
    fun recordPlay(songId: Long) {
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
        return prefs.getStringSet(KEY_BLACKLISTED_FOLDERS, emptySet()) ?: emptySet()
    }

    fun toggleBlacklistFolder(folderName: String): Boolean {
        val current = getBlacklistedFolders().toMutableSet()
        val isBlocked = if (current.contains(folderName)) {
            current.remove(folderName)
            false
        } else {
            current.add(folderName)
            true
        }
        prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, current).apply()
        return isBlocked
    }

    fun blacklistFolder(folderName: String) {
        val current = getBlacklistedFolders().toMutableSet()
        current.add(folderName)
        prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, current).apply()
    }

    fun unblacklistFolder(folderName: String) {
        val current = getBlacklistedFolders().toMutableSet()
        current.remove(folderName)
        prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, current).apply()
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
    fun saveLastPlayback(songId: Long, positionMs: Long) {
        prefs.edit()
            .putLong(KEY_LAST_SONG_ID, songId)
            .putLong(KEY_LAST_POSITION_MS, positionMs)
            .apply()
    }

    fun getLastSongId(): Long = prefs.getLong(KEY_LAST_SONG_ID, -1L)
    fun getLastPositionMs(): Long = prefs.getLong(KEY_LAST_POSITION_MS, 0L)

    // --- SETTINGS (Theme, Equalizer, Welcome) ---
    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
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
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        
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

        return root.toString(2)
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            
            // Liked
            if (root.has("likedSongIds")) {
                val arr = root.getJSONArray("likedSongIds")
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getLong(i).toString())
                }
                prefs.edit().putStringSet(KEY_LIKED_IDS, set).apply()
            }

            // Play counts
            if (root.has("playCounts")) {
                prefs.edit().putString(KEY_PLAY_COUNTS, root.getJSONObject("playCounts").toString()).apply()
            }

            // Blacklist
            if (root.has("blacklistedFolders")) {
                val arr = root.getJSONArray("blacklistedFolders")
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                prefs.edit().putStringSet(KEY_BLACKLISTED_FOLDERS, set).apply()
            }

            // Custom Playlists
            if (root.has("customPlaylists")) {
                val arr = root.getJSONArray("customPlaylists")
                val list = mutableListOf<Playlist>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val songIdsArr = obj.getJSONArray("songIds")
                    val songIds = mutableListOf<Long>()
                    for (j in 0 until songIdsArr.length()) {
                        songIds.add(songIdsArr.getLong(j))
                    }
                    list.add(Playlist(id, name, songIds))
                }
                saveCustomPlaylists(list)
            }

            true
        } catch (_: Exception) {
            false
        }
    }
}
