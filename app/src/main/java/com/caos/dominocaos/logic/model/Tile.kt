package com.caos.dominocaos.logic.model

import kotlin.math.max

/**
 * Ficha de dominó. El identificador es estable (por ejemplo, "6-3").
 * left/right conservan la orientación visual; los extremos coinciden por valor.
 */
data class Tile(val left: Int = 0, val right: Int = 0, val id: String = canonicalId(left, right)) {
    init {
        require(left in 0..6 && right in 0..6) { "Los valores de una ficha deben estar entre 0 y 6." }
    }

    val isDouble: Boolean get() = left == right
    val points: Int get() = left + right

    fun matches(value: Int): Boolean = left == value || right == value

    /** Orienta la ficha dejando el valor coincidente en el extremo izquierdo. */
    fun orientedToMatch(value: Int): Tile? = when {
        left == value -> this
        right == value -> Tile(right, left, id)
        else -> null
    }

    /** Orienta la ficha dejando el valor coincidente en el extremo derecho. */
    fun orientedToMatchRight(value: Int): Tile? = when {
        right == value -> this
        left == value -> Tile(right, left, id)
        else -> null
    }

    companion object {
        fun canonicalId(a: Int, b: Int): String = "${max(a, b)}-${minOf(a, b)}"

        /** Conjunto completo doble-seis: 28 fichas únicas. */
        fun doubleSixSet(): List<Tile> =
            (0..6).flatMap { a -> (a..6).map { b -> Tile(a, b) } }
    }
}
