package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "limits")
data class LimitEntity(
    @PrimaryKey val id: Int = 1,
    val dailyLimitMb: Long = 0,
    val updatedAt: Long
)
