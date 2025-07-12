package com.kybers.play.manager

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kybers.play.api.LiveStream

object LiveFavoritesManager {

    private const val PREFS_NAME = "IPTV_LIVE_FAVORITES"
    private const val KEY_FAVORITE_CHANNELS = "favorite_live_channels"

    fun addFavorite(context: Context, channel: LiveStream) {
        val favorites = getFavorites(context).toMutableList()
        if (favorites.none { it.streamId == channel.streamId }) {
            favorites.add(0, channel)
            saveFavorites(context, favorites)
        }
    }

    fun removeFavorite(context: Context, channel: LiveStream) {
        val favorites = getFavorites(context).toMutableList()
        favorites.removeAll { it.streamId == channel.streamId }
        saveFavorites(context, favorites)
    }

    fun isFavorite(context: Context, channel: LiveStream): Boolean {
        return getFavorites(context).any { it.streamId == channel.streamId }
    }

    fun getFavorites(context: Context): List<LiveStream> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITE_CHANNELS, null)
        return if (json != null) {
            val type = object : TypeToken<List<LiveStream>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun saveFavorites(context: Context, favorites: List<LiveStream>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_FAVORITE_CHANNELS, Gson().toJson(favorites))
        }
    }
}
