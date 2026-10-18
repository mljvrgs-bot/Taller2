package com.caos.dominocaos.data

import com.caos.dominocaos.logic.model.GameState
import com.caos.dominocaos.logic.model.Player
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

/**
 * Persistencia y sincronizacion de partidas en Realtime Database.
 * El codigo de la sala es el id de la partida: games/{codigo}.
 */
class FirebaseGameRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val games = database.reference.child("games")

    companion object {
        const val MAX_PLAYERS = 4
    }

    fun saveGame(state: GameState, onResult: (Result<Unit>) -> Unit = {}) {
        games.child(state.gameId).setValue(state)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun loadGame(gameId: String, onResult: (Result<GameState?>) -> Unit) {
        games.child(gameId).get()
            .addOnSuccessListener { onResult(Result.success(readGame(it))) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Escucha cambios en tiempo real. Devuelve una funcion para dejar de escuchar. */
    fun observeGame(
        gameId: String,
        onChange: (GameState?) -> Unit,
        onError: (DatabaseError) -> Unit = {}
    ): () -> Unit {
        val ref = games.child(gameId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = onChange(readGame(snapshot))
            override fun onCancelled(error: DatabaseError) = onError(error)
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    fun deleteGame(gameId: String, onResult: (Result<Unit>) -> Unit = {}) {
        games.child(gameId).removeValue()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Crea una sala con el anfitrion dentro y devuelve el codigo. Reintenta si el codigo ya existe. */
    fun createRoom(host: Player, onResult: (Result<String>) -> Unit, attemptsLeft: Int = 5) {
        val code = newCode()
        games.child(code).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    if (attemptsLeft > 1) createRoom(host, onResult, attemptsLeft - 1)
                    else onResult(Result.failure(IllegalStateException("No se pudo generar un codigo libre.")))
                } else {
                    val room = GameState(gameId = code, players = listOf(host), hostId = host.id)
                    saveGame(room) { result -> onResult(result.map { code }) }
                }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /**
     * Entra a una sala. Usa una transaccion para que dos jugadores que entran a la vez
     * no se pisen entre si.
     */
    fun joinGame(code: String, player: Player, onResult: (Result<Unit>) -> Unit) {
        var failure: String? = null
        games.child(code).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                failure = null
                // Si aun no hay dato local, Firebase reintenta con el dato real.
                val game = try {
                    currentData.getValue(GameState::class.java)
                } catch (e: Exception) {
                    null
                } ?: return Transaction.success(currentData)

                when {
                    game.players.any { it.id == player.id } -> return Transaction.success(currentData)
                    game.started -> { failure = "La partida ya empezo."; return Transaction.abort() }
                    game.players.size >= MAX_PLAYERS -> { failure = "La sala esta llena."; return Transaction.abort() }
                }
                currentData.value = game.copy(players = game.players + player)
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                when {
                    error != null -> onResult(Result.failure(error.toException()))
                    failure != null -> onResult(Result.failure(IllegalStateException(failure)))
                    snapshot == null || !snapshot.exists() ->
                        onResult(Result.failure(IllegalStateException("No existe una partida con ese codigo.")))
                    else -> onResult(Result.success(Unit))
                }
            }
        })
    }

    /** Lee una partida sin romper la app si los datos tienen un formato viejo. */
    private fun readGame(snapshot: DataSnapshot): GameState? =
        try {
            snapshot.getValue(GameState::class.java)
        } catch (e: Exception) {
            null
        }

    /** Codigo de 5 caracteres sin letras confusas (sin 0, O, 1, I). */
    private fun newCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..5).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }
}