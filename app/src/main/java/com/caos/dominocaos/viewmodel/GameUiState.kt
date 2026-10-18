package com.caos.dominocaos.viewmodel

import com.caos.dominocaos.logic.engine.BoardSide
import com.caos.dominocaos.logic.engine.MoveValidator
import com.caos.dominocaos.logic.model.GameState
import com.caos.dominocaos.logic.model.Player
import com.caos.dominocaos.logic.model.PowerType

/**
 * Lo que la pantalla necesita para dibujarse. La UI solo lee estas propiedades,
 * asi no repite reglas del juego (todo se calcula con el motor).
 */
data class GameUiState(
    val game: GameState,
    val myId: String,
    val selectedTileId: String? = null,
    val message: String? = null
) {
    val me: Player get() = game.players.first { it.id == myId }
    val opponents: List<Player> get() = game.players.filter { it.id != myId }
    val winner: Player? get() = game.players.firstOrNull { it.id == game.winnerId }

    val isMyTurn: Boolean get() = !game.finished && game.currentPlayer?.id == myId

    /** Fichas que se pueden tocar ahora (con Intercambio activo, todas las mias). */
    val playableIds: Set<String>
        get() = when {
            !isMyTurn -> emptySet()
            game.activePower == PowerType.SWAP -> me.hand.map { it.id }.toSet()
            else -> me.hand.filter { MoveValidator.canPlay(it, game) }.map { it.id }.toSet()
        }

    /** Extremos donde se puede colocar la ficha seleccionada. */
    val highlightSides: Set<BoardSide>
        get() {
            val tile = me.hand.firstOrNull { it.id == selectedTileId } ?: return emptySet()
            return MoveValidator.validSides(tile, game)
        }

    val canDraw: Boolean
        get() = isMyTurn && game.drawPile.isNotEmpty() && !MoveValidator.hasNaturalMove(me.hand, game)

    val canPass: Boolean
        get() = isMyTurn && game.drawPile.isEmpty() && !MoveValidator.hasNaturalMove(me.hand, game)

    /** Texto de ayuda segun el poder activo. */
    val hint: String?
        get() = when (game.activePower) {
            PowerType.SWAP -> "Toca la ficha que quieres cambiar"
            PowerType.WILDCARD -> "Comodin activo: cualquier ficha sirve"
            PowerType.SKIP -> "Salto activo: el siguiente pierde su turno"
            PowerType.REVERSE -> "Inversion activa: cambia el sentido"
            PowerType.NONE -> null
        }
}