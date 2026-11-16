package com.hexafleet.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hexafleet.data.local.dao.CampaignDao
import com.hexafleet.data.local.dao.PlayerStatsDao
import com.hexafleet.data.local.entity.CampaignProgressEntity
import com.hexafleet.data.local.entity.PlayerStatsEntity

@Database(
    entities = [
        CampaignProgressEntity::class,
        PlayerStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HexaFleetDatabase : RoomDatabase() {
    abstract fun campaignDao(): CampaignDao
    abstract fun playerStatsDao(): PlayerStatsDao

    companion object {
        @Volatile
        private var INSTANCE: HexaFleetDatabase? = null

        fun getInstance(context: Context): HexaFleetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HexaFleetDatabase::class.java,
                    "hexafleet_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

