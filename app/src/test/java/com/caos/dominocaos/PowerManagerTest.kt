package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.Player
import com.caos.dominocaos.logic.model.PowerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PowerManagerTest {

    private val engine = GameEngine(Random(1), eventChance = 0)

    private fun threePlayers() = game(
        listOf(tile(4, 5), tile(4, 4)), listOf(tile(6, 6)), listOf(tile(2, 2))
    )

    @Test
    fun `activar un poder descuenta un uso y cancelar lo devuelve`() {
        val active = engine.activatePower(threePlayers(), "a", PowerType.SKIP)
        assertEquals(Player.DEFAULT_USES - 1, active.players[0].usesLeft(PowerType.SKIP))
        assertEquals(1, active.players[0].powersUsed)

        val canceled = engine.cancelPower(active, "a")
        assertEquals(Player.DEFAULT_USES, canceled.players[0].usesLeft(PowerType.SKIP))
        assertEquals(0, canceled.players[0].powersUsed)
        assertEquals(PowerType.NONE, canceled.activePower)
    }

    @Test
    fun `no se pueden activar dos poderes a la vez`() {
        val active = engine.activatePower(threePlayers(), "a", PowerType.SKIP)
        assertFails("ya hay un poder activo") { engine.activatePower(active, "a", PowerType.REVERSE) }
    }

    @Test
    fun `sin usos restantes no se puede activar el poder`() {
        val s = threePlayers()
        val noUses = s.copy(players = s.players.map {
            if (it.id == "a") it.copy(powers = it.powers + (PowerType.SKIP.name to 0)) else it
        })
        assertFails("no quedan usos") { engine.activatePower(noUses, "a", PowerType.SKIP) }
    }

    @Test
    fun `solo el jugador en turno puede activar poderes`() {
        assertFails("le toca a A") { engine.activatePower(threePlayers(), "b", PowerType.SKIP) }
    }

    @Test
    fun `el comodin permite jugar una ficha que no coincide`() {
        val start = game(listOf(tile(0, 1), tile(5, 5)), listOf(tile(6, 6)))
        val s = engine.playTile(engine.activatePower(start, "a", PowerType.WILDCARD), "a", tile(0, 1).id, BoardSide.RIGHT)
        assertEquals(2, s.board.size)
        assertEquals(PowerType.NONE, s.activePower)
    }

    @Test
    fun `sin comodin la misma ficha es rechazada`() {
        val start = game(listOf(tile(0, 1), tile(5, 5)), listOf(tile(6, 6)))
        assertFails("0|1 no coincide") { engine.playTile(start, "a", tile(0, 1).id) }
    }

    @Test
    fun `el salto hace que el siguiente jugador pierda el turno`() {
        val s = engine.playTile(engine.activatePower(threePlayers(), "a", PowerType.SKIP), "a", tile(4, 5).id)
        assertEquals(2, s.currentPlayerIndex) // salta a B y juega C
    }

    @Test
    fun `la inversion cambia el sentido de los turnos`() {
        val s = engine.playTile(engine.activatePower(threePlayers(), "a", PowerType.REVERSE), "a", tile(4, 5).id)
        assertEquals(-1, s.direction)
        assertEquals(2, s.currentPlayerIndex)
    }

    @Test
    fun `el intercambio cambia una ficha propia por una del rival`() {
        val start = game(listOf(tile(0, 1), tile(0, 2)), listOf(tile(6, 6), tile(5, 5)))
        val active = engine.activatePower(start, "a", PowerType.SWAP)
        val s = engine.swapTile(active, "a", tile(0, 1).id, "b")

        assertEquals(2, s.players[0].hand.size)
        assertTrue(s.players[1].hand.any { it.id == tile(0, 1).id })
        assertTrue(s.players[0].hand.none { it.id == tile(0, 1).id })
        assertEquals(PowerType.NONE, s.activePower)
    }

    @Test
    fun `con un intercambio pendiente no se puede jugar`() {
        val start = game(listOf(tile(0, 1), tile(4, 5)), listOf(tile(6, 6)))
        val active = engine.activatePower(start, "a", PowerType.SWAP)
        assertFails("primero resuelve el intercambio") { engine.playTile(active, "a", tile(4, 5).id) }
    }
}