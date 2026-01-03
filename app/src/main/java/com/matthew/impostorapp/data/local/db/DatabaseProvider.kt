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

    // IMPORTANTE: Solo incrementá este número cuando cambies los JSONs
    // NO lo cambies cuando agregues datos desde la app
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
            val categoriesFromJson = loader.loadCategories()
            val wordsFromJson = loader.loadWords()

            // CORREGIDO: Obtener categorías existentes por nombre
            val existingCategories = db.categoryDao().getAll()
            val existingCategoryNames = existingCategories.map { it.name }.toSet()

            categoriesFromJson.forEach { categoryName ->
                // CORREGIDO: Solo procesar si la categoría existe
                // Si no existe, el usuario la eliminó y no la queremos recrear
                val categoryEntity = existingCategories.find { it.name == categoryName }

                val categoryId = if (categoryEntity != null) {
                    // La categoría ya existe, usar su ID
                    categoryEntity.id
                } else if (categoryName !in existingCategoryNames) {
                    // Es una categoría NUEVA del JSON, agregarla
                    db.categoryDao().insert(CategoryEntity(name = categoryName))
                } else {
                    // La categoría fue eliminada por el usuario, skip
                    return@forEach
                }

                // Obtener palabras existentes de esta categoría
                val existingWords = db.wordDao()
                    .getByCategory(categoryId)
                    .map { it.normalizedValue }
                    .toSet()

                // Agregar solo palabras nuevas del JSON
                wordsFromJson[categoryName]?.forEach { value ->
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