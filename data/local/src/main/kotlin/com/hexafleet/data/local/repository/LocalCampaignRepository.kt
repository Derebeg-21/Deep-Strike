package com.hexafleet.data.local.repository

import com.hexafleet.data.local.dao.CampaignDao
import com.hexafleet.data.local.entity.CampaignProgressEntity
import com.hexafleet.domain.game.campaign.CampaignMapGenerator
import com.hexafleet.domain.game.campaign.CampaignProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalCampaignRepository(
    private val campaignDao: CampaignDao,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = false }
) {
    /**
     * Sefer ilerlemesini reactive Flow olarak gözlemler.
     * Kayıt yoksa yeni bir sefer başlatıp döner.
     */
    fun observeProgress(): Flow<CampaignProgress> {
        return campaignDao.observeProgress().map { entity ->
            if (entity != null) {
                mapEntityToDomain(entity)
            } else {
                CampaignMapGenerator.generateNewCampaign()
            }
        }
    }

    suspend fun getProgress(): CampaignProgress {
        val entity = campaignDao.getProgress()
        return if (entity != null) {
            mapEntityToDomain(entity)
        } else {
            val fresh = CampaignMapGenerator.generateNewCampaign()
            saveProgress(fresh)
            fresh
        }
    }

    suspend fun saveProgress(progress: CampaignProgress) {
        val entity = CampaignProgressEntity(
            id = 1,
            stage = progress.stage,
            currentNodeId = progress.currentNodeId,
            credits = progress.credits,
            unlockedShipsJson = json.encodeToString(progress.unlockedShips),
            activeFleetJson = json.encodeToString(progress.activeFleet),
            nodesJson = json.encodeToString(progress.nodes)
        )
        campaignDao.saveProgress(entity)
    }

    suspend fun resetCampaign(): CampaignProgress {
        campaignDao.clearProgress()
        val fresh = CampaignMapGenerator.generateNewCampaign()
        saveProgress(fresh)
        return fresh
    }

    private fun mapEntityToDomain(entity: CampaignProgressEntity): CampaignProgress {
        return CampaignProgress(
            stage = entity.stage,
            currentNodeId = entity.currentNodeId,
            credits = entity.credits,
            unlockedShips = runCatching { json.decodeFromString<Set<com.hexafleet.domain.game.model.ShipClass>>(entity.unlockedShipsJson) }.getOrElse { emptySet() },
            activeFleet = runCatching { json.decodeFromString<List<com.hexafleet.domain.game.model.ShipClass>>(entity.activeFleetJson) }.getOrElse { emptyList() },
            nodes = runCatching { json.decodeFromString<Map<String, com.hexafleet.domain.game.campaign.CampaignNode>>(entity.nodesJson) }.getOrElse { emptyMap() }
        )
    }
}

