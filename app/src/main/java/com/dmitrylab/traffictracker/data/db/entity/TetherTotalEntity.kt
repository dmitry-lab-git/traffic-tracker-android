package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tether_total")
data class TetherTotalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val bytesUp: Long = 0,
    val bytesDown: Long = 0
)
