package com.example.pickupcode.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PickupItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PickupDatabase : RoomDatabase() {

    abstract fun pickupDao(): PickupDao

    companion object {
        private const val DATABASE_NAME = "pickup_code_local.db"

        @Volatile
        private var instance: PickupDatabase? = null

        fun getInstance(context: Context): PickupDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PickupDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
