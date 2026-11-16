package com.hexafleet.domain.game.model

import com.hexafleet.core.hexmath.HexCoordinate
import kotlinx.serialization.Serializable

@Serializable
sealed class GameActionResult {
    @Serializable
    sealed class Fire : GameActionResult() {
        @Serializable data class Hit(val shipId: String, val wasSunk: Boolean) : Fire()
        @Serializable data object Miss : Fire()
        @Serializable data class ShieldAbsorbed(val shipId: String) : Fire()
        @Serializable data class MineExploded(val coordinate: HexCoordinate) : Fire()
    }

    @Serializable
    sealed class Move : GameActionResult() {
        @Serializable data class Success(val shipId: String, val newCoordinates: List<HexCoordinate>) : Move()
        @Serializable data class Failed(val reason: String) : Move()
    }

    @Serializable
    sealed class Rotate : GameActionResult() {
        @Serializable data class Success(val shipId: String, val newCoordinates: List<HexCoordinate>) : Rotate()
        @Serializable data class Failed(val reason: String) : Rotate()
    }

    @Serializable
    sealed class Ability : GameActionResult() {
        @Serializable data class DroneScouted(val center: HexCoordinate, val revealedCoordinates: Set<HexCoordinate>) : Ability()
        @Serializable data class EmpApplied(val lockedShipIds: List<String>) : Ability()
        @Serializable data class ShieldActivated(val targetShipId: String) : Ability()
        @Serializable data class TorpedoImpact(val hitCoordinate: HexCoordinate, val hitShipId: String?, val wasSunk: Boolean) : Ability()
        @Serializable data class TrapDeployed(val coordinate: HexCoordinate, val isMine: Boolean) : Ability()
        @Serializable data class Failed(val reason: String) : Ability()
    }
}

