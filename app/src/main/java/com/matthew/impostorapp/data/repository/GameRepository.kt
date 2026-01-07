package com.matthew.impostorapp.data.repository

import android.util.Log
import com.matthew.impostorapp.data.local.dao.CategoryDao
import com.matthew.impostorapp.data.local.dao.WordDao
import com.matthew.impostorapp.data.local.entity.CategoryEntity
import com.matthew.impostorapp.data.local.entity.WordEntity
import com.matthew.impostorapp.domain.model.Word
import com.matthew.impostorapp.utils.StringSimilarity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GameRepository(
    private val categoryDao: CategoryDao,
    private val wordDao: WordDao
) {

    companion object {
        /**
         * Umbral de similitud para detectar nombres parecidos.
         * 0.X significa X% de similitud.
         */
        private const val SIMILARITY_THRESHOLD = 0.70
    }

    // ===== OBSERVABLES CON FLOW =====

    /**
     * Flow reactivo de categorías. Cualquier cambio en la BD
     * se propaga automáticamente a los observers.
     */
    fun observeCategories(): Flow<List<String>> =
        categoryDao.observeAll().map { entities ->
            entities.map { it.name }.also { names ->
                Log.d("GameRepository", "📊 Categories emitted: ${names.size} - $names")
            }
        }

    /**
     * Flow reactivo de palabras por categoría
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeWordsByCategory(categoryName: String): Flow<List<String>> =
        categoryDao.observeByName(categoryName).flatMapLatest { category ->
            if (category != null) {
                wordDao.observeByCategory(category.id).map { words ->
                    words.map { it.value }
                }
            } else {
                flowOf(emptyList())
            }
        }

    /**
     * Flow reactivo del conteo de palabras por categoría
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeWordCount(categoryName: String): Flow<Int> =
        categoryDao.observeByName(categoryName).flatMapLatest { category ->
            if (category != null) {
                wordDao.observeCountByCategory(category.id)
            } else {
                flowOf(0)
            }
        }

    // ===== LECTURA DIRECTA (para lógica de juego) =====

    suspend fun getCategories(): List<String> =
        categoryDao.getAll().map { it.name }

    suspend fun getWords(): List<Word> =
        wordDao.getAll().map { wordEntity ->
            val category = categoryDao.getAll()
                .first { it.id == wordEntity.categoryId }

            Word(
                value = wordEntity.value,
                category = category.name,
                normalizedValue = wordEntity.normalizedValue
            )
        }

    suspend fun getWordsByCategory(categoryName: String): List<String> {
        val category = categoryDao.getByName(categoryName) ?: return emptyList()
        return wordDao.getByCategory(category.id).map { it.value }
    }

    suspend fun getWordCount(categoryName: String): Int {
        val category = categoryDao.getByName(categoryName) ?: return 0
        return wordDao.countByCategory(category.id)
    }

    // ===== AGREGAR =====

    suspend fun addCategory(name: String): Result<Unit> {
        return try {
            val trimmed = name.trim()

            if (trimmed.isEmpty()) {
                return Result.failure(Exception("El nombre no puede estar vacío"))
            }

            // Obtener todas las categorías existentes
            val allCategories = categoryDao.getAll()
            val existingNames = allCategories.map { it.name }

            // Buscar categorías similares (tanto iguales como con errores tipográficos)
            val similarCategories = StringSimilarity.findSimilar(
                target = trimmed,
                candidates = existingNames,
                threshold = SIMILARITY_THRESHOLD
            )

            if (similarCategories.isNotEmpty()) {
                val similar = similarCategories.first()

                // Calcular la similitud exacta para el mensaje
                val similarityScore = StringSimilarity.similarity(
                    StringSimilarity.normalizeText(trimmed),
                    StringSimilarity.normalizeText(similar)
                )

                val message = when {
                    similarityScore >= 0.95 ->
                        "Ya existe la categoría: '$similar'"
                    else ->
                        "Ya existe una categoría similar: '$similar'\n¿Quisiste decir esa?"
                }

                return Result.failure(Exception(message))
            }

            // Si no hay similares, agregar la nueva categoría
            categoryDao.insert(CategoryEntity(name = trimmed))
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWord(categoryName: String, word: String): Result<Unit> {
        return try {
            val category = categoryDao.getByName(categoryName)
                ?: return Result.failure(Exception("Categoría no encontrada"))

            val normalized = word.trim().lowercase()

            if (normalized.isEmpty()) {
                return Result.failure(Exception("La palabra no puede estar vacía"))
            }

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
                // Room maneja el CASCADE automáticamente
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