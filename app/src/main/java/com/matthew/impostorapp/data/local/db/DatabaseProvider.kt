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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Incrementá este número cada vez que actualices los JSONs
    private const val SEED_VERSION = 2

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
                        applicationScope.launch {
                            INSTANCE?.let { database ->
                                preloadData(context, database)
                                saveSeedVersion(context, SEED_VERSION)
                            }
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        applicationScope.launch {
                            INSTANCE?.let { database ->
                                val savedVersion = getSeedVersion(context)
                                if (savedVersion < SEED_VERSION) {
                                    // Hay nuevos datos en el JSON
                                    updateData(context, database)
                                    saveSeedVersion(context, SEED_VERSION)
                                }
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
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun updateData(context: Context, db: AppDatabase) {
        try {
            val loader = SeedLoader(context)
            val categories = loader.loadCategories()
            val words = loader.loadWords()

            val existingCategories = db.categoryDao().getAll()

            categories.forEach { categoryName ->
                // Buscar si la categoría ya existe
                val existingCategory = existingCategories.find { it.name == categoryName }

                val categoryId = if (existingCategory != null) {
                    existingCategory.id
                } else {
                    // Crear nueva categoría
                    db.categoryDao().insert(CategoryEntity(name = categoryName))
                }

                // Obtener palabras existentes de esta categoría
                val existingWords = db.wordDao()
                    .getByCategory(categoryId)
                    .map { it.normalizedValue }
                    .toSet()

                // Agregar solo palabras nuevas
                words[categoryName]?.forEach { value ->
                    val normalized = value.trim().lowercase()
                    if (normalized !in existingWords) {
                        db.wordDao().insert(
                            WordEntity(
                                value = value,
                                normalizedValue = normalized,
                                categoryId = categoryId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSeedVersion(context: Context): Int {
        val prefs = context.getSharedPreferences("impostor_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("seed_version", 0)
    }

    private fun saveSeedVersion(context: Context, version: Int) {
        val prefs = context.getSharedPreferences("impostor_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("seed_version", version).apply()
    }
}