package com.matthew.impostorapp.data.repository

import com.matthew.impostorapp.data.local.dao.CategoryDao
import com.matthew.impostorapp.data.local.dao.WordDao
import com.matthew.impostorapp.data.local.entity.CategoryEntity
import com.matthew.impostorapp.data.local.entity.WordEntity
import com.matthew.impostorapp.domain.model.Word

class WordRepository(
    private val categoryDao: CategoryDao,
    private val wordDao: WordDao
) {

    suspend fun getCategories(): List<String> =
        categoryDao.getAll().map { it.name }

    suspend fun getWords(): List<Word> =
        wordDao.getAll().map {
            Word(
                value = it.value,
                category = "",
                normalizedValue = it.normalizedValue
            )
        }

    suspend fun addCategory(name: String) {
        categoryDao.insert(CategoryEntity(name = name))
    }

    suspend fun addWord(value: String, categoryId: Long) {
        wordDao.insert(
            WordEntity(
                value = value,
                normalizedValue = value.lowercase(),
                categoryId = categoryId
            )
        )
    }
}
