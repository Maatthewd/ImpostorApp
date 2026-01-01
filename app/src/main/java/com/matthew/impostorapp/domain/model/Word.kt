package com.matthew.impostorapp.domain.model

import java.util.UUID

data class Word(
    val value: String,
    val categoryId: UUID
)
