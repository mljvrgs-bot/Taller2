package com.caos.dominocaos.viewmodel

import androidx.lifecycle.ViewModel
import com.caos.dominocaos.data.FirebaseAuthRepository
import com.caos.dominocaos.data.FirebaseGameRepository
import com.caos.dominocaos.logic.engine.GameEngine
import com.caos.dominocaos.logic.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RoomUiState(
    val game: GameState? = null,
    val myId: String = "",
    val error: String? = null,
    val closed: Boolean = false      // el anfitrion cerro la sala
) {
    val isHost: Boolean get() = game != null && game.hostId == myId

    /** El anfitrion puede iniciar con 2 o mas jugadores y mientras la sala no haya empezado. */
    val canStart: Boolean get() = isHost && game != null && !game.started && game.players.size >= 2
}

/** Sala de espera: muestra los jugadores, deja iniciar al anfitrion y permite salir. */
class RoomViewModel : ViewModel() {

    private val repo = FirebaseGameRepository()
    private val engine = GameEngine()

    private val _ui =
        MutableStateFlow(RoomUiState(myId = FirebaseAuthRepository().currentUser()?.uid ?: ""))
    val ui: StateFlow<RoomUiState> = _ui.asStateFlow()

    private var stopObserving: (() -> Unit)? = null
    private var roomCode: String? = null

    fun observe(code: String) {
        if (stopObserving != null) return
        roomCode = code
        stopObserving = repo.observeGame(
            gameId = code,
            onChange = { game ->
                _ui.update {
                    when {
                        game != null -> it.copy(game = game, error = null)
                        // La sala existia y desaparecio: la cerro el anfitrion (a el no le avisa, ya salio).
                        it.game != null && !it.isHost -> it.copy(game = null, closed = true)
                        else -> it
                    }
                }
            },
            onError = { e -> _ui.update { it.copy(error = "Error de conexion: ${e.message}") } }
        )
    }

    /** Reparte las fichas y marca la partida como iniciada. Los demas la reciben por el observador. */
    fun start() {
        val s = _ui.value
        val room = s.game ?: return
        if (!s.canStart) return
        val game = engine.newGame(room.players.map { it.id to it.name }, gameId = room.gameId)
            .copy(hostId = room.hostId, started = true)
        repo.saveGame(game) { result ->
            result.onFailure { e -> _ui.update { it.copy(error = "No se pudo iniciar: ${e.message}") } }
        }
    }

    /** Sale de la sala (el anfitrion la cierra) y luego ejecuta la navegacion. */
    fun leave(onDone: () -> Unit) {
        val code = roomCode
        val started = _ui.value.game?.started == true
        if (code != null && !started) repo.leaveRoom(code, _ui.value.myId)
        onDone()
    }

    override fun onCleared() {
        stopObserving?.invoke()
        super.onCleared()
    }
}