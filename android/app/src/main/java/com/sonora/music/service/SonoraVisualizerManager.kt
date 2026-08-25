package com.sonora.music.service

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

class SonoraVisualizerManager {

    private var visualizer: Visualizer? = null
    private val _fftData = MutableStateFlow(FloatArray(16) { 0f })
    val fftData = _fftData.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0] // Smallest capture size for high responsiveness
                setDataCaptureListener(
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
                            if (fft == null) return
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
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
        _fftData.value = FloatArray(16) { 0f }
    }
}
