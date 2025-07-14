package com.kybers.play.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Objeto de utilidad para funciones relacionadas con la red.
 */
object NetworkUtils {

    /**
     * Verifica si el dispositivo tiene una conexión a internet activa.
     * @param context Contexto para acceder a los servicios del sistema.
     * @return True si hay conexión (WiFi, Celular, Ethernet), false en caso contrario.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
