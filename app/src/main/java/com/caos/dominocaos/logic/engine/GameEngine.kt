package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.GameState
import com.caos.dominocaos.logic.model.Player
import com.caos.dominocaos.logic.model.PowerType
import com.caos.dominocaos.logic.model.Tile
import java.util.UUID
import kotlin.random.Random

class GameEngine(
    private val random: Random = Random.Default,
    private val eventChance: Int = DEFAULT_EVENT_CHANCE
) {
    companion object {
        const val DEFAULT_EVENT_CHANCE = 35
    }

    fun newGame(
        playerNames: List<Pair<String, String>>,
        handSize: Int = 7,
        gameId: String = UUID.randomUUID().toString()
    ): GameState {
        require(playerNames.size in 2..4) { "La partida debe tener entre 2 y 4 jugadores." }
        require(playerNames.map { it.first }
            .toSet().size == playerNames.size) { "Los identificadores deben ser unicos." }
        require(handSize > 0) { "El tamano de la mano debe ser positivo." }

        val shuffled = Tile.doubleSixSet().shuffled(random)
        val players = playerNames.mapIndexed { index, pair ->
            Player(
                id = pair.first,
                name = pair.second,
                hand = shuffled.drop(index * handSize).take(handSize)
            )
        }
        val used = minOf(playerNames.size * handSize, shuffled.size)
        return GameState(
            gameId = gameId,
            players = players,
            drawPile = shuffled.drop(used),
            events = listOf(
                EventManager.create(
                    "GAME_STARTED",
                    "Comenzo una partida con ${players.size} jugadores."
                )
            )
        )
    }

    // ---- Poderes (delegan en PowerManager) ----
    fun activatePower(state: GameState, playerId: String, power: PowerType): GameState =
        PowerManager.activate(state, playerId, power)

    fun cancelPower(state: GameState, playerId: String): GameState =
        PowerManager.cancel(state, playerId)

    fun swapTile(
        state: GameState,
        playerId: String,
        ownTileId: String,
        opponentId: String
    ): GameState =
        PowerManager.swap(state, playerId, ownTileId, opponentId, random)

    // ---- Jugadas ----
    fun playTile(
        state: GameState,
        playerId: String,
        tileId: String,
        side: BoardSide? = null
    ): GameState {
        check(!state.finished) { "La partida ya termino." }
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        check(state.activePower != PowerType.SWAP) { "Termina o cancela el Intercambio antes de jugar." }
        val tile = player.hand.firstOrNull { it.id == tileId }
            ?: error("La ficha no esta en la mano del jugador.")
        check(
            MoveValidator.canPlay(
                tile,
                state
            )
        ) { "La ficha no coincide con ninguno de los extremos." }

        val actualSide = side ?: when {
            state.board.isEmpty() -> BoardSide.RIGHT
            tile.matches(state.rightEnd ?: -1) -> BoardSide.RIGHT
            tile.matches(state.leftEnd ?: -1) -> BoardSide.LEFT
            else -> BoardSide.RIGHT
        }
        check(
            actualSide in MoveValidator.validSides(
                tile,
                state
            )
        ) { "La ficha no se puede colocar en ese extremo." }

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
        val newLeft =
            if (state.board.isEmpty() || actualSide == BoardSide.LEFT) oriented.left else state.leftEnd
        val newRight =
            if (state.board.isEmpty() || actualSide == BoardSide.RIGHT) oriented.right else state.rightEnd
        val remainingHand = player.hand.filterNot { it.id == tile.id }
        val gained = if (state.doublePointsNextTurn) tile.points * 2 else tile.points
        var players = state.players.map {
            if (it.id == player.id) it.copy(hand = remainingHand, score = it.score + gained) else it
        }
        var events = EventManager.append(
            state.events,
            EventManager.create(
                "TILE_PLAYED",
                "${player.name} jugo ${tile.left}|${tile.right} (+$gained).",
                player.id
            )
        )

        if (remainingHand.isEmpty()) {
            val bonus =
                players.filterNot { it.id == player.id }.sumOf { o -> o.hand.sumOf(Tile::points) }
            players =
                players.map { if (it.id == player.id) it.copy(score = it.score + bonus) else it }
            events = EventManager.append(
                events,
                EventManager.create(
                    "BONUS",
                    "${player.name} se quedo sin fichas y suma $bonus puntos extra.",
                    player.id
                )
            )
            return finish(
                state.copy(
                    players = players, board = newBoard, leftEnd = newLeft, rightEnd = newRight,
                    events = events, doublePointsNextTurn = false, activePower = PowerType.NONE
                )
            )
        }

        val moved = state.copy(
            players = players, board = newBoard, leftEnd = newLeft, rightEnd = newRight,
            events = events, doublePointsNextTurn = false
        )
        return afterTurn(advanceTurn(moved))
    }

    fun drawTile(state: GameState, playerId: String): GameState {
        check(!state.finished) { "La partida ya termino." }
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        check(state.drawPile.isNotEmpty()) { "No quedan fichas para robar." }
        check(
            !MoveValidator.hasNaturalMove(
                player.hand,
                state
            )
        ) { "Tienes una ficha valida: no puedes robar." }
        val tile = state.drawPile.first()
        val players =
            state.players.map { if (it.id == player.id) it.copy(hand = it.hand + tile) else it }
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
        check(
            !MoveValidator.hasNaturalMove(
                player.hand,
                state
            )
        ) { "El jugador tiene una ficha valida y no puede pasar." }

        val logged = state.copy(
            events = EventManager.append(
                state.events,
                EventManager.create("TURN_PASSED", "${player.name} paso el turno.", player.id)
            )
        )
        val newState = afterTurn(advanceTurn(logged))
        if (newState.finished) return newState
        val blocked = newState.players.all { !MoveValidator.hasNaturalMove(it.hand, newState) }
        if (!blocked) return newState
        val lowest = newState.players.minByOrNull { p -> p.hand.sumOf(Tile::points) }
        val updated = newState.players.map { p ->
            if (p.id != lowest?.id) p
            else p.copy(score = p.score + newState.players.filterNot { it.id == p.id }
                .sumOf { o -> o.hand.sumOf(Tile::points) })
        }
        return finish(
            newState.copy(
                players = updated,
                events = EventManager.append(
                    newState.events,
                    EventManager.create("GAME_BLOCKED", "La partida quedo bloqueada.")
                )
            )
        )
    }

    fun abandon(state: GameState, playerId: String): GameState {
        if (state.finished) return state
        val leaver = state.players.firstOrNull { it.id == playerId } ?: return state
        val winner = winnerOf(state.players.filter { it.id != playerId })
        val result = if (winner != null) "Gana ${winner.name}." else "Empate."
        return state.copy(
            finished = true,
            winnerId = winner?.id,
            activePower = PowerType.NONE,
            events = EventManager.append(
                state.events,
                EventManager.create(
                    "ABANDONED",
                    "${leaver.name} abandono la partida. $result",
                    playerId
                )
            )
        )
    }

    private fun finish(state: GameState): GameState {
        val winner = winnerOf(state.players)
        val message =
            if (winner != null) "Fin de la partida: gana ${winner.name} con ${winner.score} puntos."
            else "Fin de la partida: empate."
        return state.copy(
            finished = true,
            winnerId = winner?.id,
            activePower = PowerType.NONE,
            events = EventManager.append(
                state.events,
                EventManager.create("GAME_FINISHED", message, winner?.id)
            )
        )
    }

    fun winnerOf(players: List<Player>): Player? {
        val sorted =
            players.sortedWith(compareByDescending<Player> { it.score }.thenBy { it.powersUsed })
        val best = sorted.firstOrNull() ?: return null
        val tied = sorted.count { it.score == best.score && it.powersUsed == best.powersUsed }
        return if (tied > 1) null else best
    }

    private fun advanceTurn(state: GameState): GameState {
        val direction =
            if (state.activePower == PowerType.REVERSE) -state.direction else state.direction
        val steps = if (state.activePower == PowerType.SKIP) 2 else 1
        val n = state.players.size
        val next = ((state.currentPlayerIndex + direction * steps) % n + n) % n
        return state.copy(
            currentPlayerIndex = next,
            direction = direction,
            activePower = PowerType.NONE,
            turn = state.turn + 1
        )
    }

    private fun afterTurn(state: GameState): GameState {
        if (state.turn >= state.maxRounds * state.players.size) {
            return finish(
                state.copy(
                    events = EventManager.append(
                        state.events,
                        EventManager.create(
                            "MAX_ROUNDS",
                            "Se alcanzo el maximo de ${state.maxRounds} rondas."
                        )
                    )
                )
            )
        }
        return RandomEvents.maybeTrigger(state, random, eventChance)
    }
}