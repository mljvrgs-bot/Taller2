package com.caos.dominocaos.logic.model

data class GameEvent(
    val id: String,
    val type: String,
    val message: String,
    val playerId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
