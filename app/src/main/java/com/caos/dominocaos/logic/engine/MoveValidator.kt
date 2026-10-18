package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.GameState
import com.caos.dominocaos.logic.model.PowerType
import com.caos.dominocaos.logic.model.Tile

enum class BoardSide { LEFT, RIGHT }

object MoveValidator {

    fun canPlay(
        tile: Tile,
        state: GameState,
        wildcard: Boolean = state.activePower == PowerType.WILDCARD
    ): Boolean {
        if (state.finished) return false
        if (state.board.isEmpty()) return true
        if (wildcard) return true
        return tile.matches(state.leftEnd ?: return false) ||
                tile.matches(state.rightEnd ?: return false)
    }

    fun validSides(tile: Tile, state: GameState): Set<BoardSide> {
        if (!canPlay(tile, state)) return emptySet()
        if (state.board.isEmpty() || state.activePower == PowerType.WILDCARD) {
            return setOf(BoardSide.LEFT, BoardSide.RIGHT)
        }
        return buildSet {
            if (tile.matches(state.leftEnd ?: -1)) add(BoardSide.LEFT)
            if (tile.matches(state.rightEnd ?: -1)) add(BoardSide.RIGHT)
        }
    }

    fun hasNaturalMove(hand: List<Tile>, state: GameState): Boolean =
        hand.any { canPlay(it, state, wildcard = false) }
}