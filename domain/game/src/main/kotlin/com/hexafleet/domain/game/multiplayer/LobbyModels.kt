package com.hexafleet.domain.game.multiplayer

import com.hexafleet.core.hexmath.HexCoordinate
import kotlinx.serialization.Serializable

@Serializable
enum class RoomStatus {
    WAITING_FOR_OPPONENT, // Lobi kuruldu, rakip bekleniyor
    FLEET_SETUP,          // İki oyuncu da odada, filo dizilimi & hash değiş tokuşu yapılıyor
    IN_GAME,              // Maç devam ediyor
    FINISHED,             // Maç bitti, audit logları açıldı
    FORFEITED             // Rakip maçı terk etti veya kalp atışı (heartbeat) zaman aşımına uğradı
}

@Serializable
data class LobbyRoom(
    val roomCode: String,
    val hostPlayerId: String,
    val guestPlayerId: String? = null,
    val pinHash: String? = null,
    val roomSalt: String,
    val turnDurationSeconds: Int = 45, // 45s Standart, 10s Blitz
    val mapRadius: Int = 5,
    val enableHazards: Boolean = true,
    val isPrivate: Boolean = false,
    val status: RoomStatus = RoomStatus.WAITING_FOR_OPPONENT,
    val hostCommitmentHash: String? = null,
    val guestCommitmentHash: String? = null,
    val currentTurnPlayerId: String = hostPlayerId,
    val turnNumber: Int = 1,
    val lastHeartbeatTimestamp: Long = System.currentTimeMillis(),
    val forfeitedPlayerId: String? = null
) {
    val isPinProtected: Boolean get() = pinHash != null
    val isFull: Boolean get() = guestPlayerId != null
    val isReadyToStart: Boolean get() = hostCommitmentHash != null && guestCommitmentHash != null
}

@Serializable
sealed class NetworkPayload {
    @Serializable
    data class FireRequest(
        val shooterPlayerId: String,
        val target: HexCoordinate,
        val turnNumber: Int
    ) : NetworkPayload()

    @Serializable
    data class FireResponse(
        val target: HexCoordinate,
        val isHit: Boolean,
        val isSunk: Boolean,
        val hitShipId: String?,
        val wasShielded: Boolean = false
    ) : NetworkPayload()

    @Serializable
    data class AbilityRequest(
        val playerPlayerId: String,
        val shipId: String,
        val target: HexCoordinate
    ) : NetworkPayload()

    @Serializable
    data class EndTurn(
        val nextPlayerId: String,
        val nextTurnNumber: Int
    ) : NetworkPayload()

    @Serializable
    data class Heartbeat(
        val senderPlayerId: String,
        val timestamp: Long
    ) : NetworkPayload()

    @Serializable
    data class RevealSaltAndAudit(
        val playerId: String,
        val secretSalt: String
    ) : NetworkPayload()
}

