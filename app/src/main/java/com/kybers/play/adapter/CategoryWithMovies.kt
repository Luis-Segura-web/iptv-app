package com.kybers.play.adapter

import com.kybers.play.api.Category
import com.kybers.play.api.Movie

/**
 * Un modelo de datos que combina una Categoría de Películas con su lista de Películas.
 * Ahora incluye estados para manejar la carga diferida (lazy loading).
 *
 * @param category La información de la categoría.
 * @param movies La lista de películas. Es 'nullable' porque al principio no las tendremos.
 * @param isExpanded Controla si la categoría está expandida en la UI.
 * @param isLoading Indica si estamos cargando actualmente las películas para esta categoría.
 */
data class CategoryWithMovies(
    val category: Category,
    var movies: List<Movie>? = null, // Se inicializa como nulo.
    var isExpanded: Boolean = false,
    var isLoading: Boolean = false
)
