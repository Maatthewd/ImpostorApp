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
import java.util.concurrent.atomic.AtomicBoolean

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isSeeding = AtomicBoolean(false)

    private const val SEED_VERSION = 1
    private const val TAG = "DatabaseProvider"

    fun getDatabase(context: Context): AppDatabase {
        Log.d(TAG, "🔵 getDatabase called")

        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "impostor_db"
            )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Log.d(TAG, "🟢 onCreate called")

                        if (isSeeding.compareAndSet(false, true)) {
                            Log.d(TAG, "✅ Starting seed from onCreate")
                            applicationScope.launch {
                                try {
                                    INSTANCE?.let { database ->
                                        preloadData(context, database)
                                        saveSeedVersion(context, SEED_VERSION)
                                    }
                                } finally {
                                    isSeeding.set(false)
                                }
                            }
                        } else {
                            Log.d(TAG, "⏭️ Seed already in progress, skipping onCreate")
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        Log.d(TAG, "🟡 onOpen called")

                        // Solo ejecutar si NO se está haciendo seed
                        if (!isSeeding.get()) {
                            applicationScope.launch {
                                INSTANCE?.let { database ->
                                    val savedVersion = getSeedVersion(context)
                                    Log.d(TAG, "📊 Saved: $savedVersion, Current: $SEED_VERSION")

                                    if (savedVersion < SEED_VERSION) {
                                        Log.d(TAG, "🔄 Need update")
                                        updateData(context, database)
                                        saveSeedVersion(context, SEED_VERSION)
                                    } else {
                                        val count = database.categoryDao().getAll().size
                                        Log.d(TAG, "✅ Categories in DB: $count")
                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, "⏭️ Seed in progress, skipping onOpen check")
                        }
                    }
                })
                .build()

            INSTANCE = instance
            Log.d(TAG, "🔵 Database instance created")
            instance
        }
    }

    private suspend fun preloadData(context: Context, db: AppDatabase) {
        try {
            Log.d(TAG, "📥 Starting preloadData...")
            val loader = SeedLoader(context)
            val categories = loader.loadCategories()
            val words = loader.loadWords()

            Log.d(TAG, "📦 Loaded ${categories.size} categories from JSON")

            categories.forEach { categoryName ->
                Log.d(TAG, "➕ Inserting category: $categoryName")
                val categoryId = db.categoryDao()
                    .insert(CategoryEntity(name = categoryName))

                val categoryWords = words[categoryName] ?: emptyList()
                Log.d(TAG, "  └─ ${categoryWords.size} words for $categoryName")

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

            Log.d(TAG, "✅ preloadData completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in preloadData: ${e.message}", e)
        }
    }

    private suspend fun updateData(context: Context, db: AppDatabase) {
        try {
            Log.d(TAG, "🔄 Starting updateData...")
            val loader = SeedLoader(context)
            val categories = loader.loadCategories()
            val words = loader.loadWords()

            val existingCategories = db.categoryDao().getAll()
            Log.d(TAG, "📊 Existing: ${existingCategories.size}")

            categories.forEach { categoryName ->
                val existingCategory = existingCategories.find { it.name == categoryName }

                val categoryId = if (existingCategory != null) {
                    Log.d(TAG, "♻️ Exists: $categoryName")
                    existingCategory.id
                } else {
                    Log.d(TAG, "➕ Creating: $categoryName")
                    db.categoryDao().insert(CategoryEntity(name = categoryName))
                }

                val existingWords = db.wordDao()
                    .getByCategory(categoryId)
                    .map { it.normalizedValue }
                    .toSet()

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

            Log.d(TAG, "✅ updateData completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in updateData: ${e.message}", e)
        }
    }

    private fun getSeedVersion(context: Context): Int {
        val prefs = context.getSharedPreferences("impostor_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("seed_version", 0)
    }

    private fun saveSeedVersion(context: Context, version: Int) {
        val prefs = context.getSharedPreferences("impostor_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("seed_version", version).apply()
        Log.d(TAG, "💾 Saved version: $version")
    }
}