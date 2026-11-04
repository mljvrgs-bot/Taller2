package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.GameState
import kotlin.random.Random

enum class RandomEventType(val code: String, val title: String) {
    STORM("EVENT_STORM", "Tormenta de fichas"),        // fichas y pozo
    DOUBLE_POINTS("EVENT_DOUBLE", "Puntos dobles"),    // puntaje (regla temporal)
    LOST_TURN("EVENT_LOST_TURN", "Turno perdido"),     // turno
    BONUS("EVENT_BONUS", "Lluvia de puntos")           // puntaje
}

object RandomEvents {

    const val MIN_GAP = 3
    const val BONUS_POINTS = 5

    fun maybeTrigger(state: GameState, random: Random, chancePercent: Int): GameState {
        if (state.finished) return state
        if (state.turn < 2 || state.turn - state.lastEventTurn < MIN_GAP) return state
        if (random.nextInt(100) >= chancePercent) return state
        return apply(state, RandomEventType.entries.random(random))
    }

    fun apply(state: GameState, type: RandomEventType): GameState {
        val current = state.currentPlayer ?: return state
        val n = state.players.size

        val (changed, message) = when (type) {
            RandomEventType.STORM -> {
                var pile = state.drawPile
                val players = state.players.map { p ->
                    if (pile.isEmpty()) p else p.copy(hand = p.hand + pile.first())
                        .also { pile = pile.drop(1) }
                }
                state.copy(players = players, drawPile = pile) to
                        "Tormenta de fichas: todos los jugadores roban una ficha del pozo."
            }

            RandomEventType.DOUBLE_POINTS ->
                state.copy(doublePointsNextTurn = true) to
                        "Puntos dobles: la proxima jugada vale el doble."

            RandomEventType.LOST_TURN -> {
                val next = ((state.currentPlayerIndex + state.direction) % n + n) % n
                state.copy(currentPlayerIndex = next) to
                        "Turno perdido: ${current.name} pierde su turno."
            }

            RandomEventType.BONUS ->
                state.copy(players = state.players.map {
                    if (it.id == current.id) it.copy(score = it.score + BONUS_POINTS) else it
                }) to "Lluvia de puntos: ${current.name} recibe $BONUS_POINTS puntos."
        }
        return changed.copy(
            lastEventTurn = state.turn,
            events = EventManager.append(
                state.events,
                EventManager.create(type.code, message, current.id)
            )
        )
    }
}