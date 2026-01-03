package com.matthew.impostorapp.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.matthew.impostorapp.data.local.entity.CategoryEntity
import com.matthew.impostorapp.data.local.entity.WordEntity
import com.matthew.impostorapp.data.seed.SeedLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "impostor_db"
            )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Cargar datos iniciales cuando se crea la DB por primera vez
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                preloadData(context, database)
                            }
                        }
                    }
                })
                .build()

            INSTANCE = instance
            instance
        }
    }

    private suspend fun preloadData(context: Context, db: AppDatabase) {
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