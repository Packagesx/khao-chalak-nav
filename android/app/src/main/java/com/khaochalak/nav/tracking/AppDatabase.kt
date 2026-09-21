package com.khaochalak.nav.tracking

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Milestone 3: local persistence for recorded GPS routes (Room/SQLite). */
@Database(entities = [RecordingEntity::class, TrackPointEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "khao-chalak-nav.db",
                ).build().also { instance = it }
            }
    }
}
