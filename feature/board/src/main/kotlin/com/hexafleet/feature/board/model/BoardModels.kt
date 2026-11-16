package com.hexafleet.feature.board.model

import com.hexafleet.core.hexmath.HexCoordinate
import com.hexafleet.domain.game.ai.AiDifficulty

/**
 * Altıgen peteğin topoğrafik çevre tipi.
 */
enum class TerrainType {
    WATER,            // Standart serbest okyanus suyu
    CORAL_REEF,       // Torpido ve gemi geçişini engelleyen mercan kayalığı
    MAGNETIC_STORM,   // İHA ve radar taramalarını engelleyen iyon fırtınası
    OCEAN_CURRENT_E,  // Doğuya doğru sürükleyen akıntı
    OCEAN_CURRENT_W,  // Batıya doğru sürükleyen akıntı
    DEEP_TRENCH       // Yalnızca denizaltıların dalabildiği derin hendek
}

/**
 * Savaş Sisi (Fog of War) görünürlük durumu.
 */
enum class FogVisibility {
    UNEXPLORED, // Tamamen keşfedilmemiş (Sisli)
    FOGGED,     // Daha önce taranmış
    VISIBLE     // Açık ve net görüş
}

/**
 * Kullanıcı dokunuşu ve taktiksel hedefleme vurgusu.
 */
enum class TileHighlightState {
    NONE,
    SELECTED,          // Seçili petek
    RECON_CLEAR,       // İHA taraması: Boş su
    RECON_DETECTED,    // İHA taraması: Gemi sinyali tespit edildi!
    SHIELD_ABSORBED,   // Kalkan tarafından sönümlenmiş atış bölgesi
    HIT,               // İsabet almış gemi parçası (Patlama)
    MISS,              // Boş suya düşen karavana atış
    SUNK_SHIP          // Tamamen batırılmış gemi parçası
}

/**
 * Muharebe Aşaması: Önce Gemi Yerleştirme, sonra Sıra Tabanlı Savaş.
 */
enum class BattlePhase {
    DEPLOYMENT, // Filo konuşlandırma (Gemi koyma ve yön döndürme)
    COMBAT      // Aktif sıra tabanlı Amiral Battı muharebesi
}

/**
 * Haritadaki tek bir altıgen peteğin UI durum modeli.
 */
data class HexTileUiModel(
    val coordinate: HexCoordinate,
    val terrain: TerrainType = TerrainType.WATER,
    val visibility: FogVisibility = FogVisibility.VISIBLE,
    val highlight: TileHighlightState = TileHighlightState.NONE,
    val shipPartName: String? = null,
    val isShipPartHit: Boolean = false,
    val isRevealedByDrone: Boolean = false
)

/**
 * Tahtada gösterilen dost veya düşman gemisi.
 */
data class ShipDisplayModel(
    val id: String,
    val name: String,
    val length: Int = parts.size,
    val parts: List<HexCoordinate>,
    val hitParts: Set<HexCoordinate> = emptySet(),
    val isEnemy: Boolean = false,
    val isShielded: Boolean = false,
    val isTriangleShape: Boolean = false,
    val directionIndex: Int = 0 // 0: Doğu-Batı, 1: KD-GB, 2: KB-GD (üçgen için yön rotasyonu 0..5)
) {
    val remainingHp: Int get() = (parts.size - hitParts.size).coerceAtLeast(0)
    val isSunk: Boolean get() = hitParts.size >= parts.size && parts.isNotEmpty()
}

/**
 * 7 Peteklik Heksagonal Aegis Savunma Kubbesi (Shield Dome).
 * Merkez + komşu 6 peteği (toplam 7 petek) korur.
 * Merkeze İHA gelirse hemen kalkmaz, 1 hakkı daha kalır (centerHp: 2 -> 1).
 * Komşu hücrelere İHA gelirse kalkan kubbesi çöker.
 * Normal atış gelirse darbe sönümlenir, rakip sadece kalkan bilgisi alır (gemi var mı bilmez).
 */
data class ShieldDome(
    val center: HexCoordinate,
    val centerHp: Int = 2, // 2: Tam güç, 1: 1 darbe almış (1 hakkı kaldı), 0: Çöktü
    val protectedCoords: Set<HexCoordinate> = (center.ring(1) + center).toSet()
)

/**
 * 2 Peteklik Radyoaktif Sis Bölgesi (Radioactive Mist Zone).
 * İHA geldiğinde bu alandaki gemileri gizler (sinyali bloke eder, radar boş gösterir).
 * Top atışlarına karşı koruma sağlamaz (gemi vurulabilir).
 * Sadece dost haritada estetik yeşil sis olarak görünür; düşman haritasında gizlidir.
 */
data class RadioactiveZone(
    val id: String,
    val name: String = "Radyoaktif Bölge",
    val parts: List<HexCoordinate> = emptyList(),
    val directionIndex: Int = 0
)

/**
 * Oyun Başında Ayarlanabilir Taktiksel Parametreler (Kalkan, Radyoaktif Bölge, Resif, Fırtına).
 */
data class GameCustomRules(
    val maxShieldDomes: Int = 1,          // 0 - 5 (Standart: 1, Devasa: 2)
    val radioactiveZonesCount: Int = 1,   // 0 - 5 (R=5: 1, R=6: 2, R=7: 3)
    val coralReefCount: Int = 4,          // 0 - 10 (R=5: 4, R=6: 5, R=7: 6)
    val magneticStormCount: Int = 4       // 0 - 10 (R=5: 4, R=6: 6, R=7: 8)
) {
    companion object {
        fun defaultForRadius(radius: Int): GameCustomRules {
            val shields = if (radius >= 7) 2 else 1
            val radioactive = when {
                radius <= 5 -> 1
                radius == 6 -> 2
                else -> 3
            }
            val reefs = when {
                radius <= 5 -> 4
                radius == 6 -> 5
                else -> 6
            }
            val storms = when {
                radius <= 5 -> 4
                radius == 6 -> 6
                else -> 8
            }
            return GameCustomRules(
                maxShieldDomes = shields,
                radioactiveZonesCount = radioactive,
                coralReefCount = reefs,
                magneticStormCount = storms
            )
        }
    }
}

/**
 * Hamle Geçmişi ve Muharebe Günlüğü Kaydı
 */
data class CombatLogEntry(
    val turnNumber: Int,
    val actor: String, // "DOST", "SİZ" veya "DÜŞMAN"
    val actionType: String, // "ATIŞ", "İHA", "KALKAN", "BATIK"
    val coordinate: HexCoordinate,
    val resultTitle: String,
    val detailMessage: String,
    val highlightState: TileHighlightState = TileHighlightState.NONE
) {
    val isFriendly: Boolean get() = actor == "DOST" || actor == "SİZ"
    val displayCoordinate: String
        get() = if (!isFriendly && actionType.contains("KALKAN")) "(Gizli Sektör)" else "(${coordinate.q}, ${coordinate.r})"
}

/**
 * Oyun Modu (Çevrimiçi vs Yapay Zeka)
 */
sealed class TacticalGameMode {
    data class Online(
        val roomCode: String,
        val isHost: Boolean,
        val turnDurationSeconds: Int = 45
    ) : TacticalGameMode()

    data class VsAi(
        val difficulty: AiDifficulty = AiDifficulty.VETERAN,
        val turnDurationSeconds: Int = 45
    ) : TacticalGameMode()
}

/**
 * Tek bir altıgen haritanın (Dost Savunma veya Düşman Saldırı) çizim durumu.
 */
data class HexBoardUiState(
    val radius: Int = 5,
    val tiles: Map<HexCoordinate, HexTileUiModel> = emptyMap(),
    val ships: List<ShipDisplayModel> = emptyList(),
    val selectedCoordinate: HexCoordinate? = null,
    val previewShipParts: List<HexCoordinate> = emptyList(),
    val isPreviewValid: Boolean = true,
    val previewCategory: String = "SHIP", // "SHIP" veya "RADIOACTIVE"
    val isAttackBoard: Boolean = false, // true: Düşman Hedef Haritası, false: Dost Filo Haritası
    val isFogOfWarActive: Boolean = false,
    val isRadarSweepActive: Boolean = true,
    val shieldDome: ShieldDome? = null, // Geriye dönük uyumluluk
    val shieldDomes: List<ShieldDome> = if (shieldDome != null) listOf(shieldDome) else emptyList(),
    val shieldAbsorbedCoords: Set<HexCoordinate> = emptySet(),
    val radioactiveCoords: Set<HexCoordinate> = emptySet(), // Sadece dost haritada taktiksel yeşil sis
    val lastEnemyAttacks: List<HexCoordinate> = emptyList(), // Son turda düşmanın ateş ettiği koordinatlar (Pulse animation)
    val lastEnemyDroneCenter: HexCoordinate? = null // Son turda düşmanın İHA taradığı merkez (Radar pulse)
)

/**
 * İki haritalı (Dost Filo + Düşman Radar) Amiral Battı oyununun bütünsel durumu.
 */
data class DualBattleUiState(
    val radius: Int = 5,
    val phase: BattlePhase = BattlePhase.DEPLOYMENT,
    val customRules: GameCustomRules = GameCustomRules.defaultForRadius(radius),
    // Coğrafi Arazi Haritaları: Resifler AYNI, Manyetik Fırtınalar FARKLI ve Düşmanda GİZLİ!
    val friendlyTerrainMap: Map<HexCoordinate, TerrainType> = emptyMap(),
    val enemyTerrainMap: Map<HexCoordinate, TerrainType> = emptyMap(),
    // Ortak Resif Koordinatları (Oyun başında rastgele üretilir, iki taraf için aynıdır)
    val sharedReefCoords: Set<HexCoordinate> = emptySet(),
    // Radyoaktif Bölgeler (İHA tespitini engelleyen taktiksel yeşil sis bölgeleri)
    val friendlyRadioactiveZones: List<RadioactiveZone> = emptyList(),
    val enemyRadioactiveZones: List<RadioactiveZone> = emptyList(),
    // Dost Filo ve Düşmanın Bize Yaptığı Atışlar
    val friendlyShips: List<ShipDisplayModel> = emptyList(),
    val enemyShotsOnPlayer: Map<HexCoordinate, TileHighlightState> = emptyMap(),
    val friendlyShieldDome: ShieldDome? = null,
    val friendlyShieldDomes: List<ShieldDome> = if (friendlyShieldDome != null) listOf(friendlyShieldDome) else emptyList(),
    // Düşman Filo ve Bizim Düşmana Yaptığımız Atışlar
    val enemyShips: List<ShipDisplayModel> = emptyList(),
    val playerShotsOnEnemy: Map<HexCoordinate, TileHighlightState> = emptyMap(),
    val droneScannedHexes: Map<HexCoordinate, TileHighlightState> = emptyMap(),
    val enemyShieldDome: ShieldDome? = null,
    val enemyShieldDomes: List<ShieldDome> = if (enemyShieldDome != null) listOf(enemyShieldDome) else emptyList(),
    // Kalkan tarafından sönümlenmiş aktif vuruşlar (kalkan kalkınca temizlenir)
    val friendlyShieldAbsorbedCoords: Set<HexCoordinate> = emptySet(),
    val enemyShieldAbsorbedCoords: Set<HexCoordinate> = emptySet(),
    // Düşmanın Son Turdaki Hamleleri (Animasyon ve Görsel Farkındalık için)
    val lastEnemyAttacks: List<HexCoordinate> = emptyList(),
    val lastEnemyDroneCenter: HexCoordinate? = null,
    // Seçimler ve Yerleştirme Durumu
    val selectedDeploymentCategory: String = "SHIP", // "SHIP" veya "RADIOACTIVE"
    val selectedDeploymentShipId: String = "corvette_friendly",
    val selectedDeploymentRadioactiveIndex: Int = 0,
    val deploymentDirectionIndex: Int = 0, // 0: Doğu-Batı (──), 1: KD-GB (╱), 2: KB-GD (╲)
    val selectedAttackCoord: HexCoordinate? = null,
    val selectedTargetCoord: HexCoordinate? = null,
    val selectedShieldCoord: HexCoordinate? = null,
    // Tur ve Komut Puanı (CP)
    val commandPoints: Int = 3,
    val maxCommandPoints: Int = 3,
    val turnNumber: Int = 1,
    val isPlayerTurn: Boolean = true,
    val winner: String? = null, // "PLAYER" veya "ENEMY"
    val lastBannerMessage: String = "Filonuzu haritaya yerleştirin ve savaşı başlatın!",
    val structuredLogs: List<CombatLogEntry> = emptyList(),
    val battleLog: List<String> = listOf("Filo konuşlandırma aşaması. Gemilerinizi yerleştirin.")
) {
    /**
     * Dost Savunma Haritası için HexBoardUiState üretir.
     * SADECE BURADA dost kalkan kubbeleri (friendlyShieldDomes) estetik flu hale olarak çizilir!
     * Ve oyuncunun kendi manyetik fırtınaları ile radyoaktif yeşil sisleri burada görünür.
     * Düşmanın son turdaki atış ve İHA saldırıları burada animasyonlu vurgulanır.
     */
    fun toFriendlyBoardState(): HexBoardUiState {
        val allCoords = HexCoordinate.ZERO.area(radius)
        val tileMap = mutableMapOf<HexCoordinate, HexTileUiModel>()
        val radCoords = friendlyRadioactiveZones.flatMap { it.parts }.toSet()

        for (c in allCoords) {
            val terrain = friendlyTerrainMap[c] ?: TerrainType.WATER
            val shotState = enemyShotsOnPlayer[c] ?: TileHighlightState.NONE
            val isShieldedHit = c in friendlyShieldAbsorbedCoords

            val highlight = when {
                isShieldedHit -> TileHighlightState.SHIELD_ABSORBED
                else -> shotState
            }

            tileMap[c] = HexTileUiModel(
                coordinate = c,
                terrain = terrain,
                visibility = FogVisibility.VISIBLE,
                highlight = highlight,
                isShipPartHit = highlight == TileHighlightState.HIT || highlight == TileHighlightState.SUNK_SHIP
            )
        }
        return HexBoardUiState(
            radius = radius,
            tiles = tileMap,
            ships = friendlyShips,
            selectedCoordinate = if (phase == BattlePhase.DEPLOYMENT) null else selectedShieldCoord,
            isAttackBoard = false,
            isFogOfWarActive = false,
            isRadarSweepActive = false,
            shieldDome = friendlyShieldDomes.firstOrNull(),
            shieldDomes = friendlyShieldDomes,
            shieldAbsorbedCoords = friendlyShieldAbsorbedCoords,
            radioactiveCoords = radCoords,
            lastEnemyAttacks = lastEnemyAttacks,
            lastEnemyDroneCenter = lastEnemyDroneCenter
        )
    }

    /**
     * Düşman Hedef / Saldırı Haritası için HexBoardUiState üretir.
     * DÜŞMANIN MANYETİK FIRTINALARI VE RADYOAKTİF BÖLGELERİ ASLA ÇİZİLMEZ! (Oyuncu bilmemelidir).
     * DÜŞMANIN KALKANI DA BURADA ÇİZİLMEZ! (Sadece vurulursa sönümlenme bilgisi gelir).
     * Düşman gemileri yalnızca batırıldığında (isSunk) tam silüetiyle görünür!
     */
    fun toEnemyAttackBoardState(isRadarActive: Boolean = true): HexBoardUiState {
        val allCoords = HexCoordinate.ZERO.area(radius)
        val tileMap = mutableMapOf<HexCoordinate, HexTileUiModel>()
        val sunkEnemyCoords = enemyShips.filter { it.isSunk }.flatMap { it.parts }.toSet()

        for (c in allCoords) {
            // Resifler ortak olduğu için saldırı haritasında görünür; fakat düşmanın manyetik fırtınaları gizlidir (WATER gibi görünür)!
            val terrain = if (c in sharedReefCoords) TerrainType.CORAL_REEF else TerrainType.WATER
            val playerShot = playerShotsOnEnemy[c]
            val droneScan = droneScannedHexes[c]
            val isShieldHit = c in enemyShieldAbsorbedCoords

            val highlight = when {
                c in sunkEnemyCoords -> TileHighlightState.SUNK_SHIP
                isShieldHit -> TileHighlightState.SHIELD_ABSORBED
                playerShot != null -> playerShot
                droneScan != null -> droneScan
                else -> TileHighlightState.NONE
            }

            val isExplored = playerShot != null || droneScan != null || c in sunkEnemyCoords || isShieldHit

            tileMap[c] = HexTileUiModel(
                coordinate = c,
                terrain = terrain,
                visibility = if (isExplored) FogVisibility.VISIBLE else FogVisibility.UNEXPLORED,
                highlight = highlight,
                isShipPartHit = highlight == TileHighlightState.HIT || highlight == TileHighlightState.SUNK_SHIP,
                isRevealedByDrone = droneScan != null
            )
        }

        // Saldırı haritasında yalnızca BATIRILMIŞ düşman gemilerinin gövdesi çizilir!
        val visibleEnemyShips = enemyShips.filter { it.isSunk }

        return HexBoardUiState(
            radius = radius,
            tiles = tileMap,
            ships = visibleEnemyShips,
            selectedCoordinate = selectedTargetCoord,
            isAttackBoard = true,
            isFogOfWarActive = true,
            isRadarSweepActive = isRadarActive,
            shieldDome = null, // Düşman saldırı haritasında kalkan gösterilmez!
            shieldAbsorbedCoords = enemyShieldAbsorbedCoords
        )
    }
}
