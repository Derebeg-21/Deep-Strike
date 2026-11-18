package com.hexafleet.feature.board.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hexafleet.feature.board.audio.HexaFleetSoundManager

/**
 * HexaFleet küresel oyun tercihleri ve kuralları yöneticisi.
 * Ses seviyesi, radar bip sesleri, haptik titreşim ve maç kurallarını yönetir ve kalıcı olarak saklar.
 */
object HexaFleetSettings {
    private const val PREFS_NAME = "hexafleet_settings_prefs"
    private var prefs: SharedPreferences? = null

    private var _isSoundEnabled by mutableStateOf(true)
    var isSoundEnabled: Boolean
        get() = _isSoundEnabled
        set(value) {
            _isSoundEnabled = value
            prefs?.edit()?.putBoolean("isSoundEnabled", value)?.apply()
            HexaFleetSoundManager.syncVolumeWithSettings()
        }

    private var _soundVolume by mutableFloatStateOf(0.85f)
    var soundVolume: Float
        get() = _soundVolume
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            _soundVolume = clamped
            prefs?.edit()?.putFloat("soundVolume", clamped)?.apply()
            HexaFleetSoundManager.syncVolumeWithSettings()
        }

    private var _isRadarPingEnabled by mutableStateOf(true)
    var isRadarPingEnabled: Boolean
        get() = _isRadarPingEnabled
        set(value) {
            _isRadarPingEnabled = value
            prefs?.edit()?.putBoolean("isRadarPingEnabled", value)?.apply()
        }

    // Dokunsal Geri Bildirim (Haptics)
    private var _isHapticsEnabled by mutableStateOf(true)
    var isHapticsEnabled: Boolean
        get() = _isHapticsEnabled
        set(value) {
            _isHapticsEnabled = value
            prefs?.edit()?.putBoolean("isHapticsEnabled", value)?.apply()
        }

    // Varsayılan Oyun Kural Tercihleri
    private var _defaultTurnDurationSeconds by mutableIntStateOf(45)
    var defaultTurnDurationSeconds: Int
        get() = _defaultTurnDurationSeconds
        set(value) {
            _defaultTurnDurationSeconds = value
            prefs?.edit()?.putInt("defaultTurnDurationSeconds", value)?.apply()
        }

    var isFogOfWarDefault by mutableStateOf(true)
    var isRadarSweepDefault by mutableStateOf(true)

    private var _defaultMapRadius by mutableIntStateOf(5) // 5: Standart (91 Petek), 7: Geniş (169 Petek), 8: Devasa (217 Petek)
    var defaultMapRadius: Int
        get() = _defaultMapRadius
        set(value) {
            _defaultMapRadius = value
            prefs?.edit()?.putInt("defaultMapRadius", value)?.apply()
        }

    var areTerrainHazardsEnabled by mutableStateOf(true)

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        _isSoundEnabled = sp.getBoolean("isSoundEnabled", true)
        _soundVolume = sp.getFloat("soundVolume", 0.85f).coerceIn(0f, 1f)
        _isRadarPingEnabled = sp.getBoolean("isRadarPingEnabled", true)
        _isHapticsEnabled = sp.getBoolean("isHapticsEnabled", true)
        _defaultTurnDurationSeconds = sp.getInt("defaultTurnDurationSeconds", 45)
        val savedRadius = sp.getInt("defaultMapRadius", 5)
        _defaultMapRadius = if (savedRadius == 6) 7 else savedRadius
    }
}
