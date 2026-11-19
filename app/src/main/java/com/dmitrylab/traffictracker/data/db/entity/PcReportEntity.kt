package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pc_reports")
data class PcReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val deviceId: String = "pc",
    val bytesUpTotal: Long = 0,
    val bytesDownTotal: Long = 0
)
