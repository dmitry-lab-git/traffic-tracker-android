package com.dmitrylab.traffictracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dmitrylab.traffictracker.data.db.entity.DomainPhoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainPhoneDao {
    @Insert
    suspend fun insert(domain: DomainPhoneEntity)

    @Query("SELECT * FROM domains_phone WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getSince(since: Long): Flow<List<DomainPhoneEntity>>
}
