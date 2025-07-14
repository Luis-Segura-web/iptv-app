package com.kybers.play.manager

import android.content.Context
import androidx.core.content.edit

/**
 * Objeto Singleton para gestionar los ajustes de la aplicación.
 * Utiliza SharedPreferences para guardar y recuperar datos simples de configuración.
 */
object SettingsManager {

    private const val PREFS_NAME = "IPTV_SETTINGS"
    private const val KEY_AUTOPLAY_NEXT = "autoplay_next_episode"
    private const val KEY_USER_AGENT = "user_agent"

    /**
     * Guarda la preferencia de auto-reproducir el siguiente episodio.
     * @param context Contexto para acceder a SharedPreferences.
     * @param enabled True si la auto-reproducción debe estar activada.
     */
    fun setAutoplayNextEpisode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Usamos la función de extensión KTX 'edit' para un código más limpio y seguro.
        prefs.edit {
            putBoolean(KEY_AUTOPLAY_NEXT, enabled)
        }
    }

    /**
     * Comprueba si la auto-reproducción está habilitada.
     * @return True si está habilitada, por defecto es true.
     */
    fun isAutoplayNextEpisodeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTOPLAY_NEXT, true)
    }

    /**
     * Guarda el User-Agent personalizado para el reproductor.
     * @param userAgent La cadena de User-Agent a utilizar.
     */
    fun setUserAgent(context: Context, userAgent: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_USER_AGENT, userAgent)
        }
    }

    /**
     * Obtiene el User-Agent guardado.
     * @return El User-Agent personalizado, o "ExoPlayer" por defecto.
     */
    fun getUserAgent(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // El operador 'elvis' (?:) asegura que devolvemos "ExoPlayer" si el valor es nulo.
        return prefs.getString(KEY_USER_AGENT, "ExoPlayer") ?: "ExoPlayer"
    }
}
