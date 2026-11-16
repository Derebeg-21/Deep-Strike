package com.hexafleet.domain.game.multiplayer

import com.hexafleet.domain.game.model.Ship
import java.security.MessageDigest

/**
 * Sıfır Bilgi Savaş Sisi (Zero-Knowledge Fog) ve Kriptografik Doğrulama Protokolü.
 *
 * Rakip oyuncunun istemci belleğine (RAM) gizli gemi koordinatları gönderilmez.
 * Oyun başında iki taraf da filo diziliminin SHA-256 özetini (Commitment Hash) değiş tokuş eder.
 * Oyun bitiminde taraflar tuz (Salt) anahtarlarını açarak hile denetimi yapar.
 */
object ZeroKnowledgeFog {

    /**
     * Filo dizilimi ve gizli tuz (salt) anahtarından kriptografik SHA-256 özeti üretir.
     */
    fun createFleetCommitment(ships: List<Ship>, secretSalt: String): String {
        // Gemileri ID ve koordinatlarına göre deterministik olarak sırala
        val serializedFleet = ships.sortedBy { it.id }.joinToString(separator = ";") { ship ->
            val sortedCoords = ship.segments.map { it.coordinate }.sortedWith(compareBy({ it.q }, { it.r }))
            "${ship.id}:${ship.shipClass.name}:" + sortedCoords.joinToString(",") { "${it.q}_${it.r}" }
        }
        val payload = "$serializedFleet|$secretSalt"
        return sha256(payload)
    }

    /**
     * Maç sonunda sunulan filonun oyun başında teyit edilen hash ile uyuştuğunu doğrular.
     */
    fun verifyFleetCommitment(
        claimedShips: List<Ship>,
        secretSalt: String,
        expectedHash: String
    ): Boolean {
        val calculated = createFleetCommitment(claimedShips, secretSalt)
        return calculated.equals(expectedHash, ignoreCase = true)
    }

    /**
     * Lobi için 4 haneli PIN şifresini hash'ler.
     */
    fun hashPin(pin: String, roomSalt: String): String {
        require(pin.length == 4 && pin.all { it.isDigit() }) { "PIN tam 4 haneli sayı olmalıdır." }
        return sha256("$pin|$roomSalt")
    }

    fun verifyPin(inputPin: String, roomSalt: String, expectedPinHash: String): Boolean {
        if (inputPin.length != 4 || !inputPin.all { it.isDigit() }) return false
        val hashed = hashPin(inputPin, roomSalt)
        return hashed.equals(expectedPinHash, ignoreCase = true)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

