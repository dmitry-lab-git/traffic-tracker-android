package com.dmitrylab.traffictracker.data.db.entity

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
    val deviceId: String = "phone",
    val appName: String,
    val networkType: String,
    val bytesUp: Long = 0,
    val bytesDown: Long = 0
)
