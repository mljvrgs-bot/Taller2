package com.caos.dominocaos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caos.dominocaos.logic.engine.BoardSide
import com.caos.dominocaos.logic.engine.GameEngine
import com.caos.dominocaos.logic.engine.MoveValidator
import com.caos.dominocaos.logic.model.GameState
import com.caos.dominocaos.logic.model.PowerType
import com.caos.dominocaos.logic.model.Tile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Version OFFLINE: el jugador local juega contra un bot.
 * Mas adelante este ViewModel se conecta al repositorio de Firebase;
 * la pantalla no cambia porque solo observa `ui`.
 */
class GameViewModel : ViewModel() {

    private val engine = GameEngine()
    private val myId = "me"
    private val botId = "bot"

    private val _ui = MutableStateFlow(newUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private var botJob: Job? = null

    private fun newUiState(): GameUiState {
        val game = engine.newGame(listOf(myId to "Tu", botId to "Rival"))
        val withBot = game.copy(players = game.players.map { if (it.id == botId) it.copy(bot = true) else it })
        return GameUiState(game = withBot, myId = myId)
    }

    /**
     * Ejecuta una accion del motor. Si el motor rechaza la jugada
     * (check/require lanzan excepcion) se muestra el mensaje y el estado no cambia.
     */
    private fun act(block: (GameState) -> GameState) {
        val current = _ui.value
        try {
            val next = block(current.game)
            _ui.value = current.copy(game = next, selectedTileId = null, message = null)
            runBotsIfNeeded()
        } catch (e: RuntimeException) {
            _ui.value = current.copy(message = e.message ?: "Accion no permitida")
        }
    }

    // ---- Acciones del jugador ----

    fun onTileClick(tile: Tile) {
        val s = _ui.value
        if (!s.isMyTurn) return
        val game = s.game

        // Con Intercambio activo, la ficha tocada es la que se entrega.
        if (game.activePower == PowerType.SWAP) {
            val opponent = s.opponents.first() // TODO: elegir oponente cuando haya mas de 2 jugadores
            act { engine.swapTile(it, myId, tile.id, opponent.id) }
            return
        }
        // Tocar de nuevo la ficha seleccionada la deselecciona.
        if (s.selectedTileId == tile.id) {
            _ui.value = s.copy(selectedTileId = null)
            return
        }
        val sides = MoveValidator.validSides(tile, game)
        when {
            sides.isEmpty() -> _ui.value = s.copy(message = "Esa ficha no se puede jugar")
            // Un solo lado posible (o tablero vacio): se juega directo.
            game.board.isEmpty() || sides.size == 1 -> act { engine.playTile(it, myId, tile.id, sides.first()) }
            // Dos lados posibles: se selecciona y el jugador toca el "+" del lado que quiere.
            else -> _ui.value = s.copy(selectedTileId = tile.id)
        }
    }

    fun onSideClick(side: BoardSide) {
        val tileId = _ui.value.selectedTileId ?: return
        act { engine.playTile(it, myId, tileId, side) }
    }

    /** Tocar un poder lo activa; tocar el poder ya activo lo cancela. */
    fun onPowerClick(power: PowerType) {
        val s = _ui.value
        if (!s.isMyTurn) return
        if (power == s.game.activePower) act { engine.cancelPower(it, myId) }
        else act { engine.activatePower(it, myId, power) }
    }

    fun onDraw() = act { engine.drawTile(it, myId) }

    fun onPass() = act { engine.passTurn(it, myId) }

    fun restart() {
        botJob?.cancel()
        _ui.value = newUiState()
    }

    fun dismissMessage() = _ui.update { it.copy(message = null) }

    // ---- Bot ----

    /** Mientras el turno sea del bot, juega con una pausa para que se note. */
    private fun runBotsIfNeeded() {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            while (true) {
                val game = _ui.value.game
                val current = game.currentPlayer
                if (game.finished || current == null || !current.bot) break
                delay(800)
                try {
                    _ui.update { it.copy(game = botMove(it.game, current.id)) }
                } catch (e: RuntimeException) {
                    break
                }
            }
        }
    }

    /** Estrategia simple: juega la primera ficha valida; si no hay, roba; si no puede, pasa. */
    private fun botMove(game: GameState, botId: String): GameState {
        val me = game.currentPlayer ?: return game
        val tile = me.hand.firstOrNull { MoveValidator.canPlay(it, game, wildcard = false) }
        return when {
            tile != null -> engine.playTile(game, botId, tile.id)
            game.drawPile.isNotEmpty() -> engine.drawTile(game, botId)
            else -> engine.passTurn(game, botId)
        }
    }
}