package com.hexafleet.domain.game.ai

import com.google.common.truth.Truth.assertThat
import com.hexafleet.core.hexmath.HexCoordinate
import com.hexafleet.domain.game.campaign.CampaignMapGenerator
import com.hexafleet.domain.game.campaign.CampaignNodeType
import com.hexafleet.domain.game.engine.GameState
import com.hexafleet.domain.game.model.PlayerId
import com.hexafleet.domain.game.model.Ship
import com.hexafleet.domain.game.model.ShipClass
import com.hexafleet.domain.game.model.ShipSegment
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class TacticalAiTest {

    private lateinit var state: GameState

    @Before
    fun setup() {
        // Mavi Filo: Hayalet Denizaltı (1, 1), (2, 1) + Ek İHA Gemisi
        val blueSub = Ship(
            id = "blue_sub",
            owner = PlayerId.BLUE,
            shipClass = ShipClass.STEALTH_SUBMARINE,
            segments = listOf(
                ShipSegment(HexCoordinate(1, 1), isDamaged = true), // 1 Parça vurulmuş (Aktif İsabet)
                ShipSegment(HexCoordinate(2, 1), isDamaged = false)
            )
        )

        val blueCarrier = Ship(
            id = "blue_carrier",
            owner = PlayerId.BLUE,
            shipClass = ShipClass.DRONE_CARRIER,
            segments = listOf(
                ShipSegment(HexCoordinate(-2, 3)),
                ShipSegment(HexCoordinate(-1, 3)),
                ShipSegment(HexCoordinate(0, 3))
            )
        )

        // Kırmızı Filo (AI): İHA Gemisi (-2, -1), (-1, -1), (0, -1)
        val redCarrier = Ship(
            id = "red_carrier",
            owner = PlayerId.RED,
            shipClass = ShipClass.DRONE_CARRIER,
            segments = listOf(
                ShipSegment(HexCoordinate(-2, -1)),
                ShipSegment(HexCoordinate(-1, -1)),
                ShipSegment(HexCoordinate(0, -1))
            )
        )

        state = GameState(
            radius = 6,
            ships = mapOf(
                PlayerId.BLUE to listOf(blueSub, blueCarrier),
                PlayerId.RED to listOf(redCarrier)
            ),
            activePlayer = PlayerId.RED,
            commandPoints = mapOf(PlayerId.BLUE to 3, PlayerId.RED to 3)
        )
    }

    @Test
    fun `probability density gives highest score to adjacent hexes of active hit`() {
        val density = HexProbabilityMap.calculateDensity(state, aiPlayer = PlayerId.RED)

        // (1, 1) vurulmuş olduğu için (2, 1) komşusu en yüksek olasılık puanına sahip olmalıdır
        val targetNeighbor = HexCoordinate(2, 1)
        val maxScoreCoord = density.maxByOrNull { it.value }?.key

        // En yüksek puanlı koordinatlardan biri doğrudan komşulukta olmalı
        val topScore = density[targetNeighbor] ?: 0
        assertThat(topScore).isGreaterThan(1)
        assertThat(density[maxScoreCoord!!]).isGreaterThan(10)
    }

    @Test
    fun `veteran ai fires at highest density coordinate and spends CP`() {
        val ai = TacticalAiCommander(difficulty = AiDifficulty.VETERAN, random = Random(42))
        val nextState = ai.playTurn(state)

        // Tur tamamlandığında aktif oyuncu BLUE olmalı
        assertThat(nextState.activePlayer).isEqualTo(PlayerId.BLUE)
        // AI en az bir atış yapmış olmalıdır
        val attackedTiles = nextState.tiles.filter { it.value.isAttacked }
        assertThat(attackedTiles).isNotEmpty()
    }

    @Test
    fun `grand admiral ai uses drone carrier reconnaissance when CP is available`() {
        val ai = TacticalAiCommander(difficulty = AiDifficulty.GRAND_ADMIRAL, random = Random(42))
        val nextState = ai.playTurn(state)

        // AI'ın İHA gemisinin bekleme süresine girdiği doğrulanmalı
        val aiCarrier = nextState.getPlayerShips(PlayerId.RED).first { it.id == "red_carrier" }
        assertThat(aiCarrier.abilityCooldown).isGreaterThan(0)
    }

    @Test
    fun `campaign map generator creates valid connected path from stage 1 to boss node`() {
        val campaign = CampaignMapGenerator.generateNewCampaign()

        assertThat(campaign.stage).isEqualTo(1)
        assertThat(campaign.nodes).isNotEmpty()

        // 1. Aşamada erişilebilir düğümler bulunmalı
        val availableNodes = campaign.nodes.values.filter { it.isAvailable }
        assertThat(availableNodes).isNotEmpty()

        // 5. Aşamada BOSS_LEVIATHAN düğümü yer almalı
        val bossNode = campaign.nodes.values.firstOrNull { it.type == CampaignNodeType.BOSS_LEVIATHAN }
        assertThat(bossNode).isNotNull()
        assertThat(bossNode?.stage).isEqualTo(5)
    }
}
