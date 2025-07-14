package com.kybers.play.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Objeto Singleton para gestionar la instancia de Retrofit.
 * Se encarga de crear y reutilizar el cliente de red para comunicarse con la API.
 */
object RetrofitClient {

    // Variable para almacenar la instancia de Retrofit. Es 'nullable' para poder crearla la primera vez.
    private var retrofit: Retrofit? = null

    /**
     * Obtiene una instancia configurada de Retrofit.
     * Si la instancia no existe o si la URL base ha cambiado, crea una nueva.
     * De lo contrario, devuelve la instancia existente para reutilizar la conexión.
     *
     * @param baseUrl La URL base del servidor Xtream Codes (ej: "http://servidor.com:8080").
     * @return Una instancia de Retrofit lista para usar.
     */
    fun getClient(baseUrl: String): Retrofit {
        // Comprobamos si la instancia actual es nula o si la URL ha cambiado.
        if (retrofit == null || retrofit?.baseUrl().toString() != baseUrl) {
            // Si es así, construimos una nueva instancia de Retrofit.
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl) // Establecemos la URL base del servidor.
                .addConverterFactory(GsonConverterFactory.create()) // Añadimos un conversor para transformar el JSON a objetos Kotlin (usando Gson).
                .build() // Creamos el objeto Retrofit.
        }
        // Devolvemos la instancia (nunca será nula después de la primera vez).
        return retrofit!!
    }
}
