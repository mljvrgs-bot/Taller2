package com.caos.dominocaos.logic.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomEventsTest {

    private val base = game(
        listOf(tile(0, 1)), listOf(tile(0, 2)),
        pile = listOf(tile(1, 1), tile(2, 2), tile(3, 3))
    )

    @Test
    fun `la tormenta hace que todos roben una ficha`() {
        val s = RandomEvents.apply(base, RandomEventType.STORM)
        assertTrue(s.players.all { it.hand.size == 2 })
        assertEquals(1, s.drawPile.size)
    }

    @Test
    fun `la tormenta no falla si el pozo tiene pocas fichas`() {
        val s = RandomEvents.apply(base.copy(drawPile = listOf(tile(1, 1))), RandomEventType.STORM)
        assertEquals(3, s.players.sumOf { it.hand.size })
        assertTrue(s.drawPile.isEmpty())
    }

    @Test
    fun `puntos dobles activa la regla temporal`() {
        assertTrue(RandomEvents.apply(base, RandomEventType.DOUBLE_POINTS).doublePointsNextTurn)
    }

    @Test
    fun `turno perdido pasa el turno al otro jugador`() {
        assertEquals(1, RandomEvents.apply(base, RandomEventType.LOST_TURN).currentPlayerIndex)
    }

    @Test
    fun `lluvia de puntos suma 5 al jugador en turno`() {
        assertEquals(
            RandomEvents.BONUS_POINTS,
            RandomEvents.apply(base, RandomEventType.BONUS).players[0].score
        )
    }

    @Test
    fun `el evento queda registrado en la partida`() {
        val s = RandomEvents.apply(base, RandomEventType.BONUS)
        assertEquals("EVENT_BONUS", s.events.last().type)
        assertEquals(base.turn, s.lastEventTurn)
    }

    @Test
    fun `no hay eventos antes del turno 2`() {
        val engine = GameEngine(Random(1), eventChance = 100)
        val s = engine.playTile(
            game(listOf(tile(4, 5), tile(1, 1)), listOf(tile(6, 6))),
            "a",
            tile(4, 5).id
        )
        assertTrue(s.events.none { it.type.startsWith("EVENT_") })
    }

    @Test
    fun `con probabilidad 100 ocurre un evento pasado el turno 2`() {
        val engine = GameEngine(Random(1), eventChance = 100)
        val start = game(
            listOf(tile(4, 5), tile(5, 6), tile(6, 1), tile(1, 2)),
            listOf(tile(5, 5), tile(5, 0), tile(0, 0)),
            pile = List(5) { tile(1, 1) }
        ).copy(turn = 2)
        val s = engine.playTile(start, "a", tile(4, 5).id)
        assertTrue(s.events.any { it.type.startsWith("EVENT_") })
    }

    @Test
    fun `no hay eventos seguidos`() {
        val engine = GameEngine(Random(1), eventChance = 100)
        val start = game(
            listOf(tile(4, 5), tile(5, 6)),
            listOf(tile(5, 5), tile(5, 0)),
            pile = List(5) { tile(1, 1) }
        ).copy(turn = 4, lastEventTurn = 3)
        val s = engine.playTile(start, "a", tile(4, 5).id)
        assertTrue(s.events.none { it.type.startsWith("EVENT_") })
    }
}