package com.kybers.play.manager

import com.kybers.play.adapter.CategoryWithChannels

/**
 * Un objeto singleton simple para mantener en memoria la lista de categorías de TV.
 * Esto permite una carga instantánea de la UI mientras se actualizan los datos en segundo plano.
 */
object DataCacheManager {
    var tvCategories: List<CategoryWithChannels>? = null
}
