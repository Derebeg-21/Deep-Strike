package com.hexafleet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaign_progress")
data class CampaignProgressEntity(
    @PrimaryKey
    val id: Int = 1,
    val stage: Int,
    val currentNodeId: String,
    val credits: Int,
    val unlockedShipsJson: String,
    val activeFleetJson: String,
    val nodesJson: String,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

