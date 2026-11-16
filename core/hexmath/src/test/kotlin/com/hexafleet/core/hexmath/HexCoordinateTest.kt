package com.hexafleet.core.hexmath

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HexCoordinateTest {

    @Test
    fun `cube coordinate property holds x plus y plus z equals zero`() {
        val coords = listOf(
            HexCoordinate(0, 0),
            HexCoordinate(3, -5),
            HexCoordinate(-4, 7),
            HexCoordinate(12, -12)
        )

        for (c in coords) {
            assertThat(c.x + c.y + c.z).isEqualTo(0)
        }
    }

    @Test
    fun `distance to self is zero and distance to neighbor is one`() {
        val origin = HexCoordinate.ZERO
        assertThat(origin.distanceTo(origin)).isEqualTo(0)

        for (dir in 0 until 6) {
            val neighbor = origin.neighbor(dir)
            assertThat(origin.distanceTo(neighbor)).isEqualTo(1)
            assertThat(neighbor.distanceTo(origin)).isEqualTo(1)
        }
    }

    @Test
    fun `distance satisfies triangle inequality`() {
        val a = HexCoordinate(2, -4)
        val b = HexCoordinate(-3, 1)
        val c = HexCoordinate(5, 2)

        assertThat(a.distanceTo(c)).isAtMost(a.distanceTo(b) + b.distanceTo(c))
    }

    @Test
    fun `neighbor wrapping handles modulo and negative indices`() {
        val origin = HexCoordinate.ZERO
        assertThat(origin.neighbor(6)).isEqualTo(origin.neighbor(0))
        assertThat(origin.neighbor(12)).isEqualTo(origin.neighbor(0))
        assertThat(origin.neighbor(-1)).isEqualTo(origin.neighbor(5))
        assertThat(origin.neighbor(-6)).isEqualTo(origin.neighbor(0))
    }

    @Test
    fun `all neighbors returns exactly 6 distinct adjacent hexes`() {
        val center = HexCoordinate(3, -2)
        val neighbors = center.allNeighbors()

        assertThat(neighbors).hasSize(6)
        assertThat(neighbors.toSet()).hasSize(6)
        neighbors.forEach { n ->
            assertThat(center.distanceTo(n)).isEqualTo(1)
        }
    }

    @Test
    fun `ring generates exact hex count and equidistant elements`() {
        val center = HexCoordinate(1, 2)

        // radius 0
        assertThat(center.ring(0)).containsExactly(center)

        // radius 1: 6 hexes
        val ring1 = center.ring(1)
        assertThat(ring1).hasSize(6)
        assertThat(ring1.toSet()).hasSize(6)
        ring1.forEach { assertThat(center.distanceTo(it)).isEqualTo(1) }

        // radius 2: 12 hexes
        val ring2 = center.ring(2)
        assertThat(ring2).hasSize(12)
        assertThat(ring2.toSet()).hasSize(12)
        ring2.forEach { assertThat(center.distanceTo(it)).isEqualTo(2) }

        // radius 3: 18 hexes
        val ring3 = center.ring(3)
        assertThat(ring3).hasSize(18)
        assertThat(ring3.toSet()).hasSize(18)
        ring3.forEach { assertThat(center.distanceTo(it)).isEqualTo(3) }
    }

    @Test
    fun `area generates complete spiral circle matching 1 plus 3r(r plus 1)`() {
        val center = HexCoordinate(-2, 3)

        assertThat(center.area(0)).hasSize(1)
        assertThat(center.area(1)).hasSize(7)
        assertThat(center.area(2)).hasSize(19)
        assertThat(center.area(3)).hasSize(37)

        val area3 = center.area(3)
        assertThat(area3.toSet()).hasSize(37)
        area3.forEach { assertThat(center.distanceTo(it)).isAtMost(3) }
    }

    @Test
    fun `lineTo produces unbroken straight line from start to finish`() {
        val start = HexCoordinate(0, 0)
        val end = HexCoordinate(4, -2)

        val line = start.lineTo(end)
        val expectedDist = start.distanceTo(end)

        assertThat(line).hasSize(expectedDist + 1)
        assertThat(line.first()).isEqualTo(start)
        assertThat(line.last()).isEqualTo(end)

        // Ardışık her iki petek komşu olmalıdır (kopukluk olamaz)
        for (i in 0 until line.size - 1) {
            assertThat(line[i].distanceTo(line[i + 1])).isEqualTo(1)
        }
    }

    @Test
    fun `rotate60 by six steps returns to original coordinate`() {
        val target = HexCoordinate(3, -1)
        val center = HexCoordinate(1, 1)

        val rotated6 = target.rotate60(center, 6)
        assertThat(rotated6).isEqualTo(target)

        val rotated12 = target.rotate60(center, 12)
        assertThat(rotated12).isEqualTo(target)
    }

    @Test
    fun `rotate60 preserves distance from center`() {
        val target = HexCoordinate(5, -2)
        val center = HexCoordinate(1, 0)
        val initialDist = center.distanceTo(target)

        for (step in 1..6) {
            val rotated = target.rotate60(center, step)
            assertThat(center.distanceTo(rotated)).isEqualTo(initialDist)
        }
    }
}

