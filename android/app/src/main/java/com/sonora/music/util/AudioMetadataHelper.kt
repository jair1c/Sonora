package com.sonora.music.util

import android.media.MediaExtractor
import android.media.MediaFormat
import com.sonora.music.data.model.Song
import java.io.File
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

    fun getAudioDetails(song: Song): AudioFormatDetails {
        cache[song.id]?.let { return it }

        var formatName = "Audio"
        var sampleRate = 44100
        var bitDepth = 16
        var bitrate = 0
        var channels = 2

        val path = song.filePath
        val pathLower = path.lowercase()

        // 1. Try MediaExtractor if local file exists
        if (path.isNotEmpty() && File(path).exists()) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(path)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                        }
                        if (format.containsKey("bits-per-sample")) {
                            bitDepth = format.getInteger("bits-per-sample")
                        }
                        formatName = when {
                            mime.contains("flac") -> "FLAC"
                            mime.contains("wav") || mime.contains("raw") -> "WAV"
                            mime.contains("mp4a") || mime.contains("aac") -> "AAC"
                            mime.contains("opus") -> "OPUS"
                            mime.contains("vorbis") || mime.contains("ogg") -> "OGG"
                            mime.contains("mp3") || mime.contains("mpeg") -> "MP3"
                            else -> mime.substringAfter("audio/").uppercase()
                        }
                        break
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { extractor.release() } catch (_: Exception) {}
            }
        }

        // Calculate bitrate if missing from file size and duration
        if (bitrate <= 0 && song.durationMs > 0 && song.sizeBytes > 0) {
            bitrate = ((song.sizeBytes * 8) / (song.durationMs)).toInt()
        }

        // Refine format by extension if needed
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
}
