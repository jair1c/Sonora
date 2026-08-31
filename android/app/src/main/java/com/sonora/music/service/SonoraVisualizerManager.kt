package com.sonora.music.service

import android.media.audiofx.Visualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

class SonoraVisualizerManager {

    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0
    private var isUiActive: Boolean = true
    private var isPlaying: Boolean = false
    private var lastHardwareFftTime: Long = 0L

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var fallbackJob: Job? = null

    private val _fftData = MutableStateFlow(FloatArray(16) { 0f })
    val fftData = _fftData.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0) return
        currentSessionId = audioSessionId
        setupVisualizer()
        if (isPlaying) {
            startFallbackLoopIfNeeded()
        }
    }

    fun setUiActive(active: Boolean) {
        isUiActive = active
        if (active) {
            if (visualizer == null && currentSessionId != 0) {
                setupVisualizer()
            }
            if (isPlaying) {
                startFallbackLoopIfNeeded()
            }
        }
    }

    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        if (playing) {
            startFallbackLoopIfNeeded()
        } else {
            stopFallbackLoop()
            _fftData.value = FloatArray(16) { 0f }
        }
    }

    private fun setupVisualizer() {
        releaseVisualizer()
        try {
            val v = try {
                Visualizer(currentSessionId)
            } catch (_: Throwable) {
                try { Visualizer(0) } catch (_: Throwable) { null }
            } ?: return

            v.captureSize = Visualizer.getCaptureSizeRange()[0]
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
                        if (fft == null || !isPlaying) return
                        try {
                            val bandCount = 16
                            val magnitudes = FloatArray(bandCount)
                            val n = fft.size / 2
                            var totalEnergy = 0f

                            for (i in 0 until bandCount) {
                                val idx = (i * (n / bandCount)).coerceIn(0, n - 1)
                                val real = fft[idx * 2].toFloat()
                                val imag = fft[idx * 2 + 1].toFloat()
                                val magnitude = hypot(real, imag) / 128f
                                val clamped = magnitude.coerceIn(0f, 1f)
                                magnitudes[i] = clamped
                                totalEnergy += clamped
                            }

                            if (totalEnergy > 0.005f) {
                                lastHardwareFftTime = System.currentTimeMillis()
                                // Apply dynamic logarithmic amplification so quiet passages stay responsive
                                val enhanced = FloatArray(bandCount)
                                for (k in 0 until bandCount) {
                                    enhanced[k] = kotlin.math.sqrt(magnitudes[k] * 1.5f).coerceIn(0.08f, 1f)
                                }
                                _fftData.value = enhanced
                            }
                        } catch (_: Throwable) {}
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            v.enabled = true
            visualizer = v
        } catch (_: Throwable) {
            visualizer = null
        }
    }

    private fun startFallbackLoopIfNeeded() {
        if (!isPlaying) return
        if (fallbackJob?.isActive == true) return

        fallbackJob = scope.launch {
            var phase = 0.0
            val bandCount = 16
            while (isActive && isPlaying) {
                val now = System.currentTimeMillis()
                // If no hardware FFT was received recently, generate smooth organic reactive harmonic waveform
                if (now - lastHardwareFftTime > 150L) {
                    phase += 0.22
                    val magnitudes = FloatArray(bandCount)
                    for (i in 0 until bandCount) {
                        val wave1 = sin(phase + i * 0.45)
                        val wave2 = sin(phase * 1.6 + i * 0.9)
                        val wave3 = sin(phase * 0.7 - i * 0.3)
                        val combined = abs(wave1 * 0.5 + wave2 * 0.3 + wave3 * 0.2).toFloat()
                        val freqShape = (1.0f - (abs(i - 4) / 12f)).coerceIn(0.35f, 1.0f)
                        magnitudes[i] = (combined * freqShape).coerceIn(0.08f, 0.95f)
                    }
                    _fftData.value = magnitudes
                }
                delay(35) // ~30 FPS smooth fluid animation
            }
        }
    }

    private fun stopFallbackLoop() {
        fallbackJob?.cancel()
        fallbackJob = null
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Throwable) {}
        visualizer = null
    }

    fun release() {
        stopFallbackLoop()
        releaseVisualizer()
        _fftData.value = FloatArray(16) { 0f }
    }
}
