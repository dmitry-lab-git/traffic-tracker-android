package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "traffic_log",
    indices = [Index(value = ["timestamp"])]
)
data class TrafficLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,

    @ColumnInfo(name = "device_id")
    val deviceId: String = "phone",

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "network_type")
    val networkType: String,

    @ColumnInfo(name = "bytes_up")
    val bytesUp: Long = 0,

    @ColumnInfo(name = "bytes_down")
    val bytesDown: Long = 0
)