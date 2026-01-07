package com.matthew.impostorapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.matthew.impostorapp.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    // ===== OBSERVABLES CON FLOW =====

    /**
     * Observa todas las palabras de una categoría
     */
    @Query("SELECT * FROM words WHERE categoryId = :categoryId ORDER BY value ASC")
    fun observeByCategory(categoryId: Long): Flow<List<WordEntity>>

    /**
     * Observa el conteo de palabras por categoría
     */
    @Query("SELECT COUNT(*) FROM words WHERE categoryId = :categoryId")
    fun observeCountByCategory(categoryId: Long): Flow<Int>

    // ===== OPERACIONES DIRECTAS =====

    @Query("SELECT * FROM words")
    suspend fun getAll(): List<WordEntity>

    @Query("SELECT * FROM words WHERE categoryId = :categoryId ORDER BY value ASC")
    suspend fun getByCategory(categoryId: Long): List<WordEntity>

    @Insert
    suspend fun insert(word: WordEntity)

    @Query("DELETE FROM words WHERE id = :wordId")
    suspend fun deleteById(wordId: Long)

    @Query("DELETE FROM words WHERE categoryId = :categoryId")
    suspend fun deleteByCategory(categoryId: Long)

    @Query("SELECT COUNT(*) FROM words WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("DELETE FROM words WHERE normalizedValue = :normalizedValue AND categoryId = :categoryId")
    suspend fun deleteByValueAndCategory(normalizedValue: String, categoryId: Long)
}