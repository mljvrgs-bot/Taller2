package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.GameState
import com.caos.dominocaos.logic.model.Player
import com.caos.dominocaos.logic.model.PowerType

/**
 * Efectos de poderes opcionales de Domino Caos.
 * La regla base del dominó no depende de activar poderes.
 */
object PowerManager {
    fun apply(power: PowerType, state: GameState, actor: Player): GameState {
        val event = when (power) {
            PowerType.NONE -> EventManager.create("POWER", "${actor.name} no activó un poder.", actor.id)
            PowerType.DRAW_TWO -> {
                val updatedPlayers = state.players.mapIndexed { index, player ->
                    if (index == state.currentPlayerIndex) {
                        player.copy(hand = player.hand + state.drawPile.take(2))
                    } else player
                }
                val amount = minOf(2, state.drawPile.size)
                val remaining = state.drawPile.drop(amount)
                return state.copy(
                    players = updatedPlayers,
                    drawPile = remaining,
                    events = EventManager.append(state.events,
                        EventManager.create("POWER_DRAW_TWO", "${actor.name} robó $amount ficha(s) por un poder.", actor.id))
                )
            }
            PowerType.REVERSE_TURN -> {
                return state.copy(
                    direction = -state.direction,
                    events = EventManager.append(state.events,
                        EventManager.create("POWER_REVERSE", "${actor.name} invirtió el sentido del turno.", actor.id))
                )
            }
            PowerType.SKIP_TURN -> {
                val next = nextIndex(state, state.currentPlayerIndex)
                val afterSkip = nextIndex(state, next)
                return state.copy(
                    currentPlayerIndex = afterSkip,
                    events = EventManager.append(state.events,
                        EventManager.create("POWER_SKIP", "${actor.name} saltó el turno de un jugador.", actor.id))
                )
            }
            PowerType.DOUBLE_POINTS -> {
                return state.copy(
                    doublePointsNextTurn = true,
                    events = EventManager.append(state.events,
                        EventManager.create("POWER_DOUBLE_POINTS", "${actor.name} activó puntos dobles para la próxima puntuación.", actor.id))
                )
            }
        }
        return state.copy(events = EventManager.append(state.events, event))
    }

    private fun nextIndex(state: GameState, from: Int): Int {
        if (state.players.isEmpty()) return 0
        return (from + state.direction + state.players.size) % state.players.size
    }
}
