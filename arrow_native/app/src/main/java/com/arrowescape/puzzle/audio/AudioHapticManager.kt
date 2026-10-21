package com.arrowescape.puzzle.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.arrowescape.puzzle.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Authentic Sound & Haptics Manager for Arrow Escape.
 * Manages all gameplay audio clips extracted directly from the Unity reference,
 * providing microsecond low-latency playback via SoundPool with vibration synthesis.
 */
class AudioHapticManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isSoundEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                stopAll()
            }
        }

    var isVibrationEnabled: Boolean = true

    private val audioScope = CoroutineScope(Dispatchers.Default)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(14)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // Authentic sound IDs
    private var sfxTapId: Int = 0
    private var sfxClickId: Int = 0
    private var sfxSlideId: Int = 0
    private var sfxWrongId: Int = 0
    private var sfxCoinId: Int = 0
    private var sfxStarId: Int = 0
    private var sfxWinId: Int = 0
    private var sfxHintId: Int = 0
    private var sfxFailId: Int = 0
    private var sfxHammerId: Int = 0
    private var sfxBoosterId: Int = 0
    private var sfxDotId: Int = 0

    // Combo push sounds
    private val pushSoundIds = mutableListOf<Int>()
    private val loadedSounds = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
            }
        }

        try {
            sfxTapId = loadSound(R.raw.sfx_tap)
            sfxClickId = loadSound(R.raw.sfx_click)
            sfxSlideId = loadSound(R.raw.sfx_slide)
            sfxWrongId = loadSound(R.raw.sfx_wrong)
            sfxCoinId = loadSound(R.raw.sfx_coin)
            sfxStarId = loadSound(R.raw.sfx_star)
            sfxWinId = loadSound(R.raw.sfx_win)
            sfxHintId = loadSound(R.raw.sfx_hint)
            sfxFailId = loadSound(R.raw.sfx_fail)
            sfxHammerId = loadSound(R.raw.sfx_hammer)
            sfxBoosterId = loadSound(R.raw.sfx_booster)
            sfxDotId = loadSound(R.raw.sfx_dot)

            val pushRaws = intArrayOf(
                R.raw.push1_1, R.raw.push1_2, R.raw.push1_3, R.raw.push1_4,
                R.raw.push2_1, R.raw.push2_2, R.raw.push2_3,
                R.raw.push3_1, R.raw.push3_2
            )
            for (raw in pushRaws) {
                pushSoundIds.add(loadSound(raw))
            }
        } catch (_: Exception) {
            // Audio loading fallback handled safely
        }
    }

    private fun loadSound(resId: Int): Int {
        return try {
            soundPool.load(context, resId, 1)
        } catch (_: Exception) {
            0
        }
    }

    private fun playSfx(soundId: Int, volume: Float = 0.9f, rate: Float = 1.0f): Int {
        if (!isSoundEnabled || soundId == 0) return 0
        return try {
            soundPool.play(soundId, volume, volume, 1, 0, rate)
        } catch (_: Exception) {
            0
        }
    }

    fun stopAll() {
        try {
            soundPool.autoPause()
        } catch (_: Exception) {}
    }

    /**
     * UI button click sound with crisp, lightweight haptic.
     */
    fun playClick() {
        playSfx(sfxClickId, volume = 0.75f)
        if (isVibrationEnabled) {
            vibrate(VibrationEffect.createOneShot(10, 50))
        }
    }

    /**
     * Arrow tap sound when an arrow is selected.
     */
    fun playTap() {
        playSfx(sfxTapId, volume = 0.85f)
        if (isVibrationEnabled) {
            vibrate(VibrationEffect.createOneShot(12, 60))
        }
    }

    /**
     * Arrow escape / slide whoosh with combo escalation.
     */
    fun playArrowFly(comboCount: Int = 0) {
        if (isSoundEnabled) {
            if (pushSoundIds.isNotEmpty()) {
                val idx = (comboCount.coerceAtLeast(1) - 1) % pushSoundIds.size
                playSfx(pushSoundIds[idx], volume = 0.90f)
            } else {
                playSfx(sfxSlideId, volume = 0.85f)
            }
        }
        if (isVibrationEnabled) {
            val hapticIntensity = (22 + (comboCount.coerceAtMost(8) * 4)).coerceIn(20, 55)
            vibrate(VibrationEffect.createOneShot(hapticIntensity.toLong(), 120))
        }
    }

    /**
     * Blocked arrow move / collision sound with double error vibration.
     */
    fun playWrongMove() {
        playSfx(sfxWrongId, volume = 0.85f)
        if (isVibrationEnabled) {
            val timings = longArrayOf(0, 40, 50, 60)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    /**
     * Star drop cheer sound for victory screen with pitch step per star.
     */
    fun playStarDrop(starIndex: Int = 1) {
        val pitch = when (starIndex) {
            1 -> 1.0f
            2 -> 1.12f
            else -> 1.25f
        }
        playSfx(sfxStarId, volume = 0.95f, rate = pitch)
        if (isVibrationEnabled) {
            vibrate(VibrationEffect.createOneShot(20, 90))
        }
    }

    /**
     * Coin collection / reward counter sound.
     */
    fun playCoinCollect() {
        playSfx(sfxCoinId, volume = 0.85f)
        if (isVibrationEnabled) {
            vibrate(VibrationEffect.createOneShot(14, 60))
        }
    }

    /**
     * Booster activation sound (Hammer, Magic Wand, Hint).
     */
    fun playBoosterUse(boosterType: String = "wand") {
        when (boosterType) {
            "hammer" -> playSfx(sfxHammerId, volume = 0.95f)
            "hint" -> playSfx(sfxHintId, volume = 0.85f)
            else -> playSfx(sfxBoosterId, volume = 0.90f)
        }
        if (isVibrationEnabled) {
            vibrate(VibrationEffect.createOneShot(35, 160))
        }
    }

    /**
     * Level Complete victory fanfare.
     */
    fun playLevelWin() {
        playSfx(sfxWinId, volume = 1.0f)
        if (isVibrationEnabled) {
            val timings = longArrayOf(0, 50, 80, 50, 80, 120)
            val amplitudes = intArrayOf(0, 140, 0, 190, 0, 255)
            vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    /**
     * Out of lives / Level Fail sound.
     */
    fun playLevelFail() {
        playSfx(sfxFailId, volume = 0.95f)
        if (isVibrationEnabled) {
            val timings = longArrayOf(0, 80, 100, 140)
            val amplitudes = intArrayOf(0, 180, 0, 240)
            vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }

    /**
     * Idle hint nudge sound.
     */
    fun playHintNudge() {
        playSfx(sfxHintId, volume = 0.70f)
        if (isVibrationEnabled) {
            vibrate(VibrationEffect.createOneShot(15, 50))
        }
    }

    /**
     * Dot animation appearance sound.
     */
    fun playDotAnim() {
        playSfx(sfxDotId, volume = 0.65f)
    }

    private fun vibrate(effect: VibrationEffect) {
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(effect)
        }
    }
}
