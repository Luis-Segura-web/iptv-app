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

/**
 * Objeto Singleton para gestionar el historial de visualización ("Continuar Viendo").
 */
object HistoryManager {

    private const val PREFS_NAME = "IPTV_HISTORY"
    private const val KEY_HISTORY_ITEMS = "history_items"
    private const val MAX_HISTORY_SIZE = 20 // Limita el historial a 20 elementos

    /**
     * Añade o actualiza un item en el historial.
     * Si el contenido ya existe en el historial, lo mueve al principio.
     * @param context Contexto para acceder a SharedPreferences.
     * @param historyItem El item del historial a guardar.
     */
    fun addItemToHistory(context: Context, historyItem: HistoryItem) {
        val history = getHistory(context).toMutableList()
        // Elimina cualquier entrada anterior del mismo contenido para evitar duplicados.
        history.removeAll { it.content.getContentId() == historyItem.content.getContentId() && it.content.getType() == historyItem.content.getType() }
        // Añade el nuevo item al principio de la lista.
        history.add(0, historyItem)
        // Se asegura de que la lista no exceda el tamaño máximo.
        val trimmedHistory = if (history.size > MAX_HISTORY_SIZE) history.subList(0, MAX_HISTORY_SIZE) else history
        saveHistory(context, trimmedHistory)
    }

    /**
     * Obtiene la lista completa del historial.
     * @return Una lista de HistoryItem.
     */
    fun getHistory(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY_ITEMS, null) ?: return emptyList()

        // Usamos un GsonBuilder para registrar nuestro deserializador personalizado.
        // Esto es necesario porque 'PlayableContent' es una interfaz y Gson no sabe
        // qué clase concreta (Movie, Series, etc.) instanciar sin ayuda.
        val gson = GsonBuilder()
            .registerTypeAdapter(PlayableContent::class.java, PlayableContentDeserializer())
            .create()

        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: JsonParseException) {
            // Si hay un error al leer el JSON, devuelve una lista vacía para evitar que la app crashee.
            emptyList()
        }
    }

    /**
     * Guarda la lista del historial en SharedPreferences.
     */
    private fun saveHistory(context: Context, list: List<HistoryItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Usamos un Gson simple para guardar, ya que la serialización no necesita el adaptador.
        val gson = Gson()
        prefs.edit {
            putString(KEY_HISTORY_ITEMS, gson.toJson(list))
        }
    }
}

/**
 * Deserializador personalizado para la interfaz PlayableContent.
 * Gson lo utiliza al leer el JSON del historial. Revisa las propiedades únicas
 * de cada objeto JSON para decidir si debe crear un objeto Movie, Series o LiveStream.
 */
class PlayableContentDeserializer : JsonDeserializer<PlayableContent> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PlayableContent {
        val jsonObject = json.asJsonObject
        // Decide qué tipo de objeto crear basándose en los campos que existen en el JSON.
        return when {
            // Si tiene 'seriesId', es una Serie.
            jsonObject.has("seriesId") -> context.deserialize(jsonObject, Series::class.java)
            // Si tiene 'containerExtension', es una Película.
            jsonObject.has("containerExtension") -> context.deserialize(jsonObject, Movie::class.java)
            // Si solo tiene 'streamId' (y no los otros), es un Canal.
            jsonObject.has("streamId") -> context.deserialize(jsonObject, LiveStream::class.java)
            // Si no coincide con ninguno, lanza una excepción.
            else -> throw JsonParseException("Unknown PlayableContent type")
        }
    }
}
