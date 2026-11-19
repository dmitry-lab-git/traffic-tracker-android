package com.dmitrylab.traffictracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "domains_phone")
data class DomainPhoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val appName: String,
    val domain: String,
    val bytesUp: Long = 0,
    val bytesDown: Long = 0
)
