package com.kybers.play.api

/**
 * Define un contrato para cualquier tipo de contenido que se pueda reproducir.
 * Obliga a que todas las clases que lo implementen (LiveStream, Movie, Series)
 * tengan métodos comunes para obtener su ID, título, portada y tipo.
 * Esto es muy útil para tratar diferentes tipos de contenido de la misma manera.
 */
interface PlayableContent {
    fun getContentId(): String
    fun getTitle(): String
    fun getCoverUrl(): String
    fun getType(): String
}
