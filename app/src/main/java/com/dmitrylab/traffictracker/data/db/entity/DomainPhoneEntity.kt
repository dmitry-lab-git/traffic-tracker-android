package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "domains_phone")
data class DomainPhoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,

    @ColumnInfo(name = "app_name")
    val appName: String,

    val domain: String,

    @ColumnInfo(name = "bytes_up")
    val bytesUp: Long = 0,

    @ColumnInfo(name = "bytes_down")
    val bytesDown: Long = 0
)