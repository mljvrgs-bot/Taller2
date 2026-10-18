package com.caos.dominocaos.logic.model

data class Player(
    val id: String,
    val name: String,
    val hand: List<Tile> = emptyList(),
    val score: Int = 0,
    val isBot: Boolean = false
) {
}
