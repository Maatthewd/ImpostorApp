package com.matthew.impostorapp.data.local.db

import android.content.Context
import android.util.Log
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

/**
 * Proveedor singleton de la base de datos con inicialización asíncrona.
 *
 * Enfoque profesional sin bloqueos:
 * - La BD se crea inmediatamente
 * - El seed corre en background
 * - El ViewModel usa Flow para reaccionar cuando los datos estén listos
 */
object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val SEED_VERSION = 1
    private const val TAG = "DatabaseProvider"
    private const val DB_NAME = "impostor_db"

    /**
     * Obtiene la instancia de la base de datos.
     * Retorna inmediatamente, el seed corre en background.
     */
    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = INSTANCE
            if (instance != null) {
                return@synchronized instance
            }

            Log.d(TAG, "🔵 Creating database instance")

            val newInstance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context.applicationContext))
                .build()

            INSTANCE = newInstance
            Log.d(TAG, "✅ Database instance created")

            newInstance
        }
    }

    /**
     * Callback personalizado para manejar onCreate y onOpen
     */
    private class DatabaseCallback(
        private val appContext: Context
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d(TAG, "🟢 onCreate - First time database creation")

            // Ejecutar seed en background (no bloquea)
            applicationScope.launch {
                try {
                    INSTANCE?.let { database ->
                        performInitialSeed(appContext, database)
                        saveSeedVersion(appContext, SEED_VERSION)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in onCreate seed", e)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d(TAG, "🟡 onOpen - Database opened")

            // Solo verificar actualizaciones si la BD ya existía
            applicationScope.launch {
                try {
                    INSTANCE?.let { database ->
                        val savedVersion = getSeedVersion(appContext)
                        // Solo actualizar si ya hay una versión guardada (BD no es nueva)
                        if (savedVersion > 0) {
                            checkAndUpdateSeed(appContext, database)
                        } else {
                            Log.d(TAG, "⏭️ Skipping onOpen check - onCreate will handle seed")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in onOpen check", e)
                }
            }
        }
    }

    /**
     * Ejecuta el seed inicial de la base de datos.
     * Se llama solo la primera vez que se crea la BD.
     */
    private suspend fun performInitialSeed(context: Context, db: AppDatabase) {
        Log.d(TAG, "📥 Starting initial seed...")
        val startTime = System.currentTimeMillis()

        try {
            val loader = SeedLoader(context)
            val categories = loader.loadCategories()
            val words = loader.loadWords()

            Log.d(TAG, "📦 Loaded ${categories.size} categories from JSON")

            categories.forEach { categoryName ->
                Log.d(TAG, "  ➕ Inserting: $categoryName")

                val categoryId = db.categoryDao()
                    .insert(CategoryEntity(name = categoryName))

                val categoryWords = words[categoryName] ?: emptyList()
                Log.d(TAG, "     └─ ${categoryWords.size} words")

                categoryWords.forEach { value ->
                    db.wordDao().insert(
                        WordEntity(
                            value = value,
                            normalizedValue = value.trim().lowercase(),
                            categoryId = categoryId
                        )
                    )
                }
            }

            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Initial seed completed in ${duration}ms")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in initial seed", e)
            throw e
        }
    }

    /**
     * Verifica si hay actualizaciones disponibles y las aplica.
     * Se ejecuta cada vez que se abre la app (excepto la primera vez).
     */
    private suspend fun checkAndUpdateSeed(context: Context, db: AppDatabase) {
        try {
            val savedVersion = getSeedVersion(context)
            Log.d(TAG, "📊 Seed version - Saved: $savedVersion, Current: $SEED_VERSION")

            if (savedVersion < SEED_VERSION) {
                Log.d(TAG, "🔄 Update needed, applying incremental changes...")
                performIncrementalUpdate(context, db, savedVersion)
                saveSeedVersion(context, SEED_VERSION)
            } else {
                val count = db.categoryDao().getAll().size
                Log.d(TAG, "✅ Database up to date ($count categories)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking seed version", e)
        }
    }

    /**
     * Aplica actualizaciones incrementales según la versión guardada.
     * Permite agregar nuevas categorías/palabras sin borrar datos del usuario.
     */
    private suspend fun performIncrementalUpdate(
        context: Context,
        db: AppDatabase,
        fromVersion: Int
    ) {
        Log.d(TAG, "🔄 Applying updates from version $fromVersion to $SEED_VERSION")
        val startTime = System.currentTimeMillis()

        try {
            val loader = SeedLoader(context)
            val categories = loader.loadCategories()
            val words = loader.loadWords()

            val existingCategories = db.categoryDao().getAll()
            val existingCategoryNames = existingCategories.map { it.name }.toSet()

            // Agregar nuevas categorías
            val newCategories = categories.filter { it !in existingCategoryNames }
            Log.d(TAG, "  📦 New categories to add: ${newCategories.size}")

            newCategories.forEach { categoryName ->
                Log.d(TAG, "  ➕ Adding new category: $categoryName")
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

            // Actualizar categorías existentes con nuevas palabras
            val existingToUpdate = categories.filter { it in existingCategoryNames }
            Log.d(TAG, "  🔄 Existing categories to check: ${existingToUpdate.size}")

            existingToUpdate.forEach { categoryName ->
                val category = existingCategories.first { it.name == categoryName }
                val existingWords = db.wordDao()
                    .getByCategory(category.id)
                    .map { it.normalizedValue }
                    .toSet()

                val newWords = words[categoryName]
                    ?.filter { it.trim().lowercase() !in existingWords }
                    ?: emptyList()

                if (newWords.isNotEmpty()) {
                    Log.d(TAG, "  ➕ Adding ${newWords.size} new words to $categoryName")
                    newWords.forEach { value ->
                        db.wordDao().insert(
                            WordEntity(
                                value = value,
                                normalizedValue = value.trim().lowercase(),
                                categoryId = category.id
                            )
                        )
                    }
                }
            }

            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Incremental update completed in ${duration}ms")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in incremental update", e)
            throw e
        }
    }

    /**
     * Obtiene la versión guardada del seed
     */
    private fun getSeedVersion(context: Context): Int {
        val prefs = context.getSharedPreferences("impostor_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("seed_version", 0)
    }

    /**
     * Guarda la versión actual del seed
     */
    private fun saveSeedVersion(context: Context, version: Int) {
        val prefs = context.getSharedPreferences("impostor_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("seed_version", version).apply()
        Log.d(TAG, "💾 Saved seed version: $version")
    }

    /**
     * Limpia la instancia (útil para testing)
     */
    @Synchronized
    fun closeDatabase() {
        INSTANCE?.close()
        INSTANCE = null
        Log.d(TAG, "🔴 Database closed")
    }
}