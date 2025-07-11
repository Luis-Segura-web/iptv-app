package com.kybers.play.manager

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.kybers.play.api.LiveStream
import com.kybers.play.api.Movie
import com.kybers.play.api.PlayableContent
import com.kybers.play.api.Series
import java.lang.reflect.Type

object HistoryManager {

    private const val PREFS_NAME = "IPTV_HISTORY"
    private const val KEY_HISTORY_ITEMS = "history_items"
    private const val MAX_HISTORY_SIZE = 20

    // Añade un item al historial. Si ya existe, lo mueve al principio.
    fun addItemToHistory(context: Context, historyItem: HistoryItem) {
        val history = getHistory(context).toMutableList()
        // Elimina el item si ya existía para moverlo al principio
        history.removeAll { it.content.getContentId() == historyItem.content.getContentId() && it.content.getType() == historyItem.content.getType() }
        history.add(0, historyItem)
        // Limita el tamaño del historial a los últimos 20 items
        val trimmedHistory = if (history.size > MAX_HISTORY_SIZE) history.subList(0, MAX_HISTORY_SIZE) else history
        saveHistory(context, trimmedHistory)
    }

    // Obtiene la lista completa del historial
    fun getHistory(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY_ITEMS, null) ?: return emptyList()

        // Usamos un deserializador personalizado para que Gson sepa cómo convertir el JSON a la interfaz PlayableContent
        val gson = GsonBuilder()
            .registerTypeAdapter(PlayableContent::class.java, PlayableContentDeserializer())
            .create()

        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { // CORREGIDO: Se reemplaza 'e' con '_' para indicar que no se usa.
            emptyList()
        }
    }

    // Guarda la lista del historial en SharedPreferences
    private fun saveHistory(context: Context, list: List<HistoryItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        prefs.edit {
            putString(KEY_HISTORY_ITEMS, gson.toJson(list))
        }
    }
}

/**
 * Deserializador personalizado para que Gson pueda manejar la interfaz PlayableContent.
 * Determina el tipo de objeto (Movie, Series, etc.) basándose en las claves únicas del JSON.
 */
class PlayableContentDeserializer : JsonDeserializer<PlayableContent> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PlayableContent {
        val jsonObject = json.asJsonObject
        return when {
            jsonObject.has("seriesId") -> context.deserialize(jsonObject, Series::class.java)
            jsonObject.has("containerExtension") -> context.deserialize(jsonObject, Movie::class.java)
            jsonObject.has("streamId") -> context.deserialize(jsonObject, LiveStream::class.java)
            else -> throw JsonParseException("Unknown PlayableContent type")
        }
    }
}
