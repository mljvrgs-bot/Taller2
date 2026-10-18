package com.caos.dominocaos.logic.engine

import com.caos.dominocaos.logic.model.GameState
import com.caos.dominocaos.logic.model.PowerType
import kotlin.random.Random

object PowerManager {

    fun activate(state: GameState, playerId: String, power: PowerType): GameState {
        check(!state.finished) { "La partida ya termino." }
        require(power != PowerType.NONE) { "Elige un poder valido." }
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        check(state.activePower == PowerType.NONE) { "Ya hay un poder activo en este turno." }
        check(player.usesLeft(power) > 0) { "No te quedan usos de ese poder." }
        if (power == PowerType.SWAP) {
            check(state.players.any { it.id != playerId && it.hand.isNotEmpty() }) {
                "Ningun oponente tiene fichas para intercambiar."
            }
            check(player.hand.isNotEmpty()) { "No tienes fichas para intercambiar." }
        }
        return applyUses(state, playerId, delta = -1, power = power).copy(
            activePower = power,
            events = EventManager.append(
                state.events,
                EventManager.create("POWER_ACTIVATED", "${player.name} activo ${label(power)}.", player.id)
            )
        )
    }

    fun cancel(state: GameState, playerId: String): GameState {
        val player = state.currentPlayer ?: error("No hay jugador activo.")
        check(player.id == playerId) { "No es el turno de este jugador." }
        check(state.activePower != PowerType.NONE) { "No hay un poder activo." }
        return applyUses(state, playerId, delta = +1, power = state.activePower)
            .copy(activePower = PowerType.NONE)
    }

    fun swap(
        state: GameState,
        playerId: String,
        ownTileId: String,
        opponentId: String,
        random: Random = Random.Default
    ): GameState {
        check(state.activePower == PowerType.SWAP) { "Primero activa el poder Intercambio." }
        val me = state.currentPlayer ?: error("No hay jugador activo.")
        check(me.id == playerId) { "No es el turno de este jugador." }
        check(opponentId != playerId) { "Elige a un oponente." }
        val opponent = state.players.firstOrNull { it.id == opponentId } ?: error("El oponente no existe.")
        check(opponent.hand.isNotEmpty()) { "El oponente no tiene fichas." }
        val own = me.hand.firstOrNull { it.id == ownTileId } ?: error("La ficha no esta en tu mano.")
        val taken = opponent.hand.random(random)

        val players = state.players.map {
            when (it.id) {
                me.id -> it.copy(hand = it.hand.filterNot { t -> t.id == own.id } + taken)
                opponent.id -> it.copy(hand = it.hand.filterNot { t -> t.id == taken.id } + own)
                else -> it
            }
        }
        return state.copy(
            players = players,
            activePower = PowerType.NONE,
            events = EventManager.append(
                state.events,
                EventManager.create("POWER_SWAP", "${me.name} intercambio una ficha con ${opponent.name}.", me.id)
            )
        )
    }

    fun label(power: PowerType): String = when (power) {
        PowerType.NONE -> "ningun poder"
        PowerType.WILDCARD -> "Comodin"
        PowerType.SKIP -> "Salto"
        PowerType.SWAP -> "Intercambio"
        PowerType.REVERSE -> "Inversion"
    }

    private fun applyUses(state: GameState, playerId: String, delta: Int, power: PowerType): GameState =
        state.copy(players = state.players.map { p ->
            if (p.id != playerId || power == PowerType.NONE) p
            else p.copy(
                powers = p.powers + (power.name to p.usesLeft(power) + delta),
                powersUsed = p.powersUsed - delta
            )
        })
}