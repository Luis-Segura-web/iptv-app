package com.kybers.play.manager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kybers.play.adapter.CategoryWithChannels
import com.kybers.play.api.Movie
import com.kybers.play.api.Series
import java.io.IOException

object DataCacheManager {

    var tvCategories: List<CategoryWithChannels>? = null
    var movies: List<Movie>? = null
    var series: List<Series>? = null
    var homeCategories: List<Any>? = null

    private const val PREFS_NAME = "CachePrefs"
    private const val KEY_LAST_TV_SYNC = "last_tv_sync_timestamp"
    private const val CACHE_DURATION_HOURS = 4
    private const val CACHE_DURATION_MS = CACHE_DURATION_HOURS * 60 * 60 * 1000L

    // AÑADIDO: Nombre del archivo para el caché persistente.
    private const val TV_CACHE_FILENAME = "tv_cache.json"

    fun isTvCacheStale(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_TV_SYNC, 0L)
        return (System.currentTimeMillis() - lastSync) > CACHE_DURATION_MS
    }

    fun updateTvSyncTimestamp(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putLong(KEY_LAST_TV_SYNC, System.currentTimeMillis())
            apply()
        }
    }

    /**
     * AÑADIDO: Guarda la lista de categorías de TV en un archivo JSON.
     * @param context Contexto para acceder al almacenamiento interno.
     * @param data La lista de categorías a guardar.
     */
    fun saveTvCacheToFile(context: Context, data: List<CategoryWithChannels>) {
        val gson = Gson()
        val jsonString = gson.toJson(data)
        try {
            context.openFileOutput(TV_CACHE_FILENAME, Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * AÑADIDO: Carga la lista de categorías de TV desde un archivo JSON.
     * @param context Contexto para acceder al almacenamiento interno.
     * @return La lista de categorías guardada, o null si no existe o hay un error.
     */
    fun loadTvCacheFromFile(context: Context): List<CategoryWithChannels>? {
        return try {
            val fileInputStream = context.openFileInput(TV_CACHE_FILENAME)
            val jsonString = fileInputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<CategoryWithChannels>>() {}.type
            Gson().fromJson(jsonString, type)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun clearAll() {
        tvCategories = null
        movies = null
        series = null
        homeCategories = null
    }
}
