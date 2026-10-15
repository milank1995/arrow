package com.arrowescape.puzzle.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioHapticManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isSoundEnabled: Boolean = true
    var isVibrationEnabled: Boolean = true

    private val audioScope = CoroutineScope(Dispatchers.Default)

    /**
     * Synthesizes and plays a short sine-wave tone with smooth exponential decay.
     */
    private fun playTone(startFreq: Float, endFreq: Float, durationMs: Int, volume: Float = 0.6f) {
        if (!isSoundEnabled) return

        audioScope.launch {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    val currentFreq = startFreq + (endFreq - startFreq) * t
                    val envelope = (1.0 - t) * (1.0 - t) // Quadratic decay
                    val angle = 2.0 * Math.PI * currentFreq * (i.toFloat() / sampleRate)
                    val sample = (sin(angle) * envelope * volume * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                // Auto-release track after playback
                track.setNotificationMarkerPosition(numSamples)
                track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(t: AudioTrack?) {
                        t?.release()
                    }
                    override fun onPeriodicNotification(t: AudioTrack?) {}
                })
            } catch (_: Exception) {
                // Ignore audio synthesis exceptions safely
            }
        }
    }

    fun playTap() {
        playTone(startFreq = 480f, endFreq = 620f, durationMs = 45, volume = 0.35f)
        if (isVibrationEnabled) {
            vibrate(VibrationEffect.createOneShot(12, 60))
        }
    }

    fun playArrowFly(comboCount: Int = 0) {
        // Harmonious pentatonic pitch steps for ascending combo excitement:
        // C4, D4, E4, G4, A4, C5, D5, E5
        val scaleSteps = floatArrayOf(1.0f, 1.122f, 1.260f, 1.498f, 1.682f, 2.0f, 2.245f, 2.52f)
        val step = (comboCount.coerceAtLeast(1) - 1) % scaleSteps.size
        val pitchFactor = scaleSteps[step]
        val startFreq = 340f * pitchFactor
        val endFreq = (880f * pitchFactor).coerceAtMost(3600f)

        playTone(startFreq = startFreq, endFreq = endFreq, durationMs = 120, volume = 0.5f)
        if (isVibrationEnabled) {
            val hapticIntensity = (22 + (comboCount.coerceAtMost(8) * 4)).coerceIn(20, 55)
            vibrate(VibrationEffect.createOneShot(hapticIntensity.toLong(), 120))
        }
    }

    fun playWrongMove() {
        playTone(startFreq = 220f, endFreq = 140f, durationMs = 180, volume = 0.7f)
        if (isVibrationEnabled) {
            val timings = longArrayOf(0, 40, 50, 60)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    fun playBoosterUse() {
        playTone(startFreq = 520f, endFreq = 1200f, durationMs = 200, volume = 0.6f)
        if (isVibrationEnabled) {
            vibrate(VibrationEffect.createOneShot(35, 160))
        }
    }

    fun playLevelWin() {
        audioScope.launch {
            playTone(523.25f, 523.25f, 100, 0.5f) // C5
            kotlinx.coroutines.delay(100)
            playTone(659.25f, 659.25f, 100, 0.6f) // E5
            kotlinx.coroutines.delay(100)
            playTone(783.99f, 783.99f, 120, 0.7f) // G5
            kotlinx.coroutines.delay(120)
            playTone(1046.50f, 1046.50f, 300, 0.8f) // C6
        }
        if (isVibrationEnabled) {
            val timings = longArrayOf(0, 50, 80, 50, 80, 100)
            val amplitudes = intArrayOf(0, 120, 0, 180, 0, 255)
            vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(effect)
        }
    }
}
