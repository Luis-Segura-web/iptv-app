package com.kybers.play.manager

import android.content.Context
import androidx.core.content.edit

object SettingsManager {

    private const val PREFS_NAME = "IPTV_SETTINGS"
    private const val KEY_AUTOPLAY_NEXT = "autoplay_next_episode"
    private const val KEY_USER_AGENT = "user_agent"

    fun setAutoplayNextEpisode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // CORREGIDO: Usamos la función de extensión KTX
        prefs.edit {
            putBoolean(KEY_AUTOPLAY_NEXT, enabled)
        }
    }

    fun isAutoplayNextEpisodeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTOPLAY_NEXT, true)
    }

    fun setUserAgent(context: Context, userAgent: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // CORREGIDO: Usamos la función de extensión KTX
        prefs.edit {
            putString(KEY_USER_AGENT, userAgent)
        }
    }

    fun getUserAgent(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_AGENT, "ExoPlayer") ?: "ExoPlayer"
    }
}
