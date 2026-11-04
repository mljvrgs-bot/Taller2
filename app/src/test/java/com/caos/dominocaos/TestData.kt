package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.GameState
import com.caos.dominocaos.logic.model.Player
import com.caos.dominocaos.logic.model.Tile
import org.junit.Assert.assertTrue

/** Atajos para armar estados de prueba sin repetir codigo. */

fun tile(left: Int, right: Int) = Tile(left, right)

/** Partida de prueba: tablero con la ficha 3|4 (extremos 3 y 4) y 2 o 3 jugadores (a, b, c). */
fun game(
    a: List<Tile>,
    b: List<Tile>,
    c: List<Tile>? = null,
    pile: List<Tile> = emptyList()
): GameState {
    val players = listOfNotNull(Player("a", "A", a), Player("b", "B", b), c?.let { Player("c", "C", it) })
    return GameState(
        gameId = "test", players = players, drawPile = pile,
        board = listOf(tile(3, 4)), leftEnd = 3, rightEnd = 4
    )
}

/** Deja al jugador con el puntaje indicado. */
fun GameState.withScore(playerId: String, score: Int): GameState =
    copy(players = players.map { if (it.id == playerId) it.copy(score = score) else it })

/** Verifica que el bloque lance una excepcion (el motor rechaza la accion). */
fun assertFails(message: String, block: () -> Unit) {
    assertTrue(message, runCatching(block).isFailure)
}