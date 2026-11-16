package com.hexafleet.feature.board.theme

import androidx.compose.ui.graphics.Color

/**
 * HexaFleet Standart Renk Sistemi (White Base, Lacivert, Mor, Kırmızı).
 * Modern taktiksel deniz harp arayüzü için yüksek kontrastlı ve senkron renk paleti.
 */
object BoardColors {
    // ==========================================
    // 1. ZEMİN VE BEYAZ TABAN (White Base)
    // ==========================================
    val LightBackground = Color(0xFFF8FAFC)        // Slate 50: Temiz, ferah operasyon zemin rengi
    val PureWhite = Color(0xFFFFFFFF)              // Saf Beyaz: Kart, panel ve diyalog zeminleri
    val CardBackground = Color(0xFFFFFFFF)         // Standart kart zemin rengi
    val SurfaceSubtle = Color(0xFFF1F5F9)          // Slate 100: Hafif gölgeli paneller ve girdi alanları
    val SurfaceElevated = Color(0xFFFFFFFF)        // Yükseltilmiş yüzey
    val OceanTile = Color(0xFFE2E8F0)              // Slate 200: Taktiksel deniz altıgen peteği
    val OceanTileDeep = Color(0xFFCBD5E1)          // Slate 300: Vurgulu deniz peteği

    // ==========================================
    // 2. LACİVERT (Deep Navy Blue - Yapı ve Başlıklar)
    // ==========================================
    val NavyDark = Color(0xFF0F172A)               // Slate 900: Derin kurumsal lacivert (Başlıklar, ana yazılar)
    val NavyPrimary = Color(0xFF1E293B)            // Slate 800: Zengin lacivert
    val NavyAccent = Color(0xFF1E3A8A)             // Blue 900: Klasik amirallik laciverti (Sekmeler, aktif rozetler)
    val HexGridStroke = Color(0xFF94A3B8)          // Slate 400: Net, okunabilir altıgen ızgara hatları
    val HexGridHighlight = Color(0xFF1E3A8A)       // Seçili petek lacivert hat vurgusu
    val BorderSubtle = Color(0xFFCBD5E1)           // Slate 300: Kart ve panel sınır çizgileri
    val TextPrimary = Color(0xFF0F172A)            // Yüksek kontrastlı lacivert-siyah gövde metni
    val TextSecondary = Color(0xFF475569)          // Slate 600: İkincil açıklama ve telemetri metinleri
    val TextMuted = Color(0xFF94A3B8)              // Slate 400: Pasif ve ipucu metinleri

    // ==========================================
    // 3. KIRMIZI (Crimson / Scarlet Red - Taarruz ve Eylem)
    // ==========================================
    val CrimsonRed = Color(0xFFDC2626)             // Red 600: Ana ateşleme, tehlike ve düşman rengi
    val CrimsonRedDark = Color(0xFFB91C1C)         // Red 700: Vurgulu kırmızı
    val RedLight = Color(0xFFFEE2E2)               // Red 100: Yumuşak kırmızı rozet arka planı
    val HitExplosion = Color(0xFFDC2626)           // Doğrudan isabet ve patlama rengi
    val EnemyHull = Color(0xFFDC2626)              // Düşman gemisi gövde rengi
    val CoralReef = Color(0xFFEA580C)              // Orange 600: Mercan resifi tehlike işareti

    // ==========================================
    // 4. MOR (Tactical Purple / Violet - Siber Harp ve Yetenekler)
    // ==========================================
    val PurplePrimary = Color(0xFF7C3AED)          // Violet 600: İHA keşfi, oda kodu ve siber yetenekler
    val PurpleDark = Color(0xFF5B21B6)             // Violet 800: Derin mor vurgular
    val PurpleLight = Color(0xFFEDE9FE)            // Violet 100: Mor çip ve alan zeminleri
    val MagneticStorm = Color(0xFF8B5CF6)          // Violet 500: Manyetik iyon fırtınası
    val DroneRecon = Color(0xFF7C3AED)             // İHA hava keşif çemberi
    val EmpShock = Color(0xFF6D28D9)               // EMP elektro-manyetik şok darbesi

    // ==========================================
    // 5. DENİZ & TAKTİKSEL ÇEVRE ELEMANLARI
    // ==========================================
    val MissSplash = Color(0xFF0284C7)             // Sky 600: Suya düşen mermi sıçrama halkası
    val ShieldColor = Color(0xFF2563EB)            // Blue 600: Aegis koruyucu enerji kalkanı
    val ShieldCyan = Color(0xFF2563EB)             // Geriye dönük uyumluluk
    val OceanCurrent = Color(0xFF0284C7)           // Okyanus akıntısı oku
    val DeepTrench = Color(0xFF334155)             // Derin deniz yarığı
    val RadarBeam = Color(0xFF7C3AED)              // Mor siber radar süpürme ışını
    val RadarBeamTransparent = Color(0x007C3AED)   // Saydam radar başlangıcı
    val FogOfWarMask = Color(0xAA94A3B8)           // Yarı saydam taktiksel sis perdesi
    val PitchBlackFog = Color(0xFFCBD5E1)          // Keşfedilmemiş sis zemin rengi
    val FoggedMemoryAlpha = 0.40f

    // ==========================================
    // 6. HUD VE KOMUTA KONTROLLERİ
    // ==========================================
    val HudBackground = Color(0xFFFFFFFF)          // Komuta güvertesi beyaz zemin
    val CommandPointActive = Color(0xFF7C3AED)     // Aktif Komut Puanı (Mor neon)
    val CommandPointEmpty = Color(0xFFE2E8F0)      // Boş Komut Puanı (Açık slate)
    val FriendlyHull = Color(0xFF1E3A8A)           // Dost gemi gövde rengi (Lacivert)

    // ==========================================
    // Geriye Dönük Uyumluluk Takma Adları
    // ==========================================
    val DeepOcean = LightBackground
}
