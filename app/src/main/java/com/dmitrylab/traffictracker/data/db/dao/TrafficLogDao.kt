package com.dmitrylab.traffictracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dmitrylab.traffictracker.data.db.entity.TrafficLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrafficLogDao {
    @Insert
    suspend fun insert(log: TrafficLogEntity)

    @Query("SELECT * FROM traffic_log WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getSince(since: Long): Flow<List<TrafficLogEntity>>

    @Query("SELECT SUM(bytes_up) FROM traffic_log WHERE timestamp >= :since")
    suspend fun sumUpSince(since: Long): Long?

    @Query("SELECT SUM(bytes_down) FROM traffic_log WHERE timestamp >= :since")
    suspend fun sumDownSince(since: Long): Long?

    @Query("DELETE FROM traffic_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
