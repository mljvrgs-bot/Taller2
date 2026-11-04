package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    // Sin eventos aleatorios para que los resultados sean predecibles.
    private val engine = GameEngine(Random(1), eventChance = 0)

    @Test
    fun `partida nueva reparte 7 fichas y deja 14 en el pozo`() {
        val g = engine.newGame(listOf("a" to "A", "b" to "B"))
        assertTrue(g.players.all { it.hand.size == 7 })
        assertEquals(14, g.drawPile.size)
    }

    @Test
    fun `no se puede crear una partida con un solo jugador`() {
        assertFails("un jugador no es suficiente") { engine.newGame(listOf("a" to "A")) }
    }

    @Test
    fun `jugar una ficha valida actualiza el extremo y pasa el turno`() {
        val s = engine.playTile(game(listOf(tile(4, 5), tile(1, 1)), listOf(tile(6, 6))), "a", tile(4, 5).id)
        assertEquals(5, s.rightEnd)
        assertEquals(1, s.currentPlayerIndex)
        assertEquals(1, s.players[0].hand.size)
    }

    @Test
    fun `una ficha que no coincide es rechazada`() {
        assertFails("1|2 no coincide con 3 ni 4") {
            engine.playTile(game(listOf(tile(1, 2)), listOf(tile(6, 6))), "a", tile(1, 2).id)
        }
    }

    @Test
    fun `solo juega quien tiene el turno`() {
        assertFails("le toca a A") {
            engine.playTile(game(listOf(tile(4, 5)), listOf(tile(3, 3))), "b", tile(3, 3).id)
        }
    }

    @Test
    fun `cada jugada suma los puntos de la ficha`() {
        val s = engine.playTile(game(listOf(tile(4, 5), tile(1, 1)), listOf(tile(6, 6))), "a", tile(4, 5).id)
        assertEquals(9, s.players[0].score)
    }

    @Test
    fun `con puntos dobles la jugada vale el doble`() {
        val start = game(listOf(tile(4, 5), tile(1, 1)), listOf(tile(6, 6))).copy(doublePointsNextTurn = true)
        val s = engine.playTile(start, "a", tile(4, 5).id)
        assertEquals(18, s.players[0].score)
        assertFalse(s.doublePointsNextTurn)
    }

    @Test
    fun `no se puede robar si hay una jugada valida`() {
        assertFails("tiene 4|5") {
            engine.drawTile(game(listOf(tile(4, 5)), listOf(tile(0, 0)), pile = listOf(tile(1, 1))), "a")
        }
    }

    @Test
    fun `sin jugada valida el jugador roba del pozo`() {
        val s = engine.drawTile(game(listOf(tile(0, 1)), listOf(tile(0, 0)), pile = listOf(tile(1, 1))), "a")
        assertEquals(2, s.players[0].hand.size)
        assertTrue(s.drawPile.isEmpty())
    }

    @Test
    fun `no se puede pasar mientras queden fichas en el pozo`() {
        assertFails("debe robar antes de pasar") {
            engine.passTurn(game(listOf(tile(0, 1)), listOf(tile(0, 2)), pile = listOf(tile(1, 1))), "a")
        }
    }

    @Test
    fun `si nadie puede jugar la partida queda bloqueada y termina`() {
        val s = engine.passTurn(game(listOf(tile(0, 1)), listOf(tile(0, 2))), "a")
        assertTrue(s.finished)
    }

    @Test
    fun `quedarse sin fichas da el bonus de las manos rivales y termina`() {
        // a juega 4|5 (+9) y recibe 6+6 + 1+2 = 15 de bonus
        val s = engine.playTile(game(listOf(tile(4, 5)), listOf(tile(6, 6), tile(1, 2))), "a", tile(4, 5).id)
        assertTrue(s.finished)
        assertEquals(24, s.players[0].score)
        assertEquals("a", s.winnerId)
    }

    @Test
    fun `gana el mayor puntaje aunque otro vacie la mano`() {
        val start = game(listOf(tile(4, 5)), listOf(tile(6, 6))).withScore("b", 100)
        val s = engine.playTile(start, "a", tile(4, 5).id)
        assertEquals("b", s.winnerId)
    }

    @Test
    fun `en empate gana quien uso menos poderes`() {
        val players = listOf(
            Player("a", "A", score = 10, powersUsed = 2),
            Player("b", "B", score = 10, powersUsed = 1)
        )
        assertEquals("b", engine.winnerOf(players)?.id)
    }

    @Test
    fun `empate total no tiene ganador`() {
        assertNull(engine.winnerOf(listOf(Player("a", "A", score = 5), Player("b", "B", score = 5))))
    }

    @Test
    fun `la partida termina al completar las rondas maximas`() {
        val start = game(
            listOf(tile(4, 5), tile(5, 6), tile(6, 6)),
            listOf(tile(5, 5), tile(5, 0), tile(0, 0))
        ).copy(maxRounds = 1)
        val afterA = engine.playTile(start, "a", tile(4, 5).id)
        assertFalse(afterA.finished)
        val afterB = engine.playTile(afterA, "b", tile(5, 5).id)
        assertTrue(afterB.finished)
        assertEquals(2, afterB.turn)
    }

    @Test
    fun `abandonar termina la partida y gana el rival`() {
        val s = engine.abandon(game(listOf(tile(4, 5)), listOf(tile(6, 6))), "a")
        assertTrue(s.finished)
        assertEquals("b", s.winnerId)
        assertEquals("ABANDONED", s.events.last().type)
    }

    @Test
    fun `con tres jugadores gana el mayor puntaje entre los que se quedan`() {
        val start = game(listOf(tile(4, 5)), listOf(tile(6, 6)), listOf(tile(2, 2)))
            .withScore("b", 5).withScore("c", 9).withScore("a", 50)
        assertEquals("c", engine.abandon(start, "a").winnerId)
    }

    @Test
    fun `abandonar una partida terminada no cambia nada`() {
        val done = game(listOf(tile(4, 5)), listOf(tile(6, 6))).copy(finished = true, winnerId = "a")
        assertEquals(done, engine.abandon(done, "b"))
    }
}