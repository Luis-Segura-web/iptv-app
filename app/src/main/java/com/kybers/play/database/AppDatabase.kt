package com.kybers.play.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.Series

/**
 * La clase principal de la base de datos de la aplicación.
 * CORREGIDO: Se ha incrementado la versión de la base de datos de 1 a 2
 * porque hemos modificado la estructura de la tabla 'movies'.
 */
@Database(entities = [LiveStream::class, Movie::class, Series::class], version = 2, exportSchema = false)
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
                )
                    // CORREGIDO: Se añade fallbackToDestructiveMigration().
                    // Esto le dice a Room que si hay una migración de versión sin un plan específico,
                    // simplemente borre la base de datos y la cree de nuevo. Es la solución más
                    // sencilla durante el desarrollo.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
