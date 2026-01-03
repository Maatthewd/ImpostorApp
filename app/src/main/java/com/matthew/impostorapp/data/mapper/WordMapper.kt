package com.matthew.impostorapp.data.mapper

import com.matthew.impostorapp.data.local.entity.WordEntity
import com.matthew.impostorapp.domain.model.Word

fun WordEntity.toDomain(categoryName: String): Word =
    Word(
        value = value,
        category = categoryName,
        normalizedValue = normalizedValue
    )
