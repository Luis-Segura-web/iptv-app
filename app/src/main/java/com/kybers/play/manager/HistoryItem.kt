package com.kybers.play.manager

import com.kybers.play.api.PlayableContent

/**
 * Representa un único elemento en el historial de "Continuar Viendo".
 *
 * @property content El contenido que se estaba viendo (puede ser una Película, Serie, etc.).
 * @property lastPosition La posición en milisegundos donde el usuario dejó de ver.
 * @property duration La duración total del contenido en milisegundos.
 * @property timestamp La fecha y hora en que se guardó este historial, para ordenarlo.
 */
data class HistoryItem(
    val content: PlayableContent,
    val lastPosition: Long,
    val duration: Long,
    val timestamp: Long = System.currentTimeMillis()
)
