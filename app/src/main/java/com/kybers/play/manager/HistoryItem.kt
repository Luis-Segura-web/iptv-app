package com.kybers.play.manager

import com.kybers.play.api.PlayableContent

data class HistoryItem(
    val content: PlayableContent,
    val lastPosition: Long, // Posición en milisegundos
    val duration: Long,     // Duración total en milisegundos
    val timestamp: Long = System.currentTimeMillis()
)
