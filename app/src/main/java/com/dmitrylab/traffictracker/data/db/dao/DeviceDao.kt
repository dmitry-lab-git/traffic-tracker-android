package com.dmitrylab.traffictracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dmitrylab.traffictracker.data.db.entity.DeviceEntity

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity)

    @Query("SELECT * FROM devices WHERE device_id = :id LIMIT 1")
    suspend fun getById(id: String): DeviceEntity?

    @Query("SELECT * FROM devices ORDER BY last_seen DESC")
    suspend fun getAll(): List<DeviceEntity>
}
