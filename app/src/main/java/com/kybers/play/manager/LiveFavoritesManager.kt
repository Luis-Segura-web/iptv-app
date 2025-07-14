package com.kybers.play.manager

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kybers.play.api.LiveStream

/**
 * Objeto Singleton para gestionar la lista de canales de TV favoritos del usuario.
 */
object LiveFavoritesManager {

    private const val PREFS_NAME = "IPTV_LIVE_FAVORITES"
    private const val KEY_FAVORITE_CHANNELS = "favorite_live_channels"

    /**
     * Añade un canal a la lista de favoritos.
     * Si el canal ya es favorito, no hace nada.
     * @param context Contexto para acceder a SharedPreferences.
     * @param channel El LiveStream a añadir.
     */
    fun addFavorite(context: Context, channel: LiveStream) {
        val favorites = getFavorites(context).toMutableList()
        // Aseguramos que no se añadan duplicados.
        if (favorites.none { it.streamId == channel.streamId }) {
            favorites.add(0, channel) // Añade al principio para que aparezca primero.
            saveFavorites(context, favorites)
        }
    }

    /**
     * Elimina un canal de la lista de favoritos.
     * @param channel El LiveStream a eliminar.
     */
    fun removeFavorite(context: Context, channel: LiveStream) {
        val favorites = getFavorites(context).toMutableList()
        favorites.removeAll { it.streamId == channel.streamId }
        saveFavorites(context, favorites)
    }

    /**
     * Comprueba si un canal específico está en la lista de favoritos.
     * @return True si el canal es favorito.
     */
    fun isFavorite(context: Context, channel: LiveStream): Boolean {
        return getFavorites(context).any { it.streamId == channel.streamId }
    }

    /**
     * Obtiene la lista completa de canales favoritos.
     * @return Una lista de objetos LiveStream.
     */
    fun getFavorites(context: Context): List<LiveStream> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITE_CHANNELS, null)
        return if (json != null) {
            // Usamos TypeToken para decirle a Gson cómo convertir el JSON de vuelta a una List<LiveStream>.
            val type = object : TypeToken<List<LiveStream>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList() // Si no hay favoritos guardados, devuelve una lista vacía.
        }
    }

    /**
     * Guarda la lista de favoritos en SharedPreferences.
     * Convierte la lista de objetos a una cadena JSON antes de guardarla.
     */
    private fun saveFavorites(context: Context, favorites: List<LiveStream>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_FAVORITE_CHANNELS, Gson().toJson(favorites))
        }
    }
}
