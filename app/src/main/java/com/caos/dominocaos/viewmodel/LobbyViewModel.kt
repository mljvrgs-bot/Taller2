package com.caos.dominocaos.viewmodel

import androidx.lifecycle.ViewModel
import com.caos.dominocaos.data.FirebaseAuthRepository
import com.caos.dominocaos.data.FirebaseGameRepository
import com.caos.dominocaos.logic.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LobbyUiState(val loading: Boolean = false, val error: String? = null)

/** Crear sala o unirse con codigo. */
class LobbyViewModel : ViewModel() {

    private val auth = FirebaseAuthRepository()
    private val repo = FirebaseGameRepository()

    private val _ui = MutableStateFlow(LobbyUiState())
    val ui: StateFlow<LobbyUiState> = _ui.asStateFlow()

    fun userLabel(): String = auth.currentUser()?.email?.substringBefore('@') ?: "jugador"

    /** El jugador que entra a la sala: su id es el uid de Firebase Auth. */
    private fun me(): Player? = auth.currentUser()?.let { Player(id = it.uid, name = userLabel()) }

    fun create(onDone: (String) -> Unit) {
        val player = me() ?: return fail("Inicia sesion primero.")
        _ui.value = LobbyUiState(loading = true)
        repo.createRoom(player, { result ->
            result
                .onSuccess { code -> _ui.value = LobbyUiState(); onDone(code) }
                .onFailure { fail(it.message ?: "No se pudo crear la partida.") }
        })
    }

    fun join(rawCode: String, onDone: (String) -> Unit) {
        val code = rawCode.trim().uppercase()
        if (code.length != 5) return fail("El codigo tiene 5 caracteres.")
        val player = me() ?: return fail("Inicia sesion primero.")
        _ui.value = LobbyUiState(loading = true)
        repo.joinGame(code, player) { result ->
            result
                .onSuccess { _ui.value = LobbyUiState(); onDone(code) }
                .onFailure { fail(it.message ?: "No se pudo entrar a la partida.") }
        }
    }

    private fun fail(message: String) {
        _ui.value = LobbyUiState(error = message)
    }
}