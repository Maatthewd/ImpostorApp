package com.matthew.impostorapp.viewmodel

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.impostorapp.data.repository.GameRepository
import com.matthew.impostorapp.domain.model.*
import com.matthew.impostorapp.usecase.AssignRoleUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val assignRoleUseCase = AssignRoleUseCase()

    private val _game = mutableStateOf<Game?>(null)
    val game: State<Game?> = _game

    // =====================
    // ESTADO DE CONFIG
    // =====================

    private var currentRound = 1
    private var currentConfig: GameConfig? = null

    // =====================
    // BANCO DE PARTIDA
    // =====================

    private val wordBank = mutableStateListOf<Word>()
    private val usedWords = mutableSetOf<String>()

    private val _categories = mutableStateListOf<String>()
    val categoryList: List<String> get() = _categories

    // =====================
    // GESTIÓN DE PALABRAS
    // =====================

    private val _wordsInCategory = mutableStateListOf<String>()
    val wordsInCategory: List<String> get() = _wordsInCategory

    private val _managementError = mutableStateOf<String?>(null)
    val managementError: State<String?> = _managementError

    // Contador de palabras por categoría
    private val _wordCountByCategory = mutableStateMapOf<String, Int>()
    val wordCountByCategory: Map<String, Int> get() = _wordCountByCategory

    // =====================
    // INIT
    // =====================

    init {
        viewModelScope.launch {
            delay(2000) // 2 segundos
            loadInitialData()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _categories.clear()
            _categories.addAll(repository.getCategories())

            // Cargar contadores
            _categories.forEach { category ->
                val count = repository.getWordsByCategory(category).size
                _wordCountByCategory[category] = count
            }

            Log.d("GameViewModel", "✅ Loaded ${_categories.size} categories")
        }
    }

    // =====================
    // JUEGO
    // =====================

    private var totalRounds = 0

    fun setupGame(config: GameConfig) {
        viewModelScope.launch {
            wordBank.clear()
            wordBank.addAll(repository.getWords())

            val eligibleWords = wordBank.filter {
                it.matchesCategoryMode(config.categoryMode)
            }

            // Verificar que hay palabras
            if (eligibleWords.isEmpty()) {
                _managementError.value = "Las categorías seleccionadas no tienen palabras"
                return@launch
            }

            // Verificar mínimo de palabras
            if (eligibleWords.size < 3) {
                _managementError.value = "Necesitas al menos 3 palabras para jugar"
                return@launch
            }

            currentConfig = config
            currentRound = 1
            usedWords.clear()

            totalRounds = eligibleWords.size

            startRound()
        }
    }

    private fun startRound() {
        val config = currentConfig ?: return

        val eligibleWords = wordBank
            .filter { it.matchesCategoryMode(config.categoryMode) }
            .filter { it.normalizedValue !in usedWords }

        if (eligibleWords.isEmpty()) {
            _game.value = _game.value?.copy(state = GameState.GAME_OVER)
            return
        }

        val selectedWord = eligibleWords.random()
        usedWords.add(selectedWord.normalizedValue)

        val players = assignRoleUseCase.execute(
            config.players,
            config.impostors
        )

        _game.value = Game(
            players = players,
            word = selectedWord,
            currentPlayerIndex = 0,
            state = GameState.REVEAL,
            config = config,
            round = currentRound
        )
    }

    fun nextPlayer() {
        val game = _game.value ?: return
        val next = game.currentPlayerIndex + 1

        _game.value =
            if (next >= game.players.size) {
                game.copy(state = GameState.ROUND_END)
            } else {
                game.copy(currentPlayerIndex = next)
            }
    }

    fun nextRound() {
        currentRound++
        startRound()
    }

    fun endGame() {
        _game.value = _game.value?.copy(state = GameState.GAME_OVER)
    }

    fun onConfig() {
        _game.value = _game.value?.copy(state = GameState.CONFIG)
    }

    fun resetGame() {
        _game.value = null
        currentConfig = null
        currentRound = 1
        usedWords.clear()
        wordBank.clear()
    }

    fun getTotalRounds(): Int = totalRounds

    // =====================
    // GESTIÓN DE CATEGORÍAS
    // =====================

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(name).fold(
                onSuccess = {
                    _categories.clear()
                    _categories.addAll(repository.getCategories())
                    _wordCountByCategory[name] = 0
                    _managementError.value = null
                },
                onFailure = { error ->
                    _managementError.value = error.message
                }
            )
        }
    }

    fun deleteCategory(name: String, force: Boolean) {
        viewModelScope.launch {
            repository.deleteCategory(name, force).fold(
                onSuccess = {
                    _categories.clear()
                    _categories.addAll(repository.getCategories())
                    _wordCountByCategory.remove(name)
                    _managementError.value = null
                },
                onFailure = { error ->
                    _managementError.value = error.message
                }
            )
        }
    }

    // =====================
    // GESTIÓN DE PALABRAS
    // =====================

    fun loadWordsForCategory(categoryName: String) {
        viewModelScope.launch {
            _wordsInCategory.clear()
            _wordsInCategory.addAll(repository.getWordsByCategory(categoryName))
        }
    }

    fun addWord(categoryName: String, word: String) {
        viewModelScope.launch {
            repository.addWord(categoryName, word).fold(
                onSuccess = {
                    loadWordsForCategory(categoryName)
                    _wordCountByCategory[categoryName] =
                        (_wordCountByCategory[categoryName] ?: 0) + 1
                    _managementError.value = null
                },
                onFailure = { error ->
                    _managementError.value = error.message
                }
            )
        }
    }

    fun deleteWord(categoryName: String, word: String) {
        viewModelScope.launch {
            repository.deleteWord(categoryName, word).fold(
                onSuccess = {
                    loadWordsForCategory(categoryName)
                    _wordCountByCategory[categoryName] =
                        maxOf(0, (_wordCountByCategory[categoryName] ?: 0) - 1)
                    _managementError.value = null
                },
                onFailure = { error ->
                    _managementError.value = error.message
                }
            )
        }
    }

    fun clearError() {
        _managementError.value = null
    }

    // Obtener cantidad de palabras por categoría
    fun getWordCount(category: String): Int {
        return _wordCountByCategory[category] ?: 0
    }
}