package com.hexafleet.core.hexmath

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HexLineOfSightTest {

    @Test
    fun `isClear returns true when no obstacles between points`() {
        val start = HexCoordinate(0, 0)
        val end = HexCoordinate(3, 0)

        val isClear = HexLineOfSight.isClear(start, end) { _ -> false }
        assertThat(isClear).isTrue()
    }

    @Test
    fun `isClear returns false when reef or obstacle is in path`() {
        val start = HexCoordinate(0, 0)
        val end = HexCoordinate(4, 0)
        val obstacle = HexCoordinate(2, 0)

        val isClear = HexLineOfSight.isClear(start, end) { hex -> hex == obstacle }
        assertThat(isClear).isFalse()
    }

    @Test
    fun `raycast stops exactly at first encountered obstacle`() {
        val start = HexCoordinate(0, 0)
        // Direction 0 is East (1, 0) -> (1,0), (2,0), (3,0)
        val obstacle = HexCoordinate(2, 0)

        val result = HexLineOfSight.raycast(
            start = start,
            directionIndex = 0,
            maxDistance = 5,
            isObstacle = { it == obstacle }
        )

        assertThat(result.isBlocked).isTrue()
        assertThat(result.hitCoordinate).isEqualTo(obstacle)
        assertThat(result.path).containsExactly(
            HexCoordinate(0, 0),
            HexCoordinate(1, 0),
            HexCoordinate(2, 0)
        ).inOrder()
    }
}

