package com.matthew.impostorapp.domain.model

data class SavedStateGame(
    val config: GameConfig,
    val currentRound: Int,
    val usedWords: Set<String>,
    val totalRounds: Int
)
