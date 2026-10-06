package com.example.data.focus

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [FocusSessionEntity::class, FocusSegmentEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun focusDao(): FocusDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "finite_time_database",
              )
              .build()
              .also { instance = it }
        }
    }
  }
}
