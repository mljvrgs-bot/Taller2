package com.caos.dominocaos.logic.model

data class GameState(
    val gameId: String = "",
    val players: List<Player> = emptyList(),
    val drawPile: List<Tile> = emptyList(),
    val board: List<Tile> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val leftEnd: Int? = null,
    val rightEnd: Int? = null,
    val direction: Int = 1,
    val isFinished: Boolean = false,
    val winnerId: String? = null,
    val events: List<GameEvent> = emptyList(),
    val doublePointsNextTurn: Boolean = false
) {
    val currentPlayer: Player?
        get() = players.getOrNull(currentPlayerIndex)

    val isBlocked: Boolean
        get() = !isFinished && drawPile.isEmpty() &&
            players.all { player -> player.hand.none { it.matches(leftEnd ?: -1) || it.matches(rightEnd ?: -1) } }
}
