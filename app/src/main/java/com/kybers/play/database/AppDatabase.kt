package com.kybers.play.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.Series

@Database(entities = [LiveStream::class, Movie::class, Series::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iptv_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
