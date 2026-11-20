package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    @ColumnInfo(name = "device_id")
    val deviceId: String,

    val name: String,

    @ColumnInfo(name = "last_seen")
    val lastSeen: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
