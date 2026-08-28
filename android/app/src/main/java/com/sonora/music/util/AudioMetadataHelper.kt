package com.sonora.music.util

import android.content.Context
import android.media.MediaMetadataRetriever
import com.sonora.music.data.model.Song
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

data class AudioFormatDetails(
    val formatName: String,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val bitrateKbps: Int,
    val channels: Int,
    val isHiRes: Boolean,
    val formattedString: String
)

object AudioMetadataHelper {

    private val cache = ConcurrentHashMap<Long, AudioFormatDetails>()

    fun getAudioDetails(song: Song, context: Context? = null): AudioFormatDetails {
        cache[song.id]?.let { return it }

        var formatName = "Audio"
        var sampleRate = 44100
        var bitDepth = 16
        var bitrate = 0
        var channels = 2

        val path = song.filePath
        val pathLower = path.lowercase()

        // 1. Instant binary header check for FLAC (0.01ms, non-locking)
        var parsedFromHeader = false
        if (pathLower.endsWith(".flac") || song.title.lowercase().endsWith(".flac")) {
            formatName = "FLAC"
            val flacParsed = parseFlacHeaderDirectly(song, context)
            if (flacParsed != null) {
                sampleRate = flacParsed.first
                channels = flacParsed.second
                bitDepth = flacParsed.third
                parsedFromHeader = true
            }
        } else if (pathLower.endsWith(".wav") || song.title.lowercase().endsWith(".wav")) {
            formatName = "WAV"
            val wavParsed = parseWavHeaderDirectly(song, context)
            if (wavParsed != null) {
                sampleRate = wavParsed.first
                channels = wavParsed.second
                bitDepth = wavParsed.third
                parsedFromHeader = true
            }
        }

        // 2. Non-blocking MediaMetadataRetriever fallback if not fully parsed
        if (!parsedFromHeader) {
            val mmr = MediaMetadataRetriever()
            try {
                if (path.isNotEmpty() && File(path).exists()) {
                    mmr.setDataSource(path)
                } else if (context != null) {
                    mmr.setDataSource(context, song.contentUri)
                }

                val srStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                val brStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                val mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
                val bitsStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)

                if (srStr != null) srStr.toIntOrNull()?.let { sampleRate = it }
                if (brStr != null) brStr.toIntOrNull()?.let { bitrate = it / 1000 }
                if (bitsStr != null) bitsStr.toIntOrNull()?.let { bitDepth = it }

                formatName = when {
                    mime.contains("flac") -> "FLAC"
                    mime.contains("wav") || mime.contains("raw") -> "WAV"
                    mime.contains("mp4a") || mime.contains("aac") -> "AAC"
                    mime.contains("opus") -> "OPUS"
                    mime.contains("vorbis") || mime.contains("ogg") -> "OGG"
                    mime.contains("mp3") || mime.contains("mpeg") -> "MP3"
                    else -> formatName
                }
            } catch (_: Exception) {
            } finally {
                try { mmr.release() } catch (_: Exception) {}
            }
        }

        // 3. Fallback format by extension
        if (formatName == "Audio" || formatName.isBlank()) {
            formatName = when {
                pathLower.endsWith(".flac") -> "FLAC"
                pathLower.endsWith(".wav") -> "WAV"
                pathLower.endsWith(".m4a") || pathLower.endsWith(".aac") -> "AAC"
                pathLower.endsWith(".opus") -> "OPUS"
                pathLower.endsWith(".ogg") -> "OGG"
                pathLower.endsWith(".mp3") -> "MP3"
                else -> "Audio"
            }
        }

        // Calculate bitrate if missing from file size and duration
        if (bitrate <= 0 && song.durationMs > 0 && song.sizeBytes > 0) {
            bitrate = ((song.sizeBytes * 8) / (song.durationMs)).toInt()
        }

        if (formatName == "FLAC" || formatName == "WAV") {
            if (bitDepth <= 16 && (sampleRate > 48000 || bitrate > 1500)) {
                bitDepth = 24
            }
        }

        val isHiRes = (bitDepth >= 24) || (sampleRate >= 88200) || (formatName == "FLAC" && bitrate > 1000)

        val sampleRateKhz = if (sampleRate > 0) String.format(java.util.Locale.US, "%.1f kHz", sampleRate / 1000f) else "44.1 kHz"
        val bitDepthStr = if (bitDepth > 0 && (formatName == "FLAC" || formatName == "WAV")) "$bitDepth-bit / " else ""
        val bitrateStr = if (bitrate > 0) " • ${bitrate} kbps" else ""

        val formatted = "$formatName • $bitDepthStr$sampleRateKhz$bitrateStr"

        val details = AudioFormatDetails(
            formatName = formatName,
            sampleRateHz = sampleRate,
            bitDepth = bitDepth,
            bitrateKbps = bitrate,
            channels = channels,
            isHiRes = isHiRes,
            formattedString = formatted
        )

        cache[song.id] = details
        return details
    }

    private fun parseFlacHeaderDirectly(song: Song, context: Context?): Triple<Int, Int, Int>? {
        var inputStream: InputStream? = null
        try {
            inputStream = if (song.filePath.isNotEmpty() && File(song.filePath).exists()) {
                FileInputStream(song.filePath)
            } else if (context != null) {
                context.contentResolver.openInputStream(song.contentUri)
            } else null

            if (inputStream == null) return null

            val buffer = ByteArray(26)
            var totalRead = 0
            while (totalRead < 26) {
                val r = inputStream.read(buffer, totalRead, 26 - totalRead)
                if (r <= 0) break
                totalRead += r
            }
            if (totalRead < 26) return null

            // Check "fLaC" magic bytes (0x66, 0x4C, 0x61, 0x43)
            if (buffer[0] != 0x66.toByte() || buffer[1] != 0x4C.toByte() || buffer[2] != 0x61.toByte() || buffer[3] != 0x43.toByte()) {
                return null
            }

            // STREAMINFO block check (block_type == 0)
            val blockType = buffer[4].toInt() and 0x7F
            if (blockType != 0) return null

            val b18 = buffer[18].toInt() and 0xFF
            val b19 = buffer[19].toInt() and 0xFF
            val b20 = buffer[20].toInt() and 0xFF
            val b21 = buffer[21].toInt() and 0xFF

            val sampleRate = (b18 shl 12) or (b19 shl 4) or (b20 ushr 4)
            val channels = ((b20 ushr 1) and 0x07) + 1
            val bitDepth = (((b20 and 0x01) shl 4) or (b21 ushr 4)) + 1

            if (sampleRate > 0 && bitDepth > 0) {
                return Triple(sampleRate, channels, bitDepth)
            }
        } catch (_: Exception) {
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
        return null
    }

    private fun parseWavHeaderDirectly(song: Song, context: Context?): Triple<Int, Int, Int>? {
        var inputStream: InputStream? = null
        try {
            inputStream = if (song.filePath.isNotEmpty() && File(song.filePath).exists()) {
                FileInputStream(song.filePath)
            } else if (context != null) {
                context.contentResolver.openInputStream(song.contentUri)
            } else null

            if (inputStream == null) return null

            val buffer = ByteArray(36)
            var totalRead = 0
            while (totalRead < 36) {
                val r = inputStream.read(buffer, totalRead, 36 - totalRead)
                if (r <= 0) break
                totalRead += r
            }
            if (totalRead < 36) return null

            // "RIFF" and "WAVE"
            if (buffer[0] == 'R'.code.toByte() && buffer[1] == 'I'.code.toByte() && buffer[2] == 'F'.code.toByte() && buffer[3] == 'F'.code.toByte() &&
                buffer[8] == 'W'.code.toByte() && buffer[9] == 'A'.code.toByte() && buffer[10] == 'V'.code.toByte() && buffer[11] == 'E'.code.toByte()) {

                val channels = (buffer[22].toInt() and 0xFF) or ((buffer[23].toInt() and 0xFF) shl 8)
                val sampleRate = (buffer[24].toInt() and 0xFF) or
                        ((buffer[25].toInt() and 0xFF) shl 8) or
                        ((buffer[26].toInt() and 0xFF) shl 16) or
                        ((buffer[27].toInt() and 0xFF) shl 24)
                val bitDepth = (buffer[34].toInt() and 0xFF) or ((buffer[35].toInt() and 0xFF) shl 8)

                if (sampleRate > 0 && bitDepth > 0) {
                    return Triple(sampleRate, channels, bitDepth)
                }
            }
        } catch (_: Exception) {
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
        return null
    }
}
