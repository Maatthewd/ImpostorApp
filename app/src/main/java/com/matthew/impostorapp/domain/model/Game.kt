package com.matthew.impostorapp.domain.model

data class Game(
    val players: List<Player>,
    val word: String,
    val currentPlayerIndex: Int,
    val state: GameState
)
