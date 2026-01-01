package com.matthew.impostorapp.domain.model

import java.util.UUID

data class Player(
    val id: UUID = UUID.randomUUID(),
    val role: Role,
    val word: String? = null,
    val isAlive: Boolean = true
)
