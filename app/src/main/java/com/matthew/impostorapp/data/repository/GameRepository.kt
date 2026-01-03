package com.matthew.impostorapp.data.repository

import com.matthew.impostorapp.data.local.dao.CategoryDao
import com.matthew.impostorapp.data.local.dao.WordDao
import com.matthew.impostorapp.data.local.entity.CategoryEntity
import com.matthew.impostorapp.data.local.entity.WordEntity
import com.matthew.impostorapp.domain.model.Word

class GameRepository(
    private val categoryDao: CategoryDao,
    private val wordDao: WordDao
) {

    // ===== LECTURA =====

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

    suspend fun getWordsByCategory(categoryName: String): List<String> {
        val category = categoryDao.getByName(categoryName) ?: return emptyList()
        return wordDao.getByCategory(category.id).map { it.value }
    }

    // ===== AGREGAR =====

    suspend fun addCategory(name: String): Result<Unit> {
        return try {
            val existing = categoryDao.getByName(name)
            if (existing != null) {
                Result.failure(Exception("La categoría ya existe"))
            } else {
                categoryDao.insert(CategoryEntity(name = name))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWord(categoryName: String, word: String): Result<Unit> {
        return try {
            val category = categoryDao.getByName(categoryName)
                ?: return Result.failure(Exception("Categoría no encontrada"))

            val normalized = word.trim().lowercase()
            val existingWords = wordDao.getByCategory(category.id)

            if (existingWords.any { it.normalizedValue == normalized }) {
                Result.failure(Exception("La palabra ya existe en esta categoría"))
            } else {
                wordDao.insert(
                    WordEntity(
                        value = word.trim(),
                        normalizedValue = normalized,
                        categoryId = category.id
                    )
                )
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== ELIMINAR =====

    suspend fun deleteCategory(categoryName: String, force: Boolean = false): Result<Unit> {
        return try {
            val category = categoryDao.getByName(categoryName)
                ?: return Result.failure(Exception("Categoría no encontrada"))

            val wordCount = wordDao.countByCategory(category.id)

            if (wordCount > 0 && !force) {
                Result.failure(Exception("La categoría tiene $wordCount palabras asociadas"))
            } else {
                if (wordCount > 0) {
                    wordDao.deleteByCategory(category.id)
                }
                categoryDao.deleteById(category.id)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWord(categoryName: String, word: String): Result<Unit> {
        return try {
            val category = categoryDao.getByName(categoryName)
                ?: return Result.failure(Exception("Categoría no encontrada"))

            val normalized = word.trim().lowercase()
            wordDao.deleteByValueAndCategory(normalized, category.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}