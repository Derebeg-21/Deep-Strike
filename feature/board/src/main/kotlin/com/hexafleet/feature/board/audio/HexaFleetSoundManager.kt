package com.hexafleet.feature.board.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import androidx.annotation.RawRes
import com.hexafleet.feature.board.R
import com.hexafleet.feature.board.settings.HexaFleetSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Düşük gecikmeli taktiksel ses ve müzik motoru.
 * HexaFleetSettings üzerinden ses seviyesi, sessiz mod ve telefonun fiziksel sessiz/titreşim durumuna tam duyarlıdır.
 */
class HexaFleetSoundManager(private val appContext: Context? = null) {

    private var toneGenerator: ToneGenerator? = null
    private var lastAppliedVolume: Int = -1

    init {
        if (appContext != null) {
            Companion.appContextRef = appContext.applicationContext
        }
        recreateToneGenerator()
    }

    private fun recreateToneGenerator() {
        try {
            toneGenerator?.release()
            val volume = (HexaFleetSettings.soundVolume * 100).toInt().coerceIn(0, 100)
            lastAppliedVolume = volume
            toneGenerator = if (volume > 0) {
                ToneGenerator(AudioManager.STREAM_MUSIC, volume)
            } else {
                null
            }
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    private val audioScope = CoroutineScope(Dispatchers.Default)

    private fun canPlaySoundEffects(): Boolean {
        if (!HexaFleetSettings.isSoundEnabled || HexaFleetSettings.soundVolume <= 0.01f) return false
        val ctx = appContext ?: Companion.appContextRef
        if (ctx != null && isDeviceSilentOrMuted(ctx)) return false
        return true
    }

    /**
     * Radar Sonar Yankısı: Yüksek frekanslı keskin bip
     */
    fun playRadarPing() {
        if (!canPlaySoundEffects() || !HexaFleetSettings.isRadarPingEnabled) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
    }

    /**
     * Topçu Ateşi
     */
    fun playCannonFire() {
        if (!canPlaySoundEffects()) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 60)
    }

    /**
     * Düşman Gemisine İsabet: Tok patlama alarmı
     */
    fun playHitExplosion() {
        if (!canPlaySoundEffects()) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 150)
    }

    /**
     * Suya Düşen Karavana Atışı: Düşük frekanslı kısa tık
     */
    fun playWaterSplash() {
        if (!canPlaySoundEffects()) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 40)
    }

    /**
     * Aegis Kalkanı Sönümlemesi: Titreşen teyit sinyali
     */
    fun playShieldAbsorb() {
        if (!canPlaySoundEffects()) return
        ensureToneGenerator()
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 120)
    }

    /**
     * EMP Şok Dalgası: Arka arkaya inen elektronik kilit tonları
     */
    fun playEmpPulse() {
        if (!canPlaySoundEffects()) return
        ensureToneGenerator()
        audioScope.launch {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 60)
            delay(50)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 80)
        }
    }

    private fun ensureToneGenerator() {
        val targetVolume = (HexaFleetSettings.soundVolume * 100).toInt().coerceIn(0, 100)
        if (toneGenerator == null || lastAppliedVolume != targetVolume) {
            recreateToneGenerator()
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    companion object {
        private var appContextRef: Context? = null
        private var activeMediaPlayer: MediaPlayer? = null
        private var hasPlayedStartupMusic: Boolean = false

        /**
         * Telefonun fiziksel olarak sessizde (Silent/Vibrate) veya medya sesinin tamamen kısık olup olmadığını denetler.
         */
        fun isDeviceSilentOrMuted(context: Context): Boolean {
            return try {
                val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    ?: return false
                val ringerMode = audioManager.ringerMode
                val musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                ringerMode == AudioManager.RINGER_MODE_SILENT ||
                    ringerMode == AudioManager.RINGER_MODE_VIBRATE ||
                    musicVolume <= 0
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Uygulama ilk açıldığında 1 kez açılış müziğini çalar.
         */
        fun playStartupMusicOnce(context: Context) {
            appContextRef = context.applicationContext
            if (hasPlayedStartupMusic) return
            hasPlayedStartupMusic = true
            playOneShotTrack(context, R.raw.sfx_startup)
        }

        /**
         * Oyuncu maçı kazandığında (Tüm oyun modlarında) zafer müziğini 1 kez çalar.
         */
        fun playVictoryMusic(context: Context) {
            appContextRef = context.applicationContext
            playOneShotTrack(context, R.raw.sfx_victory)
        }

        /**
         * Oyuncu maçı kaybettiğinde (Tüm oyun modlarında) bozgun müziğini 1 kez çalar.
         */
        fun playDefeatMusic(context: Context) {
            appContextRef = context.applicationContext
            playOneShotTrack(context, R.raw.sfx_defeat)
        }

        private fun playOneShotTrack(context: Context, @RawRes resId: Int) {
            stopMusic()
            if (!HexaFleetSettings.isSoundEnabled || HexaFleetSettings.soundVolume <= 0.01f) return
            if (isDeviceSilentOrMuted(context)) return

            try {
                val mp = MediaPlayer.create(context.applicationContext, resId) ?: return
                val vol = HexaFleetSettings.soundVolume.coerceIn(0f, 1f)
                mp.isLooping = false
                mp.setVolume(vol, vol)
                mp.setOnCompletionListener { player ->
                    try {
                        player.release()
                    } catch (_: Exception) {}
                    if (activeMediaPlayer === player) {
                        activeMediaPlayer = null
                    }
                }
                activeMediaPlayer = mp
                mp.start()
            } catch (e: Exception) {
                activeMediaPlayer = null
            }
        }

        /**
         * Ayarlar penceresinden ses seviyesi veya ses açık/kapalı durumu değiştirildiğinde
         * o anda çalmakta olan müziğin ses seviyesini anlık olarak senkronize eder.
         */
        fun syncVolumeWithSettings() {
            val player = activeMediaPlayer ?: return
            try {
                val ctx = appContextRef
                if (!HexaFleetSettings.isSoundEnabled ||
                    HexaFleetSettings.soundVolume <= 0.01f ||
                    (ctx != null && isDeviceSilentOrMuted(ctx))
                ) {
                    stopMusic()
                } else {
                    val vol = HexaFleetSettings.soundVolume.coerceIn(0f, 1f)
                    player.setVolume(vol, vol)
                }
            } catch (_: Exception) {
                activeMediaPlayer = null
            }
        }

        /**
         * Aktif çalan müziği güvenli şekilde durdurur ve belleği temizler.
         */
        fun stopMusic() {
            val player = activeMediaPlayer ?: return
            activeMediaPlayer = null
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: Exception) {}
            try {
                player.release()
            } catch (_: Exception) {}
        }
    }
}
