package com.matthew.impostorapp.domain.model

data class GameConfig(
    val players: Int,
    val impostors: Int,
    val categoryMode: CategoryMode
)
