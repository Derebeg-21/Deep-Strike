package com.hexafleet.core.hexmath

/**
 * Işın gönderme (Raycast) sonucu.
 *
 * @property path Işının kat ettiği altıgen petek dizisi (başlangıç dahil)
 * @property hitCoordinate Işının bir engele veya hedefe çarptığı son petek (hiçbir şeye çarpmadıysa null)
 * @property isBlocked Yol üzerinde görüşü veya torpidoyu kesen bir engel olup olmadığı
 */
data class HexRaycastResult(
    val path: List<HexCoordinate>,
    val hitCoordinate: HexCoordinate?,
    val isBlocked: Boolean
)

object HexLineOfSight {

    /**
     * [start] ile [end] petekleri arasında görüş hattının açık olup olmadığını denetler.
     *
     * @param start Başlangıç peteği
     * @param end Hedef petek
     * @param isObstacle Verilen peteğin görüşü kesip kesmediğini bildiren lambda (örneğin resif veya ada)
     * @return Arada görüşü kesen engel yoksa true, varsa false döner.
     */
    fun isClear(
        start: HexCoordinate,
        end: HexCoordinate,
        isObstacle: (HexCoordinate) -> Boolean
    ): Boolean {
        val line = start.lineTo(end)
        // Başlangıç ve bitiş hariç ara petekleri denetle
        for (i in 1 until line.size - 1) {
            if (isObstacle(line[i])) {
                return false
            }
        }
        return true
    }

    /**
     * [start] noktasından verilen [directionIndex] (0..5) yönünde [maxDistance] mesafeye kadar torpido/sinyal ışını gönderir.
     * Bir engele rastlarsa ışın durur.
     */
    fun raycast(
        start: HexCoordinate,
        directionIndex: Int,
        maxDistance: Int,
        isObstacle: (HexCoordinate) -> Boolean
    ): HexRaycastResult {
        val path = mutableListOf<HexCoordinate>()
        path.add(start)
        var current = start
        val dir = HexCoordinate.DIRECTIONS[Math.floorMod(directionIndex, 6)]

        for (step in 1..maxDistance) {
            current += dir
            path.add(current)
            if (isObstacle(current)) {
                return HexRaycastResult(
                    path = path,
                    hitCoordinate = current,
                    isBlocked = true
                )
            }
        }

        return HexRaycastResult(
            path = path,
            hitCoordinate = null,
            isBlocked = false
        )
    }
}

