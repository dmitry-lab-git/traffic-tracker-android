package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "limits")
data class LimitEntity(
    @PrimaryKey val id: Int = 1,

    @ColumnInfo(name = "daily_limit_mb")
    val dailyLimitMb: Long = 0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)