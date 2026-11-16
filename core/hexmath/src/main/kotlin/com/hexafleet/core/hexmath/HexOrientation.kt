package com.hexafleet.core.hexmath

import kotlin.math.sqrt

/**
 * Altıgen yönelim matrisleri (Pointy-Topped veya Flat-Topped).
 */
data class HexOrientation(
    val f0: Double, val f1: Double, val f2: Double, val f3: Double,
    val b0: Double, val b1: Double, val b2: Double, val b3: Double,
    val startAngle: Double // Radyan cinsinden köşe başlangıç açısı
) {
    companion object {
        private val SQRT_3 = sqrt(3.0)

        /**
         * Sivri uçlu (Pointy-Topped) altıgen. Mobil dikey ekranlar için varsayılandır.
         */
        val POINTY = HexOrientation(
            f0 = SQRT_3, f1 = SQRT_3 / 2.0, f2 = 0.0, f3 = 3.0 / 2.0,
            b0 = SQRT_3 / 3.0, b1 = -1.0 / 3.0, b2 = 0.0, b3 = 2.0 / 3.0,
            startAngle = 0.5 // 30 derece
        )

        /**
         * Düz tepeli (Flat-Topped) altıgen. Yatay geniş ekranlar ve tabletler için opsiyoneldir.
         */
        val FLAT = HexOrientation(
            f0 = 3.0 / 2.0, f1 = 0.0, f2 = SQRT_3 / 2.0, f3 = SQRT_3,
            b0 = 2.0 / 3.0, b1 = 0.0, b2 = -1.0 / 3.0, b3 = SQRT_3 / 3.0,
            startAngle = 0.0 // 0 derece
        )
    }
}

