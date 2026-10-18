package com.caos.dominocaos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caos.dominocaos.data.FirebaseAuthRepository
import com.caos.dominocaos.data.FirebaseGameRepository
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
 * Un solo ViewModel para dos modos:
 *  - Offline (code == null): el jugador contra un bot.
 *  - Online (code != null): la partida vive en Firebase; cada jugada se guarda y
 *    el otro celular la recibe por el observador.
 */
class GameViewModel : ViewModel() {

    private val engine = GameEngine()
    private val repo = FirebaseGameRepository()

    // null hasta que start() carga la partida (la pantalla muestra "Cargando").
    private val _ui = MutableStateFlow<GameUiState?>(null)
    val ui: StateFlow<GameUiState?> = _ui.asStateFlow()

    private var gameCode: String? = null
    private var initialized = false
    private var stopObserving: (() -> Unit)? = null
    private var botJob: Job? = null

    /** Se llama una sola vez desde la pantalla. code = null significa modo offline. */
    fun start(code: String?) {
        if (initialized) return
        initialized = true
        gameCode = code

        if (code == null) {
            _ui.value = newOfflineState()
            return
        }
        val uid = FirebaseAuthRepository().currentUser()?.uid ?: return
        stopObserving = repo.observeGame(
            gameId = code,
            onChange = { game ->
                // Cada cambio remoto (jugada del rival, reinicio...) reemplaza el estado local.
                if (game != null && game.players.any { it.id == uid }) {
                    _ui.update { cur -> cur?.copy(game = game) ?: GameUiState(game = game, myId = uid) }
                }
            },
            onError = { e -> _ui.update { it?.copy(message = "Error de conexion: ${e.message}") } }
        )
    }

    private fun newOfflineState(): GameUiState {
        val game = engine.newGame(listOf("me" to "Tu", "bot" to "Rival"))
        val withBot = game.copy(players = game.players.map { if (it.id == "bot") it.copy(bot = true) else it })
        return GameUiState(game = withBot, myId = "me")
    }

    /**
     * Aplica una accion del motor. Si el motor la rechaza se muestra el mensaje y nada cambia.
     * Online: se actualiza el estado local al instante y se guarda en Firebase.
     */
    private fun act(block: (GameState, String) -> GameState) {
        val current = _ui.value ?: return
        try {
            val next = block(current.game, current.myId)
            _ui.value = current.copy(game = next, selectedTileId = null, message = null)
            if (gameCode == null) {
                runBotsIfNeeded()
            } else {
                repo.saveGame(next) { result ->
                    result.onFailure { e -> _ui.update { it?.copy(message = "No se pudo guardar: ${e.message}") } }
                }
            }
        } catch (e: RuntimeException) {
            _ui.value = current.copy(message = e.message ?: "Accion no permitida")
        }
    }

    // ---- Acciones del jugador ----

    fun onTileClick(tile: Tile) {
        val s = _ui.value ?: return
        if (!s.isMyTurn) return
        val game = s.game

        // Con Intercambio activo, la ficha tocada es la que se entrega.
        if (game.activePower == PowerType.SWAP) {
            val opponent = s.opponents.first() // TODO: elegir oponente si hay mas de 2 jugadores
            act { g, me -> engine.swapTile(g, me, tile.id, opponent.id) }
            return
        }
        if (s.selectedTileId == tile.id) {
            _ui.value = s.copy(selectedTileId = null)
            return
        }
        val sides = MoveValidator.validSides(tile, game)
        when {
            sides.isEmpty() -> _ui.value = s.copy(message = "Esa ficha no se puede jugar")
            // Un solo lado posible (o tablero vacio): se juega directo.
            game.board.isEmpty() || sides.size == 1 -> act { g, me -> engine.playTile(g, me, tile.id, sides.first()) }
            // Dos lados posibles: se selecciona y el jugador toca el "+" del lado que quiere.
            else -> _ui.value = s.copy(selectedTileId = tile.id)
        }
    }

    fun onSideClick(side: BoardSide) {
        val tileId = _ui.value?.selectedTileId ?: return
        act { g, me -> engine.playTile(g, me, tileId, side) }
    }

    /** Tocar un poder lo activa; tocar el poder ya activo lo cancela. */
    fun onPowerClick(power: PowerType) {
        val s = _ui.value ?: return
        if (!s.isMyTurn) return
        if (power == s.game.activePower) act { g, me -> engine.cancelPower(g, me) }
        else act { g, me -> engine.activatePower(g, me, power) }
    }

    fun onDraw() = act { g, me -> engine.drawTile(g, me) }

    fun onPass() = act { g, me -> engine.passTurn(g, me) }

    fun restart() {
        val s = _ui.value ?: return
        val code = gameCode
        if (code == null) {
            botJob?.cancel()
            _ui.value = newOfflineState()
            return
        }
        // Online: solo el anfitrion reinicia, para evitar dos mezclas distintas a la vez.
        if (s.game.hostId != s.myId) {
            _ui.value = s.copy(message = "Solo el anfitrion puede reiniciar")
            return
        }
        val fresh = engine.newGame(s.game.players.map { it.id to it.name }, gameId = code)
            .copy(hostId = s.game.hostId, started = true)
        _ui.value = s.copy(game = fresh, selectedTileId = null)
        repo.saveGame(fresh)
    }

    fun dismissMessage() = _ui.update { it?.copy(message = null) }

    override fun onCleared() {
        stopObserving?.invoke()
        super.onCleared()
    }

    // ---- Bot (solo offline) ----

    private fun runBotsIfNeeded() {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            while (true) {
                val game = _ui.value?.game ?: break
                val current = game.currentPlayer
                if (game.finished || current == null || !current.bot) break
                delay(800)
                try {
                    _ui.update { it?.copy(game = botMove(it.game, current.id)) }
                } catch (e: RuntimeException) {
                    break
                }
            }
        }
    }

    /** Estrategia simple: primera ficha valida; si no hay, roba; si no puede, pasa. */
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