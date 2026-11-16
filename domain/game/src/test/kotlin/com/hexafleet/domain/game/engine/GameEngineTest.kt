package com.hexafleet.domain.game.engine

import com.google.common.truth.Truth.assertThat
import com.hexafleet.core.hexmath.HexCoordinate
import com.hexafleet.domain.game.model.BoardTile
import com.hexafleet.domain.game.model.GameActionResult
import com.hexafleet.domain.game.model.PlayerId
import com.hexafleet.domain.game.model.Ship
import com.hexafleet.domain.game.model.ShipClass
import com.hexafleet.domain.game.model.ShipSegment
import com.hexafleet.domain.game.model.Terrain
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var baseState: GameState

    @Before
    fun setup() {
        // Mavi Filo: İHA Gemisi (-2, 0), (-1, 0), (0, 0)
        val blueCarrier = Ship(
            id = "blue_carrier",
            owner = PlayerId.BLUE,
            shipClass = ShipClass.DRONE_CARRIER,
            segments = listOf(
                ShipSegment(HexCoordinate(-2, 0)),
                ShipSegment(HexCoordinate(-1, 0)),
                ShipSegment(HexCoordinate(0, 0))
            )
        )

        // Kırmızı Filo: Aegis Muhribi (2, 0), (3, 0)
        val redDestroyer = Ship(
            id = "red_destroyer",
            owner = PlayerId.RED,
            shipClass = ShipClass.AEGIS_DESTROYER,
            segments = listOf(
                ShipSegment(HexCoordinate(2, 0)),
                ShipSegment(HexCoordinate(3, 0))
            )
        )

        baseState = GameState(
            radius = 6,
            ships = mapOf(
                PlayerId.BLUE to listOf(blueCarrier),
                PlayerId.RED to listOf(redDestroyer)
            ),
            activePlayer = PlayerId.BLUE,
            commandPoints = mapOf(PlayerId.BLUE to 3, PlayerId.RED to 3)
        )
    }

    @Test
    fun `fire deals damage and sinks ship on last segment`() {
        val target1 = HexCoordinate(2, 0)
        val (state1, result1) = GameEngine.fire(baseState, target1)

        assertThat(result1).isInstanceOf(GameActionResult.Fire.Hit::class.java)
        val hit1 = result1 as GameActionResult.Fire.Hit
        assertThat(hit1.wasSunk).isFalse()
        assertThat(state1.getCp(PlayerId.BLUE)).isEqualTo(2)

        // İkinci parçaya ateş et
        val target2 = HexCoordinate(3, 0)
        val (state2, result2) = GameEngine.fire(state1, target2)

        assertThat(result2).isInstanceOf(GameActionResult.Fire.Hit::class.java)
        val hit2 = result2 as GameActionResult.Fire.Hit
        assertThat(hit2.wasSunk).isTrue()
        assertThat(state2.winner).isEqualTo(PlayerId.BLUE)
    }

    @Test
    fun `fire on open water results in miss and consumes 1 CP`() {
        val waterTarget = HexCoordinate(0, 3)
        val (state, result) = GameEngine.fire(baseState, waterTarget)

        assertThat(result).isInstanceOf(GameActionResult.Fire.Miss::class.java)
        assertThat(state.getCp(PlayerId.BLUE)).isEqualTo(2)
        assertThat(state.tiles[waterTarget]?.isAttacked).isTrue()
    }

    @Test
    fun `aegis shield absorbs direct hit without taking hull damage`() {
        // Red destroyer kalkanlı olsun
        val redShip = baseState.getPlayerShips(PlayerId.RED).first()
        val shieldedRed = redShip.copy(isShielded = true)
        val stateWithShield = baseState.copy(
            ships = mapOf(
                PlayerId.BLUE to baseState.getPlayerShips(PlayerId.BLUE),
                PlayerId.RED to listOf(shieldedRed)
            )
        )

        val target = HexCoordinate(2, 0)
        val (stateAfterHit, result) = GameEngine.fire(stateWithShield, target)

        assertThat(result).isInstanceOf(GameActionResult.Fire.ShieldAbsorbed::class.java)
        val targetShip = stateAfterHit.getPlayerShips(PlayerId.RED).first()
        assertThat(targetShip.isShielded).isFalse()
        assertThat(targetShip.segments.all { !it.isDamaged }).isTrue()
    }

    @Test
    fun `move ship one hex updates coordinates and costs 1 CP`() {
        // Doğu (yön 0: +1, 0) yönünde 1 petek kaydır
        val (state, result) = GameEngine.moveShip(baseState, "blue_carrier", directionIndex = 0)

        assertThat(result).isInstanceOf(GameActionResult.Move.Success::class.java)
        assertThat(state.getCp(PlayerId.BLUE)).isEqualTo(2)

        val movedShip = state.getPlayerShips(PlayerId.BLUE).first { it.id == "blue_carrier" }
        assertThat(movedShip.coordinates).containsExactly(
            HexCoordinate(-1, 0),
            HexCoordinate(0, 0),
            HexCoordinate(1, 0)
        ).inOrder()
    }

    @Test
    fun `move ship fails when blocked by coral reef`() {
        val reefTile = HexCoordinate(1, 0)
        val stateWithReef = baseState.copy(
            tiles = mapOf(reefTile to BoardTile(coordinate = reefTile, terrain = Terrain.CoralReef))
        )

        val (state, result) = GameEngine.moveShip(stateWithReef, "blue_carrier", directionIndex = 0)
        assertThat(result).isInstanceOf(GameActionResult.Move.Failed::class.java)
        assertThat(state.getCp(PlayerId.BLUE)).isEqualTo(3) // CP harcanmaz
    }

    @Test
    fun `drone carrier ability reveals 7 hex area and sets cooldown`() {
        val scanCenter = HexCoordinate(2, 0)
        val (state, result) = GameEngine.useAbility(baseState, "blue_carrier", scanCenter)

        assertThat(result).isInstanceOf(GameActionResult.Ability.DroneScouted::class.java)
        val scoutResult = result as GameActionResult.Ability.DroneScouted
        assertThat(scoutResult.revealedCoordinates).hasSize(7)
        assertThat(state.getCp(PlayerId.BLUE)).isEqualTo(0) // 3 CP harcandı (User Requirement 3)

        val carrier = state.getPlayerShips(PlayerId.BLUE).first { it.id == "blue_carrier" }
        assertThat(carrier.abilityCooldown).isEqualTo(ShipClass.DRONE_CARRIER.cooldownRounds)
    }

    @Test
    fun `endTurn drifts ship stationed on ocean current for 0 CP`() {
        val currentCoord = HexCoordinate(-2, 0)
        // Doğuya akan akıntı (yön 0: +1, 0)
        val stateWithCurrent = baseState.copy(
            tiles = mapOf(currentCoord to BoardTile(coordinate = currentCoord, terrain = Terrain.Current(0)))
        )

        val nextState = GameEngine.endTurn(stateWithCurrent)

        val driftedShip = nextState.getPlayerShips(PlayerId.BLUE).first { it.id == "blue_carrier" }
        assertThat(driftedShip.segments.first().coordinate).isEqualTo(HexCoordinate(-1, 0))
    }

    @Test
    fun `endTurn caps replenished command points at 4`() {
        // Red oyuncunun 2 CP'si varken tur bittiğinde +3 eklenir ama 4'te sınırlandırılır
        val stateWith2Cp = baseState.copy(
            commandPoints = mapOf(PlayerId.BLUE to 1, PlayerId.RED to 2)
        )

        val nextState = GameEngine.endTurn(stateWith2Cp)
        assertThat(nextState.activePlayer).isEqualTo(PlayerId.RED)
        assertThat(nextState.getCp(PlayerId.RED)).isEqualTo(4) // 2 + 3 = 5 değil, 4 (Capped)
    }
}

