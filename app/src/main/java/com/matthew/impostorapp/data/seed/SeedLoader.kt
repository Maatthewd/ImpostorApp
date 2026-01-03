package com.matthew.impostorapp.data.seed

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SeedLoader(private val context: Context) {

    private val gson = Gson()

    fun loadCategories(): List<String> {
        val json = readAsset("categories.json")
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    fun loadWords(): Map<String, List<String>> {
        val json = readAsset("words.json")
        return gson.fromJson(
            json,
            object : TypeToken<Map<String, List<String>>>() {}.type
        )
    }

    private fun readAsset(fileName: String): String {
        return context.assets.open(fileName)
            .bufferedReader()
            .use { it.readText() }
    }
}
