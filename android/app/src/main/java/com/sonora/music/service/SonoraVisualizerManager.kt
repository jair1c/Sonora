package com.sonora.music.service

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

class SonoraVisualizerManager {

    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0
    private var isUiActive: Boolean = false

    private val _fftData = MutableStateFlow(FloatArray(16) { 0f })
    val fftData = _fftData.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        currentSessionId = audioSessionId
        if (isUiActive) {
            setupVisualizer()
        }
    }

    fun setUiActive(active: Boolean) {
        isUiActive = active
        if (active) {
            if (visualizer == null && currentSessionId != 0) {
                setupVisualizer()
            } else {
                try {
                    visualizer?.enabled = true
                } catch (_: Throwable) {}
            }
        } else {
            // When screen turns off or UI backgrounded, disable & clear to prevent C++ AudioFlinger crash
            try {
                visualizer?.enabled = false
            } catch (_: Throwable) {}
            _fftData.value = FloatArray(16) { 0f }
        }
    }

    private fun setupVisualizer() {
        release()
        try {
            val v = Visualizer(currentSessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[0] // Smallest capture size for high responsiveness
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {}

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft == null || !isUiActive) return
                        try {
                            val bandCount = 16
                            val magnitudes = FloatArray(bandCount)
                            val n = fft.size / 2

                            for (i in 0 until bandCount) {
                                val idx = (i * (n / bandCount)).coerceIn(0, n - 1)
                                val real = fft[idx * 2].toFloat()
                                val imag = fft[idx * 2 + 1].toFloat()
                                val magnitude = hypot(real, imag) / 128f
                                magnitudes[i] = magnitude.coerceIn(0f, 1f)
                            }
                            _fftData.value = magnitudes
                        } catch (_: Throwable) {}
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            v.enabled = isUiActive
            visualizer = v
        } catch (_: Throwable) {
            visualizer = null
        }
    }

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Throwable) {}
        visualizer = null
        _fftData.value = FloatArray(16) { 0f }
    }
}
