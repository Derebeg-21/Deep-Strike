package com.hexafleet.domain.game.ai

import com.hexafleet.core.hexmath.HexCoordinate
import com.hexafleet.domain.game.engine.GameState
import com.hexafleet.domain.game.model.PlayerId
import com.hexafleet.domain.game.model.Terrain

/**
 * 6 Eksenli Altıgen Olasılık Yoğunluk Haritası (Hex-Probability Density Function - H-PDF).
 * Klasik Amiral Battı'daki Hunt/Target algoritmasını altıgen geometriye ve taktiksel engellere uyarlar.
 */
object HexProbabilityMap {

    /**
     * Verilen oyun durumuna göre tüm keşfedilmemiş petekler için olasılık puanı hesaplar.
     * En yüksek puanlı petek, düşman gemisi barındırma ihtimali en yüksek olan noktadır.
     */
    fun calculateDensity(
        state: GameState,
        aiPlayer: PlayerId = PlayerId.RED
    ): Map<HexCoordinate, Int> {
        val opponent = state.getOpponent(aiPlayer)
        val opponentShips = state.getPlayerShips(opponent)
        val remainingShipLengths = opponentShips.filter { !it.isSunk }.map { it.shipClass.length }

        val allCoords = HexCoordinate.ZERO.area(state.radius)
        val attackedCoords = state.tiles.filter { it.value.isAttacked }.keys
        val hitCoords = opponentShips.flatMap { ship ->
            ship.segments.filter { it.isDamaged }.map { it.coordinate }
        }.toSet()
        val sunkCoords = opponentShips.filter { it.isSunk }.flatMap { it.coordinates }.toSet()
        val activeHitCoords = hitCoords - sunkCoords // Henüz batmamış aktif isabetler

        val densityMap = mutableMapOf<HexCoordinate, Int>()
        for (coord in allCoords) {
            densityMap[coord] = 0
        }

        // Geçersiz petekler: Daha önce ıskalanmış (miss), batmış gemi parçası veya resif olan yerler
        val blockedCoords = attackedCoords.filter { it !in activeHitCoords }.toSet() +
                state.tiles.filter { it.value.terrain is Terrain.CoralReef }.keys

        // 3 Ana Eksen (Pointy-topped altıgende birbirine zıt yön çiftleri):
        // 0: Doğu-Batı (DIRECTIONS[0] ve DIRECTIONS[3])
        // 1: Kuzeydoğu-Güneybatı (DIRECTIONS[1] ve DIRECTIONS[4])
        // 2: Kuzeybatı-Güneydoğu (DIRECTIONS[2] ve DIRECTIONS[5])
        val primaryDirections = listOf(
            HexCoordinate.DIRECTIONS[0], // E
            HexCoordinate.DIRECTIONS[1], // NE
            HexCoordinate.DIRECTIONS[2]  // NW
        )

        // Kalan her gemi uzunluğu için olası tüm altıgen doğrusal dizilimleri tara
        for (length in remainingShipLengths) {
            for (coord in allCoords) {
                for (dir in primaryDirections) {
                    val potentialShipSegments = mutableListOf<HexCoordinate>()
                    var isValidPlacement = true

                    for (step in 0 until length) {
                        val segmentCoord = coord + (dir * step)
                        // 1. Harita sınırları içinde mi?
                        if (segmentCoord.distanceTo(HexCoordinate.ZERO) > state.radius) {
                            isValidPlacement = false
                            break
                        }
                        // 2. Engel veya bilinen karavana ile çakışıyor mu?
                        if (segmentCoord in blockedCoords) {
                            isValidPlacement = false
                            break
                        }
                        potentialShipSegments.add(segmentCoord)
                    }

                    if (isValidPlacement) {
                        // Eğer bu yerleşim hattı daha önce vurulmuş aktif bir isabeti (activeHit) içeriyorsa
                        // olasılık çarpanını dramatik olarak artır (Hedef Takip Modu - Target Mode)
                        val containsActiveHit = potentialShipSegments.any { it in activeHitCoords }
                        val weight = if (containsActiveHit) 15 else 1

                        for (seg in potentialShipSegments) {
                            // Henüz ateş edilmemiş peteklerin puanını artır
                            if (seg !in attackedCoords) {
                                densityMap[seg] = (densityMap[seg] ?: 0) + weight
                            }
                        }
                    }
                }
            }
        }

        return densityMap
    }
}

