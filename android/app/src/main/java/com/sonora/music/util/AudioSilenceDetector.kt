package com.sonora.music.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.sqrt

object AudioSilenceDetector {

    /**
     * Fast PCM scan of the first [maxScanMs] ms to detect initial silence or dead space.
     * Returns the timestamp in ms where audible music begins (up to 3000ms max).
     */
    fun detectInitialSilenceMs(filePath: String, maxScanMs: Long = 3500L, thresholdRms: Double = 320.0): Long {
        if (filePath.isEmpty()) return 0L
        val file = File(filePath)
        if (!file.exists() || !file.canRead() || file.length() < 1024) return 0L

        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null

        try {
            extractor = MediaExtractor().apply { setDataSource(filePath) }
            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) return 0L

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return 0L
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var firstAudibleMs = 0L
            var foundAudible = false
            val maxScanUs = maxScanMs * 1000L

            var iterations = 0
            while (!foundAudible && iterations < 80) {
                iterations++
                if (!sawInputEOS) {
                    val inputIndex = codec.dequeueInputBuffer(4000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sawInputEOS = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                                extractor.advance()
                                if (sampleTime > maxScanUs) {
                                    sawInputEOS = true
                                }
                            }
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 4000L)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val presentationMs = bufferInfo.presentationTimeUs / 1000L
                        val rms = calculatePcmRms(outputBuffer, bufferInfo.offset, bufferInfo.size)
                        if (rms >= thresholdRms) {
                            firstAudibleMs = presentationMs
                            foundAudible = true
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }

            // If music started after some silence, return silence duration (capped at 3.0s)
            return if (foundAudible) firstAudibleMs.coerceIn(0L, 3000L) else 0L
        } catch (_: Exception) {
            return 0L
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    /**
     * Checks if the last ~4 seconds of the audio file contain a pre-existing natural fade out.
     */
    fun hasNaturalFadeOut(filePath: String, durationMs: Long): Boolean {
        if (filePath.isEmpty() || durationMs < 10000L) return false
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return false

        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null

        try {
            extractor = MediaExtractor().apply { setDataSource(filePath) }
            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) return false

            extractor.selectTrack(audioTrackIndex)
            val seekTargetUs = (durationMs - 4000L).coerceAtLeast(0L) * 1000L
            extractor.seekTo(seekTargetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return false
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            val rmsSamples = mutableListOf<Double>()
            var iterations = 0

            while (iterations < 60) {
                iterations++
                if (!sawInputEOS) {
                    val inputIndex = codec.dequeueInputBuffer(4000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sawInputEOS = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 4000L)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val rms = calculatePcmRms(outputBuffer, bufferInfo.offset, bufferInfo.size)
                        rmsSamples.add(rms)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }

            if (rmsSamples.size >= 4) {
                val firstHalf = rmsSamples.take(rmsSamples.size / 2).average()
                val secondHalf = rmsSamples.takeLast(rmsSamples.size / 2).average()
                return firstHalf > 600.0 && secondHalf < (firstHalf * 0.45)
            }
            return false
        } catch (_: Exception) {
            return false
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    private fun calculatePcmRms(buffer: ByteBuffer, offset: Int, size: Int): Double {
        val bytes = ByteArray(size)
        buffer.position(offset)
        buffer.get(bytes, 0, size)

        var sumSquare = 0.0
        val sampleCount = size / 2
        if (sampleCount <= 0) return 0.0

        for (i in 0 until sampleCount) {
            val idx = i * 2
            val sample = (bytes[idx].toInt() and 0xFF) or (bytes[idx + 1].toInt() shl 8)
            val shortSample = sample.toShort().toDouble()
            sumSquare += shortSample * shortSample
        }
        return sqrt(sumSquare / sampleCount)
    }
}
