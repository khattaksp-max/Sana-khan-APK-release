package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class, MemoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SanaDatabase : RoomDatabase() {
    abstract fun sanaDao(): SanaDao

    companion object {
        @Volatile
        private var INSTANCE: SanaDatabase? = null

        fun getDatabase(context: Context): SanaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SanaDatabase::class.java,
                    "sana_assistant_db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
