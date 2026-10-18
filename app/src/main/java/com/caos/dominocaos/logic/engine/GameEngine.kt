package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.*
import java.util.UUID
import kotlin.random.Random

class GameEngine(private val random: Random = Random.Default) {
    fun newGame(
        playerNames: List<Pair<String, String>>,
        handSize: Int = 7,
        gameId: String = UUID.randomUUID().toString()
    ): GameState {
        require(playerNames.size in 2..4) { "La partida debe tener entre 2 y 4 jugadores." }
        require(playerNames.map { it.first }.toSet().size == playerNames.size) { "Los identificadores de jugadores deben ser únicos." }
        require(handSize > 0) { "El tamaño de la mano debe ser positivo." }

        val shuffled = Tile.doubleSixSet().shuffled(random)
        val players = playerNames.mapIndexed { index, pair ->
            val start = index * handSize
            Player(pair.first, pair.second, shuffled.drop(start).take(handSize))
        }
        val used = minOf(playerNames.size * handSize, shuffled.size)
        return GameState(gameId, players, shuffled.drop(used),
            events = listOf(EventManager.create("GAME_STARTED", "Comenzó una partida con ${players.size} jugadores.")))
    }

    fun playTile(state: GameState, playerId: String, tileId: String, side: BoardSide? = null): GameState {
        check(!state.isFinished) { "La partida ya terminó." }
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        val tile = player.hand.firstOrNull { it.id == tileId } ?: error("La ficha no está en la mano del jugador.")
        check(MoveValidator.canPlay(tile, state)) { "La ficha no coincide con ninguno de los extremos." }

        val actualSide = side ?: if (state.board.isEmpty()) BoardSide.RIGHT
            else if (tile.matches(state.rightEnd ?: -1)) BoardSide.RIGHT else BoardSide.LEFT
        check(actualSide in MoveValidator.validSides(tile, state)) { "La ficha no se puede colocar en ese extremo." }

        val oriented = when {
            state.board.isEmpty() -> tile
            actualSide == BoardSide.RIGHT -> tile.orientedToMatch(state.rightEnd!!)!!
            else -> tile.orientedToMatchRight(state.leftEnd!!)!!
        }
        val newBoard = if (state.board.isEmpty()) listOf(oriented)
            else if (actualSide == BoardSide.RIGHT) state.board + oriented else listOf(oriented) + state.board
        val newLeft = if (state.board.isEmpty()) oriented.left else if (actualSide == BoardSide.LEFT) oriented.left else state.leftEnd
        val newRight = if (state.board.isEmpty()) oriented.right else if (actualSide == BoardSide.RIGHT) oriented.right else state.rightEnd
        val remainingHand = player.hand.filterNot { it.id == tile.id }
        var players = state.players.map { if (it.id == player.id) it.copy(hand = remainingHand) else it }
        var events = EventManager.append(state.events, EventManager.create("TILE_PLAYED",
            "${player.name} jugó ${tile.left}|${tile.right}.", player.id))
        val won = remainingHand.isEmpty()
        var scoreWinner: String? = null
        if (won) {
            val points = players.filterNot { it.id == player.id }.sumOf { other -> other.hand.sumOf(Tile::points) }
            val scored = if (state.doublePointsNextTurn) points * 2 else points
            players = players.map { if (it.id == player.id) it.copy(score = it.score + scored) else it }
            scoreWinner = player.id
            events = EventManager.append(events, EventManager.create("GAME_FINISHED",
                "${player.name} ganó la ronda y obtuvo $scored puntos.", player.id))
        }
        val next = if (won) state.currentPlayerIndex else nextIndex(state, state.currentPlayerIndex)
        return state.copy(players = players, board = newBoard, leftEnd = newLeft, rightEnd = newRight,
            currentPlayerIndex = next, isFinished = won, winnerId = scoreWinner, events = events,
            doublePointsNextTurn = false)
    }

    fun drawTile(state: GameState, playerId: String): GameState {
        check(!state.isFinished) { "La partida ya terminó." }
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        check(state.drawPile.isNotEmpty()) { "No quedan fichas para robar." }
        val tile = state.drawPile.first()
        val players = state.players.map { if (it.id == player.id) it.copy(hand = it.hand + tile) else it }
        return state.copy(players = players, drawPile = state.drawPile.drop(1),
            events = EventManager.append(state.events, EventManager.create("TILE_DRAWN",
                "${player.name} robó una ficha.", player.id)))
    }

    fun passTurn(state: GameState, playerId: String): GameState {
        check(!state.isFinished) { "La partida ya terminó." }
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        check(state.drawPile.isEmpty()) { "Si quedan fichas, el jugador debe robar antes de pasar." }
        check(player.hand.none { MoveValidator.canPlay(it, state) }) { "El jugador tiene una ficha válida y no puede pasar." }
        val next = nextIndex(state, state.currentPlayerIndex)
        val newState = state.copy(currentPlayerIndex = next,
            events = EventManager.append(state.events, EventManager.create("TURN_PASSED",
                "${player.name} pasó el turno.", player.id)))
        return if (newState.players.all { p -> p.hand.none { MoveValidator.canPlay(it, newState) } }) {
            // La ronda queda bloqueada; gana quien tenga menos puntos en la mano.
            val winner = newState.players.minByOrNull { p -> p.hand.sumOf(Tile::points) }
            val updatedPlayers = if (winner == null) newState.players else newState.players.map {
                if (it.id == winner.id) it.copy(score = it.score + newState.players.filterNot { p -> p.id == winner.id }
                    .sumOf { p -> p.hand.sumOf(Tile::points) }) else it
            }
            newState.copy(players = updatedPlayers, isFinished = true, winnerId = winner?.id,
                events = EventManager.append(newState.events, EventManager.create("GAME_BLOCKED",
                    "La partida quedó bloqueada; ganó ${winner?.name ?: "nadie"}.", winner?.id)))
        } else newState
    }

    private fun nextIndex(state: GameState, from: Int): Int =
        (from + state.direction + state.players.size) % state.players.size
}
