package com.sonora.music.service

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import kotlin.math.pow

class SonoraEqualizerManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var preAmpDb: Float = 0f

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = true
                }
            } catch (_: Exception) {}
            applyPreAmp(preAmpDb)
        } catch (_: Exception) {}
    }

    fun getBandCount(): Int {
        return equalizer?.numberOfBands?.toInt() ?: 5
    }

    fun getBandFrequency(band: Short): Int {
        return equalizer?.getCenterFreq(band)?.div(1000) ?: 1000
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        val range = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        return Pair(range[0], range[1])
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (_: Exception) {}
    }

    fun getBandLevel(band: Short): Short {
        return equalizer?.getBandLevel(band) ?: 0
    }

    fun getPresets(): List<String> {
        val count = equalizer?.numberOfPresets?.toInt() ?: 0
        val list = mutableListOf<String>()
        for (i in 0 until count) {
            val name = equalizer?.getPresetName(i.toShort()) ?: "Preset $i"
            list.add(name)
        }
        if (list.isEmpty()) {
            return listOf("Flat", "Rock", "Pop", "Jazz", "Bass Boost", "Vocal", "Clásica")
        }
        return list
    }

    fun usePreset(presetIdx: Short) {
        try {
            equalizer?.usePreset(presetIdx)
        } catch (_: Exception) {}
    }

    fun setBassBoost(strength: Short) {
        try {
            bassBoost?.setStrength(strength)
        } catch (_: Exception) {}
    }

    fun setPreAmp(gainDb: Float) {
        preAmpDb = gainDb.coerceIn(-12f, 12f)
        applyPreAmp(preAmpDb)
    }

    fun getPreAmp(): Float = preAmpDb

    private fun applyPreAmp(gainDb: Float) {
        try {
            if (gainDb > 0f) {
                val targetMilliBels = (gainDb * 100).toInt()
                loudnessEnhancer?.setTargetGain(targetMilliBels)
                loudnessEnhancer?.enabled = true
            } else {
                loudnessEnhancer?.setTargetGain(0)
                loudnessEnhancer?.enabled = false
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
    }
}
