package com.hexafleet.domain.game.model

import com.hexafleet.core.hexmath.HexCoordinate
import kotlinx.serialization.Serializable

/**
 * Oyuncu Tanımlayıcıları
 */
@Serializable
enum class PlayerId {
    BLUE, // Mavi Filo (Oyuncu 1 / Host)
    RED   // Kırmızı Filo (Oyuncu 2 / Rakip / AI)
}

/**
 * 5 Temel Gemi Sınıfı ve Özellikleri
 */
@Serializable
enum class ShipClass(
    val displayName: String,
    val length: Int,
    val maxHp: Int,
    val abilityCpCost: Int,
    val cooldownRounds: Int,
    val canSubmerge: Boolean = false
) {
    DRONE_CARRIER(
        displayName = "İHA Taşıyıcı",
        length = 3,
        maxHp = 3,
        abilityCpCost = 3,
        cooldownRounds = 3
    ),
    EMP_CRUISER(
        displayName = "EMP Kruvazörü",
        length = 3,
        maxHp = 3,
        abilityCpCost = 2,
        cooldownRounds = 4
    ),
    AEGIS_DESTROYER(
        displayName = "Aegis Muhribi",
        length = 2,
        maxHp = 2,
        abilityCpCost = 1,
        cooldownRounds = 3
    ),
    STEALTH_SUBMARINE(
        displayName = "Hayalet Denizaltı",
        length = 2,
        maxHp = 2,
        abilityCpCost = 2,
        cooldownRounds = 3,
        canSubmerge = true
    ),
    CORVETTE_MINELAYER(
        displayName = "Hücumbot & Mayın Gemisi",
        length = 1,
        maxHp = 1,
        abilityCpCost = 1,
        cooldownRounds = 2
    ),
    DREADNOUGHT(
        displayName = "Amiral Zırhlısı",
        length = 4,
        maxHp = 4,
        abilityCpCost = 2,
        cooldownRounds = 4
    )
}

/**
 * Geminin her bir petekteki gövde parçası
 */
@Serializable
data class ShipSegment(
    val coordinate: HexCoordinate,
    val isDamaged: Boolean = false
)

/**
 * Harp sahasındaki aktif gemi varlığı
 */
@Serializable
data class Ship(
    val id: String,
    val owner: PlayerId,
    val shipClass: ShipClass,
    val segments: List<ShipSegment>,
    val orientation: Int = 0, // 0..5 ana eksen yönü
    val abilityCooldown: Int = 0,
    val isShielded: Boolean = false,
    val empLockTurnsRemaining: Int = 0,
    val isSubmerged: Boolean = false
) {
    val isSunk: Boolean get() = segments.all { it.isDamaged }
    val currentHp: Int get() = segments.count { !it.isDamaged }
    val coordinates: List<HexCoordinate> get() = segments.map { it.coordinate }

    fun containsCoordinate(coord: HexCoordinate): Boolean =
        segments.any { it.coordinate == coord }

    fun damageSegmentAt(coord: HexCoordinate): Ship {
        val updated = segments.map { segment ->
            if (segment.coordinate == coord) segment.copy(isDamaged = true) else segment
        }
        return copy(segments = updated)
    }

    fun hasAbilityReady(): Boolean =
        abilityCooldown == 0 && empLockTurnsRemaining == 0 && !isSunk
}

/**
 * Topoğrafik çevre ve taktiksel engel türleri
 */
@Serializable
sealed class Terrain {
    @Serializable data object Water : Terrain()
    @Serializable data object CoralReef : Terrain()
    @Serializable data object MagneticStorm : Terrain()
    @Serializable data class Current(val directionIndex: Int) : Terrain()
    @Serializable data object Trench : Terrain()
}

/**
 * Tahtadaki tek bir peteğin veri modeli
 */
@Serializable
data class BoardTile(
    val coordinate: HexCoordinate,
    val terrain: Terrain = Terrain.Water,
    val hasMine: Boolean = false,
    val hasDecoy: Boolean = false,
    val isAttacked: Boolean = false
)

