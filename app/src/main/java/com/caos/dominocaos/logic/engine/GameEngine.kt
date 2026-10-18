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
        require(playerNames.map { it.first }.toSet().size == playerNames.size) { "Los identificadores deben ser unicos." }
        require(handSize > 0) { "El tamano de la mano debe ser positivo." }

        val shuffled = Tile.doubleSixSet().shuffled(random)
        val players = playerNames.mapIndexed { index, pair ->
            Player(id = pair.first, name = pair.second, hand = shuffled.drop(index * handSize).take(handSize))
        }
        val used = minOf(playerNames.size * handSize, shuffled.size)
        return GameState(
            gameId = gameId,
            players = players,
            drawPile = shuffled.drop(used),
            events = listOf(EventManager.create("GAME_STARTED", "Comenzo una partida con ${players.size} jugadores."))
        )
    }

    // ---- Poderes (delegan en PowerManager) ----
    fun activatePower(state: GameState, playerId: String, power: PowerType): GameState =
        PowerManager.activate(state, playerId, power)

    fun cancelPower(state: GameState, playerId: String): GameState =
        PowerManager.cancel(state, playerId)

    fun swapTile(state: GameState, playerId: String, ownTileId: String, opponentId: String): GameState =
        PowerManager.swap(state, playerId, ownTileId, opponentId, random)

    // ---- Jugadas ----
    fun playTile(state: GameState, playerId: String, tileId: String, side: BoardSide? = null): GameState {
        check(!state.finished) { "La partida ya termino." }
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        check(state.activePower != PowerType.SWAP) { "Termina o cancela el Intercambio antes de jugar." }
        val tile = player.hand.firstOrNull { it.id == tileId } ?: error("La ficha no esta en la mano del jugador.")
        check(MoveValidator.canPlay(tile, state)) { "La ficha no coincide con ninguno de los extremos." }

        // Lado por defecto: derecha, salvo que solo coincida con la izquierda.
        val actualSide = side ?: when {
            state.board.isEmpty() -> BoardSide.RIGHT
            tile.matches(state.rightEnd ?: -1) -> BoardSide.RIGHT
            tile.matches(state.leftEnd ?: -1) -> BoardSide.LEFT
            else -> BoardSide.RIGHT // solo ocurre con Comodin
        }
        check(actualSide in MoveValidator.validSides(tile, state)) { "La ficha no se puede colocar en ese extremo." }

        // Con Comodin la ficha puede no coincidir: entonces se deja tal cual.
        val oriented = when {
            state.board.isEmpty() -> tile
            actualSide == BoardSide.RIGHT -> tile.orientedToMatch(state.rightEnd!!) ?: tile
            else -> tile.orientedToMatchRight(state.leftEnd!!) ?: tile
        }
        val newBoard = when {
            state.board.isEmpty() -> listOf(oriented)
            actualSide == BoardSide.RIGHT -> state.board + oriented
            else -> listOf(oriented) + state.board
        }
        val newLeft = if (state.board.isEmpty() || actualSide == BoardSide.LEFT) oriented.left else state.leftEnd
        val newRight = if (state.board.isEmpty() || actualSide == BoardSide.RIGHT) oriented.right else state.rightEnd

        val remainingHand = player.hand.filterNot { it.id == tile.id }
        var players = state.players.map { if (it.id == player.id) it.copy(hand = remainingHand) else it }
        var events = EventManager.append(
            state.events,
            EventManager.create("TILE_PLAYED", "${player.name} jugo ${tile.left}|${tile.right}.", player.id)
        )

        if (remainingHand.isEmpty()) {
            val points = players.filterNot { it.id == player.id }.sumOf { o -> o.hand.sumOf(Tile::points) }
            val scored = if (state.doublePointsNextTurn) points * 2 else points
            players = players.map { if (it.id == player.id) it.copy(score = it.score + scored) else it }
            events = EventManager.append(
                events,
                EventManager.create("GAME_FINISHED", "${player.name} gano la ronda y obtuvo $scored puntos.", player.id)
            )
            return state.copy(
                players = players, board = newBoard, leftEnd = newLeft, rightEnd = newRight,
                finished = true, winnerId = player.id, events = events,
                doublePointsNextTurn = false, activePower = PowerType.NONE
            )
        }

        val moved = state.copy(
            players = players, board = newBoard, leftEnd = newLeft, rightEnd = newRight,
            events = events, doublePointsNextTurn = false
        )
        return advanceTurn(moved)
    }

    fun drawTile(state: GameState, playerId: String): GameState {
        check(!state.finished) { "La partida ya termino." }
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        check(state.drawPile.isNotEmpty()) { "No quedan fichas para robar." }
        check(!MoveValidator.hasNaturalMove(player.hand, state)) { "Tienes una ficha valida: no puedes robar." }
        val tile = state.drawPile.first()
        val players = state.players.map { if (it.id == player.id) it.copy(hand = it.hand + tile) else it }
        return state.copy(
            players = players,
            drawPile = state.drawPile.drop(1),
            events = EventManager.append(
                state.events,
                EventManager.create("TILE_DRAWN", "${player.name} robo una ficha.", player.id)
            )
        )
    }

    fun passTurn(state: GameState, playerId: String): GameState {
        check(!state.finished) { "La partida ya termino." }
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        check(state.drawPile.isEmpty()) { "Si quedan fichas, el jugador debe robar antes de pasar." }
        check(!MoveValidator.hasNaturalMove(player.hand, state)) { "El jugador tiene una ficha valida y no puede pasar." }

        val logged = state.copy(
            events = EventManager.append(
                state.events,
                EventManager.create("TURN_PASSED", "${player.name} paso el turno.", player.id)
            )
        )
        val newState = advanceTurn(logged)

        // Si nadie puede jugar, la ronda queda bloqueada: gana quien tenga menos puntos en mano.
        val blocked = newState.players.all { !MoveValidator.hasNaturalMove(it.hand, newState) }
        if (!blocked) return newState

        val winner = newState.players.minByOrNull { p -> p.hand.sumOf(Tile::points) }
        val updated = if (winner == null) newState.players else newState.players.map {
            if (it.id == winner.id) {
                it.copy(score = it.score + newState.players.filterNot { p -> p.id == winner.id }
                    .sumOf { p -> p.hand.sumOf(Tile::points) })
            } else it
        }
        return newState.copy(
            players = updated, finished = true, winnerId = winner?.id,
            events = EventManager.append(
                newState.events,
                EventManager.create("GAME_BLOCKED", "La partida quedo bloqueada; gano ${winner?.name ?: "nadie"}.", winner?.id)
            )
        )
    }

    private fun advanceTurn(state: GameState): GameState {
        val direction = if (state.activePower == PowerType.REVERSE) -state.direction else state.direction
        val steps = if (state.activePower == PowerType.SKIP) 2 else 1
        val n = state.players.size
        val next = ((state.currentPlayerIndex + direction * steps) % n + n) % n
        return state.copy(currentPlayerIndex = next, direction = direction, activePower = PowerType.NONE)
    }
}