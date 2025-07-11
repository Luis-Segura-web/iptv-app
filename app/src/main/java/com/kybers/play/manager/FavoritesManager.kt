package com.kybers.play.manager

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kybers.play.api.Movie
import com.kybers.play.api.Series

object FavoritesManager {

    private const val PREFS_NAME = "IPTV_FAVORITES"
    private const val KEY_FAVORITE_SERIES = "favorite_series"

    fun addFavoriteSeries(context: Context, series: Series) {
        val favorites = getFavoriteSeries(context).toMutableList()
        if (favorites.none { it.seriesId == series.seriesId }) {
            favorites.add(0, series)
            saveList(context, KEY_FAVORITE_SERIES, favorites)
        }
    }

    fun removeFavoriteSeries(context: Context, series: Series) {
        val favorites = getFavoriteSeries(context).toMutableList()
        favorites.removeAll { it.seriesId == series.seriesId }
        saveList(context, KEY_FAVORITE_SERIES, favorites)
    }

    fun getFavoriteSeries(context: Context): List<Series> {
        return loadList(context, KEY_FAVORITE_SERIES)
    }

    fun isFavorite(context: Context, seriesId: Int): Boolean {
        return getFavoriteSeries(context).any { it.seriesId == seriesId }
    }

    private inline fun <reified T> saveList(context: Context, key: String, list: List<T>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(key, Gson().toJson(list))
        }
    }

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
