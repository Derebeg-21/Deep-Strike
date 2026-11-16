package com.hexafleet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val shotsFired: Int = 0,
    val hitsScored: Int = 0,
    val shipsSunk: Int = 0
)

