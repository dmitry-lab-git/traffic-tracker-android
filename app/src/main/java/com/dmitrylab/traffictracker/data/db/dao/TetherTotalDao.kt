package com.dmitrylab.traffictracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dmitrylab.traffictracker.data.db.entity.TetherTotalEntity

@Dao
interface TetherTotalDao {
    @Insert
    suspend fun insert(record: TetherTotalEntity)

    @Query("SELECT * FROM tether_total WHERE timestamp >= :since ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSince(since: Long): TetherTotalEntity?
    
    @Query("SELECT * FROM tether_total ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): TetherTotalEntity?
}
