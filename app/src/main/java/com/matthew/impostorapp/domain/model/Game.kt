package com.matthew.impostorapp.domain.model

data class Game(
    val players: List<Player>,
    val word: Word,
    val currentPlayerIndex: Int,
    val state: GameState,
    val config: GameConfig,
    val round: Int,
    val usedWords: Set<String> = emptySet()
)
