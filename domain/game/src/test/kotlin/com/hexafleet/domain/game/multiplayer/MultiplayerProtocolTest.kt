package com.hexafleet.domain.game.multiplayer

import com.google.common.truth.Truth.assertThat
import com.hexafleet.core.hexmath.HexCoordinate
import com.hexafleet.domain.game.model.PlayerId
import com.hexafleet.domain.game.model.Ship
import com.hexafleet.domain.game.model.ShipClass
import com.hexafleet.domain.game.model.ShipSegment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.random.Random

class MultiplayerProtocolTest {

    @Test
    fun `room code generator produces valid Base32 format with prefix`() {
        val random = Random(12345)
        for (i in 0 until 50) {
            val code = RoomCodeGenerator.generate(random)
            assertThat(code).startsWith("HX-")
            assertThat(code).hasLength(7) // "HX-XXXX"
            assertThat(RoomCodeGenerator.isValid(code)).isTrue()
            // Karışıklık yaratan harflerin olmadığını doğrula
            assertThat(code).doesNotContain("0")
            assertThat(code).doesNotContain("O")
            assertThat(code).doesNotContain("1")
            assertThat(code).doesNotContain("I")
        }
    }

    @Test
    fun `pin hashing and verification matches correct pin and rejects wrong pin`() {
        val salt = "test_salt_98765"
        val correctPin = "4812"
        val wrongPin = "4813"

        val hash = ZeroKnowledgeFog.hashPin(correctPin, salt)

        assertThat(ZeroKnowledgeFog.verifyPin(correctPin, salt, hash)).isTrue()
        assertThat(ZeroKnowledgeFog.verifyPin(wrongPin, salt, hash)).isFalse()
        assertThat(ZeroKnowledgeFog.verifyPin("123", salt, hash)).isFalse() // 3 hane geçersiz
    }

    @Test
    fun `zero knowledge fog produces matching hash and detects ship tampering`() {
        val secretSalt = "player_secret_key_443"

        val originalFleet = listOf(
            Ship(
                id = "ship_1",
                owner = PlayerId.BLUE,
                shipClass = ShipClass.AEGIS_DESTROYER,
                segments = listOf(ShipSegment(HexCoordinate(0, 0)), ShipSegment(HexCoordinate(1, 0)))
            ),
            Ship(
                id = "ship_2",
                owner = PlayerId.BLUE,
                shipClass = ShipClass.STEALTH_SUBMARINE,
                segments = listOf(ShipSegment(HexCoordinate(-2, 1)), ShipSegment(HexCoordinate(-1, 1)))
            )
        )

        val commitmentHash = ZeroKnowledgeFog.createFleetCommitment(originalFleet, secretSalt)
        assertThat(commitmentHash).isNotEmpty()

        // Doğru filo ve tuz ile teyit başarılı olmalı
        val isVerified = ZeroKnowledgeFog.verifyFleetCommitment(originalFleet, secretSalt, commitmentHash)
        assertThat(isVerified).isTrue()

        // Hile denemesi: Oyuncu gemisini gizlice 1 petek kaydırmış olsun
        val tamperedFleet = listOf(
            Ship(
                id = "ship_1",
                owner = PlayerId.BLUE,
                shipClass = ShipClass.AEGIS_DESTROYER,
                segments = listOf(ShipSegment(HexCoordinate(0, 1)), ShipSegment(HexCoordinate(1, 1))) // Değiştirildi!
            ),
            originalFleet[1]
        )

        val isTamperDetected = ZeroKnowledgeFog.verifyFleetCommitment(tamperedFleet, secretSalt, commitmentHash)
        assertThat(isTamperDetected).isFalse()
    }

    @Test
    fun `network payload serializes and deserializes cleanly via Json`() {
        val json = Json { ignoreUnknownKeys = true }

        val fireRequest: NetworkPayload = NetworkPayload.FireRequest(
            shooterPlayerId = "player_alpha",
            target = HexCoordinate(3, -2),
            turnNumber = 4
        )

        val encoded = json.encodeToString(fireRequest)
        val decoded = json.decodeFromString<NetworkPayload>(encoded)

        assertThat(decoded).isEqualTo(fireRequest)
    }
}

