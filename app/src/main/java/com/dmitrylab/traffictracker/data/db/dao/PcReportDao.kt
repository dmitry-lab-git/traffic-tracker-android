package com.dmitrylab.traffictracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dmitrylab.traffictracker.data.db.entity.PcReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PcReportDao {
    @Insert
    suspend fun insert(report: PcReportEntity)

    @Query("SELECT * FROM pc_reports ORDER BY timestamp DESC LIMIT 1")
    fun getLatestFlow(): Flow<PcReportEntity?>

    @Query("SELECT * FROM pc_reports ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): PcReportEntity?
}
