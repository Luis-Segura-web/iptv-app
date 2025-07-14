package com.kybers.play.ui.home

/**
 * Representa una categoría o fila en la pantalla de inicio.
 *
 * @property title El título que se mostrará para la fila (ej: "Continuar Viendo").
 * @property items La lista de contenido para esa fila (puede ser una lista de HistoryItem, Movie, Series, etc.).
 * Usamos 'List<Any>' para que sea flexible y pueda contener cualquier tipo de contenido.
 */
data class HomeCategory(
    val title: String,
    val items: List<Any>
)
