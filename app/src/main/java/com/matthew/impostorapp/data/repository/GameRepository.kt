package com.matthew.impostorapp.data.repository

import com.matthew.impostorapp.data.local.dao.CategoryDao
import com.matthew.impostorapp.data.local.dao.WordDao
import com.matthew.impostorapp.domain.model.Word

class GameRepository(
    private val categoryDao: CategoryDao,
    private val wordDao: WordDao
) {

    suspend fun getCategories(): List<String> =
        categoryDao.getAll().map { it.name }

    suspend fun getWords(): List<Word> =
        wordDao.getAll().map {
            Word(
                value = it.value,
                category = categoryDao
                    .getAll()
                    .first { cat -> cat.id == it.categoryId }
                    .name,
                normalizedValue = it.normalizedValue
            )
        }
}
