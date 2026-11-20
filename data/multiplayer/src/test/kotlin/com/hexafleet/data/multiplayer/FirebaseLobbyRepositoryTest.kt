package com.hexafleet.data.multiplayer

import com.google.common.truth.Truth.assertThat
import com.hexafleet.domain.game.multiplayer.RoomStatus
import org.junit.Before
import org.junit.Test

class FirebaseLobbyRepositoryTest {

    private lateinit var repository: FirebaseLobbyRepository

    @Before
    fun setup() {
        repository = FirebaseLobbyRepository()
    }

    @Test
    fun `create public room generates code and initializes in waiting state`() {
        val result = repository.createRoom(hostPlayerId = "host_123")

        assertThat(result.isSuccess).isTrue()
        val room = result.getOrThrow()
        assertThat(room.roomCode).startsWith("HX-")
        assertThat(room.isPinProtected).isFalse()
        assertThat(room.status).isEqualTo(RoomStatus.WAITING_FOR_OPPONENT)
    }

    @Test
    fun `join pin-protected room succeeds with valid pin`() {
        val createResult = repository.createRoom(hostPlayerId = "host_123", pin = "1234")
        val roomCode = createResult.getOrThrow().roomCode

        val joinResult = repository.joinRoom(roomCode, guestPlayerId = "guest_456", inputPin = "1234")

        assertThat(joinResult.isSuccess).isTrue()
        val updatedRoom = joinResult.getOrThrow()
        assertThat(updatedRoom.guestPlayerId).isEqualTo("guest_456")
        assertThat(updatedRoom.status).isEqualTo(RoomStatus.FLEET_SETUP)
    }

    @Test
    fun `join pin-protected room fails with invalid pin`() {
        val createResult = repository.createRoom(hostPlayerId = "host_123", pin = "9999")
        val roomCode = createResult.getOrThrow().roomCode

        val joinResult = repository.joinRoom(roomCode, guestPlayerId = "guest_456", inputPin = "0000")

        assertThat(joinResult.isFailure).isTrue()
        assertThat(joinResult.exceptionOrNull()).isInstanceOf(SecurityException::class.java)
    }

    @Test
    fun `committing fleet hash from both players starts the game`() {
        val createResult = repository.createRoom(hostPlayerId = "host_123")
        val roomCode = createResult.getOrThrow().roomCode
        repository.joinRoom(roomCode, guestPlayerId = "guest_456")

        repository.commitFleetHash(roomCode, "host_123", "hash_alpha_1")
        val finalResult = repository.commitFleetHash(roomCode, "guest_456", "hash_beta_2")

        val finalRoom = finalResult.getOrThrow()
        assertThat(finalRoom.status).isEqualTo(RoomStatus.IN_GAME)
    }

    @Test
    fun `forfeitMatch marks room as forfeited and records disconnected player`() {
        val createResult = repository.createRoom(hostPlayerId = "host_123")
        val roomCode = createResult.getOrThrow().roomCode
        repository.joinRoom(roomCode, guestPlayerId = "guest_456")

        val forfeitResult = repository.forfeitMatch(roomCode, forfeitedPlayerId = "guest_456")
        assertThat(forfeitResult.isSuccess).isTrue()
        val forfeitedRoom = forfeitResult.getOrThrow()

        assertThat(forfeitedRoom.status).isEqualTo(RoomStatus.FORFEITED)
        assertThat(forfeitedRoom.forfeitedPlayerId).isEqualTo("guest_456")
    }

    @Test
    fun `sendHeartbeat updates lastHeartbeatTimestamp successfully`() {
        val createResult = repository.createRoom(hostPlayerId = "host_123")
        val roomCode = createResult.getOrThrow().roomCode

        val success = repository.sendHeartbeat(roomCode, "host_123")
        assertThat(success).isTrue()

        val invalidPlayer = repository.sendHeartbeat(roomCode, "unknown_player")
        assertThat(invalidPlayer).isFalse()
    }
}

