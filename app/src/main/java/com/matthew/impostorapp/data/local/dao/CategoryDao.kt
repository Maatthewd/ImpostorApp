package com.matthew.impostorapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.matthew.impostorapp.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // ===== OBSERVABLES CON FLOW =====

    /**
     * Observa todos los cambios en categorías.
     * Se actualiza automáticamente cuando hay INSERT/UPDATE/DELETE
     */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    /**
     * Observa una categoría específica por nombre
     */
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    fun observeByName(name: String): Flow<CategoryEntity?>

    // ===== OPERACIONES DIRECTAS =====

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long)

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): CategoryEntity?
}