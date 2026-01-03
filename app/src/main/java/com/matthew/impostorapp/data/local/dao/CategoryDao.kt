package com.matthew.impostorapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.matthew.impostorapp.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long)

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): CategoryEntity?
}