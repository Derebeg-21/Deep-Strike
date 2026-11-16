package com.hexafleet.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hexafleet.data.local.entity.CampaignProgressEntity
import com.hexafleet.data.local.entity.PlayerStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaign_progress WHERE id = 1")
    fun observeProgress(): Flow<CampaignProgressEntity?>

    @Query("SELECT * FROM campaign_progress WHERE id = 1")
    suspend fun getProgress(): CampaignProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(entity: CampaignProgressEntity)

    @Query("DELETE FROM campaign_progress")
    suspend fun clearProgress()
}

@Dao
interface PlayerStatsDao {
    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun observeStats(): Flow<PlayerStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStats(entity: PlayerStatsEntity)
}

