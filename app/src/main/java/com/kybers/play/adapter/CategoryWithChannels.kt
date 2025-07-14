package com.kybers.play.adapter

import com.kybers.play.api.Category
import com.kybers.play.api.LiveStream

/**
 * Un modelo de datos que combina una Categoría con su lista de Canales
 * y mantiene el estado de si está expandida o no en la UI.
 */
data class CategoryWithChannels(
    val category: Category,
    val channels: MutableList<LiveStream>,
    var isExpanded: Boolean = false
)
