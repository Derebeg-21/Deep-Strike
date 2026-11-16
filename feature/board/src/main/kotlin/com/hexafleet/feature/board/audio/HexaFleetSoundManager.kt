package com.hexafleet.feature.board.audio

import android.media.AudioManager
import android.media.ToneGenerator
import com.hexafleet.feature.board.settings.HexaFleetSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Düşük gecikmeli taktiksel ses motoru.
 * HexaFleetSettings üzerinden ses seviyesi ve sessiz mod kontrollerine tam duyarlıdır.
 */
class HexaFleetSoundManager {

    private var toneGenerator: ToneGenerator? = null

    init {
        recreateToneGenerator()
    }

    private fun recreateToneGenerator() {
        try {
            toneGenerator?.release()
            val volume = (HexaFleetSettings.soundVolume * 100).toInt().coerceIn(0, 100)
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    private val audioScope = CoroutineScope(Dispatchers.Default)

    /**
     * Radar Sonar Yankısı: Yüksek frekanslı keskin bip
     */
    fun playRadarPing() {
        if (!HexaFleetSettings.isSoundEnabled || !HexaFleetSettings.isRadarPingEnabled) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
    }

    /**
     * Topçu Ateşi
     */
    fun playCannonFire() {
        if (!HexaFleetSettings.isSoundEnabled) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 60)
    }

    /**
     * Düşman Gemisine İsabet: Tok patlama alarmı
     */
    fun playHitExplosion() {
        if (!HexaFleetSettings.isSoundEnabled) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 150)
    }

    /**
     * Suya Düşen Karavana Atışı: Düşük frekanslı kısa tık
     */
    fun playWaterSplash() {
        if (!HexaFleetSettings.isSoundEnabled) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 40)
    }

    /**
     * Aegis Kalkanı Sönümlemesi: Titreşen teyit sinyali
     */
    fun playShieldAbsorb() {
        if (!HexaFleetSettings.isSoundEnabled) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 120)
    }

    /**
     * EMP Şok Dalgası: Arka arkaya inen elektronik kilit tonları
     */
    fun playEmpPulse() {
        if (!HexaFleetSettings.isSoundEnabled) return
        ensureToneGenerator()
        audioScope.launch {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 60)
            delay(50)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 80)
        }
    }

    private fun ensureToneGenerator() {
        if (toneGenerator == null) {
            recreateToneGenerator()
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
