package com.hexafleet.domain.game.ai

import com.hexafleet.core.hexmath.HexCoordinate
import com.hexafleet.domain.game.engine.GameEngine
import com.hexafleet.domain.game.engine.GameState
import com.hexafleet.domain.game.model.GameActionResult
import com.hexafleet.domain.game.model.PlayerId
import com.hexafleet.domain.game.model.ShipClass
import kotlin.random.Random

/**
 * Taktiksel Çevrimdışı Yapay Zeka Amirali (AI Commander).
 * Zorluk derecesine göre H-PDF ısı haritasını, yetenek kullanımını ve kaçış manevralarını yönetir.
 */
class TacticalAiCommander(
    val difficulty: AiDifficulty = AiDifficulty.VETERAN,
    val aiPlayer: PlayerId = PlayerId.RED,
    private val random: Random = Random.Default
) {
    /**
     * AI'ın sırası geldiğinde mevcut Komut Puanlarını (CP) kullanarak tam bir tur hamlesi icra eder.
     * @return Yapılan hamleler sonucunda güncellenmiş ve sırası devredilmiş GameState
     */
    fun playTurn(initialState: GameState): GameState {
        var currentState = initialState
        if (currentState.activePlayer != aiPlayer || currentState.winner != null) {
            return currentState
        }

        // Büyük Amiral: Önce taktiksel yetenekleri ve manevraları değerlendir
        if (difficulty == AiDifficulty.GRAND_ADMIRAL) {
            currentState = considerTacticalAbilitiesAndEvasion(currentState)
        }

        // Kalan CP bitene kadar atış yap
        while (currentState.getCp(aiPlayer) >= 1 && currentState.winner == null) {
            val target = selectTargetCoordinate(currentState) ?: break
            val (nextState, _) = GameEngine.fire(currentState, target)
            currentState = nextState
        }

        // Turu tamamla ve sırayı devret
        return if (currentState.winner == null) GameEngine.endTurn(currentState) else currentState
    }

    /**
     * Zorluk seviyesine göre hedef koordinat seçimi
     */
    fun selectTargetCoordinate(state: GameState): HexCoordinate? {
        val allCoords = HexCoordinate.ZERO.area(state.radius)
        val unattackedCoords = allCoords.filter { coord ->
            state.tiles[coord]?.isAttacked != true
        }

        if (unattackedCoords.isEmpty()) return null

        return when (difficulty) {
            AiDifficulty.RECRUIT -> {
                // Acemi: Aktif isabet varsa komşularından birine ateş et, yoksa rastgele petek seç
                val activeHitNeighbors = findActiveHitNeighbors(state, unattackedCoords)
                if (activeHitNeighbors.isNotEmpty()) {
                    activeHitNeighbors.random(random)
                } else {
                    unattackedCoords.random(random)
                }
            }

            AiDifficulty.VETERAN, AiDifficulty.GRAND_ADMIRAL -> {
                // Kıdemli ve Büyük Amiral: 6 eksenli Olasılık Yoğunluk Haritasını (H-PDF) kullan
                val densityMap = HexProbabilityMap.calculateDensity(state, aiPlayer)
                val unattackedDensity = unattackedCoords.associateWith { densityMap[it] ?: 0 }

                val maxScore = unattackedDensity.values.maxOrNull() ?: 0
                val candidates = unattackedDensity.filter { it.value == maxScore }.keys

                if (candidates.isNotEmpty()) {
                    candidates.toList().random(random)
                } else {
                    unattackedCoords.random(random)
                }
            }
        }
    }

    /**
     * Büyük Amiral için yetenek ve kaçış manevraları değerlendiricisi
     */
    private fun considerTacticalAbilitiesAndEvasion(state: GameState): GameState {
        var currentState = state
        val aiShips = currentState.getPlayerShips(aiPlayer).filter { !it.isSunk }

        // 1. İHA Gemisi Yeteneği: CP >= 3 ve bekleme süresi 0 ise en yoğun bölgeyi tara
        val droneCarrier = aiShips.firstOrNull { it.shipClass == ShipClass.DRONE_CARRIER && it.hasAbilityReady() }
        if (droneCarrier != null && currentState.getCp(aiPlayer) >= 3) {
            val densityMap = HexProbabilityMap.calculateDensity(currentState, aiPlayer)
            val bestScoutTarget = densityMap.filter { (coord, _) ->
                currentState.tiles[coord]?.isAttacked != true
            }.maxByOrNull { it.value }?.key

            if (bestScoutTarget != null) {
                val (nextState, result) = GameEngine.useAbility(currentState, droneCarrier.id, bestScoutTarget)
                if (result !is GameActionResult.Ability.Failed) {
                    currentState = nextState
                }
            }
        }

        // 2. Aegis Muhribi: Hasar almış dost gemi varsa kalkan örter
        val aegis = aiShips.firstOrNull { it.shipClass == ShipClass.AEGIS_DESTROYER && it.hasAbilityReady() }
        if (aegis != null && currentState.getCp(aiPlayer) >= 1) {
            val damagedShip = aiShips.firstOrNull { it.currentHp < it.shipClass.maxHp && !it.isShielded }
            if (damagedShip != null) {
                val targetCoord = damagedShip.segments.first().coordinate
                val (nextState, result) = GameEngine.useAbility(currentState, aegis.id, targetCoord)
                if (result !is GameActionResult.Ability.Failed) {
                    currentState = nextState
                }
            }
        }

        return currentState
    }

    private fun findActiveHitNeighbors(
        state: GameState,
        unattackedCoords: List<HexCoordinate>
    ): List<HexCoordinate> {
        val opponent = state.getOpponent(aiPlayer)
        val opponentShips = state.getPlayerShips(opponent)
        val hitCoords = opponentShips.flatMap { ship ->
            if (!ship.isSunk) ship.segments.filter { it.isDamaged }.map { it.coordinate } else emptyList()
        }

        return hitCoords.flatMap { hit ->
            hit.allNeighbors().filter { it in unattackedCoords }
        }.distinct()
    }
}

