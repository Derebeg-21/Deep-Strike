package com.hexafleet.domain.game.multiplayer

import kotlin.random.Random

/**
 * Karışıklığa yol açabilecek karakterlerden (0, O, 1, I, L) arındırılmış
 * 6 karakterli benzersiz Base32 oda kodu üreticisi (Örn: HX-7482).
 */
object RoomCodeGenerator {

    // Okunması kolay 30 karakterlik güvenli alfabe
    private const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    private const val PREFIX = "HX"

    /**
     * Format: "HX-XXXX" (Örn: "HX-8F92")
     */
    fun generate(random: Random = Random.Default): String {
        val codePart = StringBuilder(4)
        for (i in 0 until 4) {
            val randomIndex = random.nextInt(ALPHABET.length)
            codePart.append(ALPHABET[randomIndex])
        }
        return "$PREFIX-$codePart"
    }

    /**
     * Aktif odalar arasında asla mükerrer olmayacak benzersiz bir oda kodu üretir.
     */
    fun generateUnique(existingCodes: Set<String>, random: Random = Random.Default): String {
        var code: String
        var attempts = 0
        do {
            code = generate(random)
            attempts++
            if (attempts > 50) {
                // Aşırı yoğunluk durumunda çakışmayı önlemek için 5 karakterli varyanta geç
                val extra = ALPHABET[random.nextInt(ALPHABET.length)]
                code = "$code$extra"
            }
        } while (code in existingCodes)
        return code
    }

    /**
     * Kodun geçerli formatta olup olmadığını denetler.
     */
    fun isValid(code: String): Boolean {
        val normalized = code.trim().uppercase()
        if (!normalized.startsWith("$PREFIX-")) return false
        val suffix = normalized.removePrefix("$PREFIX-")
        if (suffix.length != 4) return false
        return suffix.all { it in ALPHABET }
    }
}

