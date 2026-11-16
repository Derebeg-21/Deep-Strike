package com.hexafleet.core.hexmath

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 2D piksel düzlemindeki nokta koordinatı.
 */
data class HexPoint(
    val x: Float,
    val y: Float
)

/**
 * Eksenel altıgen koordinatları ile ekran pikselleri arasındaki izdüşüm ve ters izdüşüm motoru.
 *
 * @property size Altıgenin merkezinden köşelerine olan yarıçapı (piksel)
 * @property originX Izgara merkezinin veya sıfır noktasının ekrandaki X konumu
 * @property originY Izgara merkezinin veya sıfır noktasının ekrandaki Y konumu
 * @property orientation Sivri uçlu (POINTY) veya Düz tepeli (FLAT) altıgen yönelimi
 */
data class HexLayout(
    val size: Float,
    val originX: Float = 0f,
    val originY: Float = 0f,
    val orientation: HexOrientation = HexOrientation.POINTY
) {
    /**
     * Bir altıgenin merkez koordinatını ekran pikseline çevirir.
     */
    fun hexToPixel(hex: HexCoordinate): HexPoint {
        val m = orientation
        val px = (m.f0 * hex.q + m.f1 * hex.r) * size + originX
        val py = (m.f2 * hex.q + m.f3 * hex.r) * size + originY
        return HexPoint(px.toFloat(), py.toFloat())
    }

    /**
     * Ekranda dokunulan piksel noktasını en yakın altıgen koordinatına dönüştürür.
     */
    fun pixelToHex(px: Float, py: Float): HexCoordinate {
        val m = orientation
        val ptX = (px - originX) / size
        val ptY = (py - originY) / size

        val fracQ = m.b0 * ptX + m.b1 * ptY
        val fracR = m.b2 * ptX + m.b3 * ptY
        val fracY = -fracQ - fracR

        return HexCoordinate.roundFromFractional(fracQ, fracY, fracR)
    }

    /**
     * Verilen köşe indeksi (0..5) için merkezden köşeye olan piksel ofsetini hesaplar.
     */
    fun hexCornerOffset(corner: Int): HexPoint {
        val angle = 2.0 * PI * (orientation.startAngle + corner) / 6.0
        return HexPoint(
            x = (size * cos(angle)).toFloat(),
            y = (size * sin(angle)).toFloat()
        )
    }

    /**
     * Canvas üzerinde Path ile altıgen çizmek için 6 köşe noktasının mutlak piksellerini döner.
     */
    fun polygonCorners(hex: HexCoordinate): List<HexPoint> {
        val center = hexToPixel(hex)
        return (0 until 6).map { corner ->
            val offset = hexCornerOffset(corner)
            HexPoint(center.x + offset.x, center.y + offset.y)
        }
    }
}

