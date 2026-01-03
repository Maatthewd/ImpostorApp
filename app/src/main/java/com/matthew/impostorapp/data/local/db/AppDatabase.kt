package com.matthew.impostorapp.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.matthew.impostorapp.data.local.dao.CategoryDao
import com.matthew.impostorapp.data.local.dao.WordDao
import com.matthew.impostorapp.data.local.entity.CategoryEntity
import com.matthew.impostorapp.data.local.entity.WordEntity
import com.matthew.impostorapp.data.seed.SeedLoader

@Database(
    entities = [CategoryEntity::class, WordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun wordDao(): WordDao

    suspend fun preloadSeedIfNeeded(
        context: Context,
        db: AppDatabase
    ) {
        if (db.categoryDao().getAll().isNotEmpty()) return

        val loader = SeedLoader(context)

        val categories = loader.loadCategories()
        val words = loader.loadWords()

        categories.forEach { categoryName ->
            val categoryId = db.categoryDao()
                .insert(CategoryEntity(name = categoryName))

            words[categoryName]?.forEach { value ->
                db.wordDao().insert(
                    WordEntity(
                        value = value,
                        normalizedValue = value.trim().lowercase(),
                        categoryId = categoryId
                    )
                )
            }
        }
    }


}
