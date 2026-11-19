package com.dmitrylab.traffictracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dmitrylab.traffictracker.data.db.entity.LimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: LimitEntity)

    @Query("SELECT * FROM limits WHERE id = 1")
    fun getFlow(): Flow<LimitEntity?>

    @Query("SELECT * FROM limits WHERE id = 1")
    suspend fun get(): LimitEntity?
}
