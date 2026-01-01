package com.matthew.impostorapp.domain.model

import java.util.UUID

data class Category(
    val id: UUID = UUID.randomUUID(),
    val name: String
)
