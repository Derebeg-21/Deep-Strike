package com.hexafleet.feature.board.haptics

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

import com.hexafleet.feature.board.settings.HexaFleetSettings

/**
 * Android Donanım Titreşim (Haptic Feedback) Motoru.
 * Sonar sinyalleri, füze isabetleri ve kalkan patlamaları için özelleştirilmiş dokunsal profiller sunar.
 */
class HexaFleetHapticFeedback(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Sonar Tarama Tıkırtısı: Hafif ve mikroskobik dokunma hissi
     */
    fun playSonarTick() {
        vibrate(durationMs = 12, amplitude = 40)
    }

    /**
     * Topçu Ateşi / Buton Onayı: Net ve tok mekanik klik
     */
    fun playButtonClick() {
        vibrate(durationMs = 25, amplitude = 80)
    }

    /**
     * Düşman Gemisine İsabet: Keskin ve güçlü patlama darbesi
     */
    fun playHitImpact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK) == true) {
            val composition = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f)
            vibrator?.vibrate(composition.compose())
        } else {
            vibrate(durationMs = 70, amplitude = 220)
        }
    }

    /**
     * Kalkan Sönümlemesi: Titreşen dalga efekti
     */
    fun playShieldHum() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 30, 20, 30)
            val amplitudes = intArrayOf(0, 100, 40, 120)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrate(durationMs = 50, amplitude = 120)
        }
    }

    /**
     * Gemi Batırma: 3 kademeli dramatik yıkım titreşimi
     */
    fun playShipSunkDrama() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 40, 30, 80, 40, 120)
            val amplitudes = intArrayOf(0, 200, 0, 255, 0, 160)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrate(durationMs = 200, amplitude = 255)
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        if (!HexaFleetSettings.isHapticsEnabled) return
        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val clampedAmplitude = amplitude.coerceIn(1, 255)
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(durationMs)
        }
    }
}

