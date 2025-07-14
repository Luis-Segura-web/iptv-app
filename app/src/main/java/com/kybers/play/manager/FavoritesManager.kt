package com.kybers.play.manager

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kybers.play.api.Series

/**
 * Objeto Singleton para gestionar la lista de series favoritas del usuario.
 * Es muy similar al LiveFavoritesManager, pero se especializa en objetos 'Series'.
 */
object FavoritesManager {

    private const val PREFS_NAME = "IPTV_FAVORITES"
    private const val KEY_FAVORITE_SERIES = "favorite_series"

    /**
     * Añade una serie a la lista de favoritos.
     * Si la serie ya es favorita, no hace nada.
     * @param context Contexto para acceder a SharedPreferences.
     * @param series La Serie a añadir.
     */
    fun addFavoriteSeries(context: Context, series: Series) {
        val favorites = getFavoriteSeries(context).toMutableList()
        if (favorites.none { it.seriesId == series.seriesId }) {
            favorites.add(0, series)
            saveList(context, KEY_FAVORITE_SERIES, favorites)
        }
    }

    /**
     * Elimina una serie de la lista de favoritos.
     * @param series La Serie a eliminar.
     */
    fun removeFavoriteSeries(context: Context, series: Series) {
        val favorites = getFavoriteSeries(context).toMutableList()
        favorites.removeAll { it.seriesId == series.seriesId }
        saveList(context, KEY_FAVORITE_SERIES, favorites)
    }

    /**
     * Obtiene la lista completa de series favoritas.
     */
    fun getFavoriteSeries(context: Context): List<Series> {
        return loadList(context, KEY_FAVORITE_SERIES)
    }

    /**
     * Comprueba si una serie específica es favorita por su ID.
     * @return True si la serie es favorita.
     */
    fun isFavorite(context: Context, seriesId: Int): Boolean {
        return getFavoriteSeries(context).any { it.seriesId == seriesId }
    }

    /**
     * Función genérica y reutilizable para guardar una lista de cualquier tipo en SharedPreferences.
     * @param key La clave bajo la cual se guardará la lista.
     * @param list La lista de objetos a guardar.
     */
    private inline fun <reified T> saveList(context: Context, key: String, list: List<T>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(key, Gson().toJson(list))
        }
    }

    /**
     * Función genérica y reutilizable para cargar una lista de cualquier tipo desde SharedPreferences.
     * @param key La clave de la lista a cargar.
     * @return La lista de objetos, o una lista vacía si no se encuentra nada.
     */
    private inline fun <reified T> loadList(context: Context, key: String): List<T> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(key, null)
        return if (json != null) {
            val type = object : TypeToken<List<T>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }
}
