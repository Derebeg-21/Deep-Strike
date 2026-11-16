package com.hexafleet.core.hexmath

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.hypot

class HexLayoutTest {

    private val layout = HexLayout(
        size = 40f,
        originX = 500f,
        originY = 500f,
        orientation = HexOrientation.POINTY
    )

    @Test
    fun `pixelToHex correctly round-trips hexToPixel for various coordinates`() {
        val testCoordinates = listOf(
            HexCoordinate(0, 0),
            HexCoordinate(1, 0),
            HexCoordinate(-1, 0),
            HexCoordinate(0, 1),
            HexCoordinate(0, -1),
            HexCoordinate(4, -3),
            HexCoordinate(-5, 2),
            HexCoordinate(10, -10),
            HexCoordinate(-7, -3)
        )

        for (coord in testCoordinates) {
            val point = layout.hexToPixel(coord)
            val roundTripHex = layout.pixelToHex(point.x, point.y)
            assertThat(roundTripHex).isEqualTo(coord)
        }
    }

    @Test
    fun `polygonCorners produces six vertices with distance equal to size`() {
        val hex = HexCoordinate(2, -1)
        val center = layout.hexToPixel(hex)
        val corners = layout.polygonCorners(hex)

        assertThat(corners).hasSize(6)

        for (corner in corners) {
            val distanceToCenter = hypot(corner.x - center.x, corner.y - center.y)
            assertThat(distanceToCenter).isWithin(0.01f).of(layout.size)
        }
    }
}

