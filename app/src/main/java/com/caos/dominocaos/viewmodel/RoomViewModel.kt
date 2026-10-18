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
    val error: String? = null
) {
    val isHost: Boolean get() = game != null && game.hostId == myId
    /** El anfitrion puede iniciar con 2 o mas jugadores y mientras la sala no haya empezado. */
    val canStart: Boolean get() = isHost && game != null && !game.started && game.players.size >= 2
}

/** Sala de espera: muestra los jugadores y deja iniciar al anfitrion. */
class RoomViewModel : ViewModel() {

    private val repo = FirebaseGameRepository()
    private val engine = GameEngine()

    private val _ui = MutableStateFlow(RoomUiState(myId = FirebaseAuthRepository().currentUser()?.uid ?: ""))
    val ui: StateFlow<RoomUiState> = _ui.asStateFlow()

    private var stopObserving: (() -> Unit)? = null

    fun observe(code: String) {
        if (stopObserving != null) return
        stopObserving = repo.observeGame(
            gameId = code,
            onChange = { game ->
                _ui.update {
                    if (game == null) it.copy(error = "La sala ya no existe.") else it.copy(game = game, error = null)
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

    override fun onCleared() {
        stopObserving?.invoke()
        super.onCleared()
    }
}