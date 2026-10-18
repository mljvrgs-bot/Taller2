package com.caos.dominocaos.logic.model

data class Player(
    val id: String = "",
    val name: String = "",
    val hand: List<Tile> = emptyList(),
    val score: Int = 0,
    val bot: Boolean = false,
    val powers: Map<String, Int> = defaultPowers(),
    val powersUsed: Int = 0
) {
    fun usesLeft(power: PowerType): Int = powers[power.name] ?: 0

    companion object {
        const val DEFAULT_USES = 2

        fun defaultPowers(uses: Int = DEFAULT_USES): Map<String, Int> =
            PowerType.usable().associate { it.name to uses }
    }
}