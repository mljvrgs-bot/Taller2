package com.caos.dominocaos.data

import com.caos.dominocaos.logic.model.GameState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Persistencia básica de partidas. La base de datos debe tener reglas seguras
 * que permitan leer/escribir solo a usuarios autenticados y autorizados.
 */
class FirebaseGameRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val games = database.reference.child("games")

    fun saveGame(state: GameState, onResult: (Result<Unit>) -> Unit = {}) {
        games.child(state.gameId).setValue(state)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun loadGame(gameId: String, onResult: (Result<GameState?>) -> Unit) {
        games.child(gameId).get()
            .addOnSuccessListener { snapshot ->
                onResult(Result.success(snapshot.getValue(GameState::class.java)))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun observeGame(gameId: String, onChange: (GameState?) -> Unit, onError: (DatabaseError) -> Unit = {}) {
        games.child(gameId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChange(snapshot.getValue(GameState::class.java))
            }
            override fun onCancelled(error: DatabaseError) = onError(error)
        })
    }

    fun deleteGame(gameId: String, onResult: (Result<Unit>) -> Unit = {}) {
        games.child(gameId).removeValue()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}
