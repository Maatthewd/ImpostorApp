package com.matthew.impostorapp.domain.model

data class Word(
    val value: String,
    val category: String,
    val normalizedValue: String = value.trim().lowercase()
) {

    fun matchesCategoryMode(mode: CategoryMode): Boolean =
        when (mode) {
            is CategoryMode.Mixed -> true
            is CategoryMode.Single -> category == mode.category
            is CategoryMode.Multiple -> category in mode.categories
        }

}
