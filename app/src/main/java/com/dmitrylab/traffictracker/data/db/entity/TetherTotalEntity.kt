package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tether_total")
data class TetherTotalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,

    @ColumnInfo(name = "bytes_up")
    val bytesUp: Long = 0,

    @ColumnInfo(name = "bytes_down")
    val bytesDown: Long = 0
)