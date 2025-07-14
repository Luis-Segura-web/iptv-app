package com.kybers.play.manager

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kybers.play.adapter.CategoryWithChannels
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Objeto Singleton para gestionar el caché de datos en archivos y
 * controlar la lógica de expiración.
 */
object DataCacheManager {

    private const val PREFS_NAME = "IPTV_CACHE_TIMESTAMPS"
    private const val KEY_TV_SYNC_TIMESTAMP = "tv_sync_timestamp"
    private const val TV_CACHE_FILE = "tv_cache.json"
    private val CACHE_EXPIRATION_HOURS = 12L

    // Caché en memoria para acceso rápido durante la sesión.
    var tvCategories: List<CategoryWithChannels>? = null

    /**
     * Comprueba si el caché de TV ha expirado (más de 12 horas).
     */
    fun isTvCacheStale(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_TV_SYNC_TIMESTAMP, 0L)
        if (lastSync == 0L) return true // Si nunca se ha sincronizado, está obsoleto.

        val hoursSinceLastSync = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastSync)
        return hoursSinceLastSync >= CACHE_EXPIRATION_HOURS
    }

    /**
     * Actualiza la marca de tiempo de la última sincronización de TV.
     */
    fun updateTvSyncTimestamp(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_TV_SYNC_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    /**
     * Guarda la lista de categorías de TV en un archivo JSON en el almacenamiento interno.
     */
    fun saveTvCacheToFile(context: Context, data: List<CategoryWithChannels>) {
        try {
            val file = File(context.filesDir, TV_CACHE_FILE)
            val json = Gson().toJson(data)
            file.writeText(json)
            Log.d("DataCacheManager", "Caché de TV guardado en archivo exitosamente.")
        } catch (e: Exception) {
            Log.e("DataCacheManager", "Error al guardar el caché de TV en archivo", e)
        }
    }

    /**
     * Carga la lista de categorías de TV desde el archivo JSON.
     * @return La lista de categorías o null si el archivo no existe o hay un error.
     */
    fun loadTvCacheFromFile(context: Context): List<CategoryWithChannels>? {
        try {
            val file = File(context.filesDir, TV_CACHE_FILE)
            if (!file.exists()) return null

            val json = file.readText()
            if (json.isBlank()) return null

            val type = object : TypeToken<List<CategoryWithChannels>>() {}.type
            Log.d("DataCacheManager", "Caché de TV cargado desde archivo exitosamente.")
            return Gson().fromJson(json, type)
        } catch (e: Exception) {
            Log.e("DataCacheManager", "Error al cargar el caché de TV desde archivo", e)
            return null
        }
    }
}
