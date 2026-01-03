package com.matthew.impostorapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.matthew.impostorapp.data.local.entity.WordEntity

@Dao
interface WordDao {

    @Query("SELECT * FROM words")
    suspend fun getAll(): List<WordEntity>

    @Query("SELECT * FROM words WHERE categoryId = :categoryId")
    suspend fun getByCategory(categoryId: Long): List<WordEntity>

    @Insert
    suspend fun insert(word: WordEntity)
}
