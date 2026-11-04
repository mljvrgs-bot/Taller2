package com.caos.dominocaos.viewmodel

import com.caos.dominocaos.logic.engine.game
import com.caos.dominocaos.logic.engine.tile
import com.caos.dominocaos.logic.model.GameEvent
import com.caos.dominocaos.logic.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUiStateTest {

    private fun ui(game: GameState, me: String = "a") = GameUiState(game = game, myId = me)

    @Test
    fun `playableIds solo incluye las fichas que coinciden en mi turno`() {
        val s = ui(game(listOf(tile(4, 5), tile(1, 2)), listOf(tile(6, 6))))
        assertEquals(setOf(tile(4, 5).id), s.playableIds)
    }

    @Test
    fun `playableIds esta vacio cuando no es mi turno`() {
        val s = ui(game(listOf(tile(4, 5)), listOf(tile(3, 3))), me = "b")
        assertTrue(s.playableIds.isEmpty())
        assertFalse(s.isMyTurn)
    }

    @Test
    fun `solo se puede robar sin jugada valida y con fichas en el pozo`() {
        assertTrue(
            ui(
                game(
                    listOf(tile(0, 1)),
                    listOf(tile(6, 6)),
                    pile = listOf(tile(1, 1))
                )
            ).canDraw
        )
        assertFalse(
            ui(
                game(
                    listOf(tile(4, 5)),
                    listOf(tile(6, 6)),
                    pile = listOf(tile(1, 1))
                )
            ).canDraw
        )
        assertFalse(ui(game(listOf(tile(0, 1)), listOf(tile(6, 6)))).canDraw)
    }

    @Test
    fun `solo se puede pasar con el pozo vacio y sin jugada valida`() {
        assertTrue(ui(game(listOf(tile(0, 1)), listOf(tile(6, 6)))).canPass)
        assertFalse(
            ui(
                game(
                    listOf(tile(0, 1)),
                    listOf(tile(6, 6)),
                    pile = listOf(tile(1, 1))
                )
            ).canPass
        )
    }

    @Test
    fun `la ronda avanza cada vuelta completa de turnos y no pasa del maximo`() {
        val g = game(listOf(tile(4, 5)), listOf(tile(6, 6)))
        assertEquals(1, ui(g.copy(turn = 0)).round)
        assertEquals(1, ui(g.copy(turn = 1)).round)
        assertEquals(2, ui(g.copy(turn = 2)).round)
        assertEquals(g.maxRounds, ui(g.copy(turn = 500)).round)
    }

    @Test
    fun `el evento pendiente se muestra hasta que el jugador lo cierra`() {
        val event = GameEvent(id = "e1", type = "EVENT_STORM", message = "Tormenta")
        val g = game(listOf(tile(4, 5)), listOf(tile(6, 6))).copy(events = listOf(event))

        assertEquals(event, ui(g).pendingEvent)
        assertNull(ui(g).copy(seenEventId = "e1").pendingEvent)
        assertNull(ui(g.copy(finished = true)).pendingEvent)
    }

    @Test
    fun `abandoned es true solo cuando el ultimo evento es un abandono`() {
        val g = game(listOf(tile(4, 5)), listOf(tile(6, 6)))
        assertFalse(ui(g).abandoned)
        val left = GameEvent(id = "x", type = "ABANDONED", message = "B abandono la partida.")
        assertTrue(ui(g.copy(events = listOf(left), finished = true)).abandoned)
    }
}