package com.caos.dominocaos.logic.model

enum class PowerType {
    NONE,
    WILDCARD,
    SKIP,
    SWAP,
    REVERSE;

    companion object {
        fun usable(): List<PowerType> = entries.filter { it != NONE }
    }
}