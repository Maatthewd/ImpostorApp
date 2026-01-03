package com.matthew.impostorapp.data.local.db

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseCleaner {

    /**
     * Limpia categorías duplicadas manteniendo solo la primera de cada nombre
     * y reasignando todas sus palabras.
     *
     * Llamar UNA VEZ desde MainActivity.onCreate() después de obtener la DB
     */

    fun cleanDuplicateCategories(context: Context, db: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allCategories = db.categoryDao().getAll()
                val categoryGroups = allCategories.groupBy { it.name }

                categoryGroups.forEach { (name, categories) ->
                    if (categories.size > 1) {
                        // Hay duplicados
                        val keepCategory = categories.first() // Mantener la primera
                        val duplicates = categories.drop(1)   // Eliminar el resto

                        duplicates.forEach { duplicate ->
                            // Obtener todas las palabras de la categoría duplicada
                            val words = db.wordDao().getByCategory(duplicate.id)

                            // Reasignar palabras a la categoría que mantenemos
                            words.forEach { word ->
                                // Verificar si la palabra ya existe en la categoría buena
                                val existingWords = db.wordDao()
                                    .getByCategory(keepCategory.id)
                                    .map { it.normalizedValue }

                                if (word.normalizedValue !in existingWords) {
                                    // Solo agregar si no existe
                                    db.wordDao().insert(
                                        word.copy(
                                            id = 0, // Nuevo ID
                                            categoryId = keepCategory.id
                                        )
                                    )
                                }
                            }

                            // Eliminar palabras de la categoría duplicada
                            db.wordDao().deleteByCategory(duplicate.id)

                            // Eliminar categoría duplicada
                            db.categoryDao().deleteById(duplicate.id)
                        }

                        println("✅ Limpiada categoría duplicada: $name (${duplicates.size} duplicados eliminados)")
                    }
                }

                // Resetear seed version para que no intente actualizar de nuevo
                val prefs = context.getSharedPreferences("impostor_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("seed_version", 1).apply()

                println("✅ Limpieza completada")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}