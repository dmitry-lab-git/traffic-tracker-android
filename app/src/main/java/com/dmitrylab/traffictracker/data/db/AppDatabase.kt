package com.dmitrylab.traffictracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dmitrylab.traffictracker.data.db.dao.*
import com.dmitrylab.traffictracker.data.db.entity.*

@Database(
    entities = [
        DeviceEntity::class,
        TrafficLogEntity::class,
        TetherTotalEntity::class,
        PcReportEntity::class,
        DomainPhoneEntity::class,
        LimitEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun trafficLogDao(): TrafficLogDao
    abstract fun tetherTotalDao(): TetherTotalDao
    abstract fun pcReportDao(): PcReportDao
    abstract fun domainPhoneDao(): DomainPhoneDao
    abstract fun limitDao(): LimitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tt_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}