package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pc_reports")
data class PcReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,

    @ColumnInfo(name = "device_id")
    val deviceId: String = "pc",

    @ColumnInfo(name = "bytes_up_total")
    val bytesUpTotal: Long = 0,

    @ColumnInfo(name = "bytes_down_total")
    val bytesDownTotal: Long = 0
)