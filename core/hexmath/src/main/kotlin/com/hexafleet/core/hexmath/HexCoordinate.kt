package com.hexafleet.core.hexmath

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pointy-Topped altıgen ızgara için Eksenel (Axial) ve Kübik (Cube) koordinat veri sınıfı.
 *
 * Eksenel Koordinatlar: (q, r)
 * Kübik Koordinatlar: (x, y, z) bağıntısı -> x + y + z = 0
 *
 * @property q Eksenel sütun (Kübik x)
 * @property r Eksenel satır (Kübik z)
 */
@Serializable
data class HexCoordinate(
    val q: Int,
    val r: Int
) {
    val x: Int get() = q
    val z: Int get() = r
    val y: Int get() = -q - r

    init {
        require(x + y + z == 0) { "Kübik koordinat kuralı ihlali: x + y + z != 0" }
    }

    companion object {
        val ZERO = HexCoordinate(0, 0)

        /**
         * Pointy-Topped altıgenler için 6 temel yön vektörü (Saat yönünde):
         * 0: Doğu (E)
         * 1: Kuzeydoğu (NE)
         * 2: Kuzeybatı (NW)
         * 3: Batı (W)
         * 4: Güneybatı (SW)
         * 5: Güneydoğu (SE)
         */
        val DIRECTIONS = listOf(
            HexCoordinate(1, 0),   // 0: E
            HexCoordinate(1, -1),  // 1: NE
            HexCoordinate(0, -1),  // 2: NW
            HexCoordinate(-1, 0),  // 3: W
            HexCoordinate(-1, 1),  // 4: SW
            HexCoordinate(0, 1)    // 5: SE
        )

        /**
         * Kübik koordinatları (x, y, z) doğrudan alarak HexCoordinate oluşturur.
         */
        fun fromCube(x: Int, y: Int, z: Int): HexCoordinate {
            require(x + y + z == 0) { "Kübik koordinat kuralı (x + y + z == 0) sağlanmalıdır." }
            return HexCoordinate(x, z)
        }

        /**
         * Kesirli kübik koordinatları en yakın geçerli tamsayı altıgene yuvarlar.
         */
        fun roundFromFractional(fracX: Double, fracY: Double, fracZ: Double): HexCoordinate {
            var rx = fracX.roundToInt()
            var ry = fracY.roundToInt()
            var rz = fracZ.roundToInt()

            val xDiff = abs(rx - fracX)
            val yDiff = abs(ry - fracY)
            val zDiff = abs(rz - fracZ)

            if (xDiff > yDiff && xDiff > zDiff) {
                rx = -ry - rz
            } else if (yDiff > zDiff) {
                ry = -rx - rz
            } else {
                rz = -rx - ry
            }
            return HexCoordinate(rx, rz)
        }
    }

    operator fun plus(other: HexCoordinate): HexCoordinate =
        HexCoordinate(q + other.q, r + other.r)

    operator fun minus(other: HexCoordinate): HexCoordinate =
        HexCoordinate(q - other.q, r - other.r)

    operator fun times(scalar: Int): HexCoordinate =
        HexCoordinate(q * scalar, r * scalar)

    /**
     * Verilen yön indeksindeki (0..5) komşu peteği döner.
     */
    fun neighbor(directionIndex: Int): HexCoordinate {
        val dir = DIRECTIONS[Math.floorMod(directionIndex, 6)]
        return this + dir
    }

    /**
     * Bu altıgenin tüm 6 komşusunu döner.
     */
    fun allNeighbors(): List<HexCoordinate> {
        return DIRECTIONS.map { this + it }
    }

    /**
     * İki altıgen arasındaki Manhattan-Hex mesafesini hesaplar.
     */
    fun distanceTo(other: HexCoordinate): Int {
        return (abs(x - other.x) + abs(y - other.y) + abs(z - other.z)) / 2
    }

    /**
     * Merkezden tam [radius] uzaklığındaki altıgen halkasını (Ring) döner.
     * radius = 0 ise sadece merkezi döner.
     * radius > 0 ise tam 6 * radius adet petek döner.
     */
    fun ring(radius: Int): List<HexCoordinate> {
        if (radius <= 0) return listOf(this)
        val results = ArrayList<HexCoordinate>(6 * radius)
        
        // Başlangıç: 4. yönde (SW) radius kadar git
        var current = this + (DIRECTIONS[4] * radius)

        for (i in 0 until 6) {
            for (step in 0 until radius) {
                results.add(current)
                current = current.neighbor(i)
            }
        }
        return results
    }

    /**
     * Merkezden [radius] yarıçapı içindeki tüm altıgenleri (Spiral / Dolu Alan) döner.
     * Toplam petek sayısı: 1 + 3 * radius * (radius + 1)
     */
    fun area(radius: Int): List<HexCoordinate> {
        if (radius <= 0) return listOf(this)
        val expectedCapacity = 1 + 3 * radius * (radius + 1)
        val results = ArrayList<HexCoordinate>(expectedCapacity)
        
        for (dq in -radius..radius) {
            val minR = max(-radius, -dq - radius)
            val maxR = min(radius, -dq + radius)
            for (dr in minR..maxR) {
                results.add(HexCoordinate(q + dq, r + dr))
            }
        }
        return results
    }

    /**
     * Bu altıgenden hedef [target] altıgene doğrusal ışın (Line Raycasting) çizer.
     * Torpido rotaları ve görüş hattı hesaplamalarında kullanılır.
     */
    fun lineTo(target: HexCoordinate): List<HexCoordinate> {
        val n = distanceTo(target)
        if (n == 0) return listOf(this)

        val results = ArrayList<HexCoordinate>(n + 1)
        val stepSize = 1.0 / n

        for (i in 0..n) {
            val t = i * stepSize
            // Kübik doğrusal interpolasyon (Lerp)
            val fx = x + (target.x - x) * t
            val fy = y + (target.y - y) * t
            val fz = z + (target.z - z) * t
            results.add(roundFromFractional(fx, fy, fz))
        }
        return results
    }

    /**
     * Peteği bir merkez [center] etrafında saat yönünde 60 derecelik adımlarla döndürür.
     * [steps] pozitifse saat yönünde, negatifse saatin tersi yönünde döner.
     */
    fun rotate60(center: HexCoordinate = ZERO, steps: Int = 1): HexCoordinate {
        val normalizedSteps = Math.floorMod(steps, 6)
        if (normalizedSteps == 0) return this

        var relX = this.x - center.x
        var relY = this.y - center.y
        var relZ = this.z - center.z

        for (i in 0 until normalizedSteps) {
            // Saat yönünde 60° kübik dönüşüm kuralı: (x, y, z) -> (-z, -x, -y)
            val nextX = -relZ
            val nextY = -relX
            val nextZ = -relY
            relX = nextX
            relY = nextY
            relZ = nextZ
        }

        return fromCube(center.x + relX, center.y + relY, center.z + relZ)
    }
}

