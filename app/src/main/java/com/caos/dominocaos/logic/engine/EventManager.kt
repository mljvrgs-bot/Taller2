package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.GameEvent
import java.util.UUID

/** Crea eventos de partida y mantiene el historial acotado (ultimos 100). */
object EventManager {
    fun create(type: String, message: String, playerId: String? = null): GameEvent =
        GameEvent(
            id = UUID.randomUUID().toString(),
            type = type,
            message = message,
            playerId = playerId
        )

    fun append(events: List<GameEvent>, event: GameEvent, maxEvents: Int = 100): List<GameEvent> =
        (events + event).takeLast(maxEvents)
}
