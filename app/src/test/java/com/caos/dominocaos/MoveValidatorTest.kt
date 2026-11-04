package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveValidatorTest {

    private val state = game(listOf(tile(4, 5)), listOf(tile(6, 6))) // extremos 3 y 4

    @Test
    fun `una ficha que coincide con un extremo se puede jugar`() {
        assertTrue(MoveValidator.canPlay(tile(4, 5), state))
        assertTrue(MoveValidator.canPlay(tile(3, 0), state))
    }

    @Test
    fun `una ficha que no coincide no se puede jugar`() {
        assertFalse(MoveValidator.canPlay(tile(1, 2), state))
    }

    @Test
    fun `con el tablero vacio cualquier ficha se puede jugar`() {
        assertTrue(MoveValidator.canPlay(tile(1, 2), GameState()))
    }

    @Test
    fun `los lados validos dependen de a que extremo coincide`() {
        assertEquals(setOf(BoardSide.RIGHT), MoveValidator.validSides(tile(4, 5), state))
        assertEquals(setOf(BoardSide.LEFT), MoveValidator.validSides(tile(3, 0), state))
        assertEquals(
            setOf(BoardSide.LEFT, BoardSide.RIGHT),
            MoveValidator.validSides(tile(3, 4), state)
        )
    }

    @Test
    fun `hasNaturalMove detecta si la mano tiene jugada`() {
        assertTrue(MoveValidator.hasNaturalMove(listOf(tile(1, 2), tile(4, 4)), state))
        assertFalse(MoveValidator.hasNaturalMove(listOf(tile(1, 2), tile(0, 0)), state))
    }
}