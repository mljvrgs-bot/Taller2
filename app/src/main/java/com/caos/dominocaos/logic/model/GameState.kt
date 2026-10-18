package com.caos.dominocaos.logic.model

import com.google.firebase.database.Exclude

data class GameState(
    val gameId: String = "",
    val players: List<Player> = emptyList(),
    val drawPile: List<Tile> = emptyList(),
    val board: List<Tile> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val leftEnd: Int? = null,
    val rightEnd: Int? = null,
    val direction: Int = 1,
    val finished: Boolean = false,
    val winnerId: String? = null,
    val events: List<GameEvent> = emptyList(),
    val doublePointsNextTurn: Boolean = false,
    val activePower: PowerType = PowerType.NONE
) {
    @get:Exclude
    val currentPlayer: Player?
        get() = players.getOrNull(currentPlayerIndex)

    @get:Exclude
    val isBlocked: Boolean
        get() = !finished && drawPile.isEmpty() &&
                players.all { p -> p.hand.none { it.matches(leftEnd ?: -1) || it.matches(rightEnd ?: -1) } }
}