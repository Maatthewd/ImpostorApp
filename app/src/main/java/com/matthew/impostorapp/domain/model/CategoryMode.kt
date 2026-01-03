package com.matthew.impostorapp.domain.model

sealed class CategoryMode {

    data object Mixed : CategoryMode()

    data class Single(val category: String) : CategoryMode()

    data class Multiple(val categories: Set<String>) : CategoryMode()
}
