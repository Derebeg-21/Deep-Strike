package com.hexafleet.feature.board.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * HexaFleet küresel oyun tercihleri ve kuralları yöneticisi.
 * Ses seviyesi, radar bip sesleri, haptik titreşim ve maç kurallarını yönetir.
 */
object HexaFleetSettings {
    // Ses ve Efekt Ayarları
    var isSoundEnabled by mutableStateOf(true)
    var soundVolume by mutableFloatStateOf(0.85f)
    var isRadarPingEnabled by mutableStateOf(true)

    // Dokunsal Geri Bildirim (Haptics)
    var isHapticsEnabled by mutableStateOf(true)

    // Varsayılan Oyun Kural Tercihleri
    var defaultTurnDurationSeconds by mutableIntStateOf(45) // 15: Blitz, 45: Standart, 60: Taktik
    var isFogOfWarDefault by mutableStateOf(true)
    var isRadarSweepDefault by mutableStateOf(true)
    var defaultMapRadius by mutableIntStateOf(5) // 5: Standart (91 Petek), 6: Geniş (127 Petek), 7: Devasa (169 Petek)
    var areTerrainHazardsEnabled by mutableStateOf(true)
}
