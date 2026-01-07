package com.matthew.impostorapp.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.impostorapp.data.repository.GameRepository
import com.matthew.impostorapp.domain.model.*
import com.matthew.impostorapp.usecase.AssignRoleUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel profesional con manejo de estados reactivo usando Flow.
 *
 * Ventajas de este enfoque:
 * - Los datos se actualizan automáticamente cuando cambia la BD
 * - No hay race conditions con el seed inicial
 * - Separación clara entre estado de UI y estado de datos
 * - Más fácil de testear
 */
class GameViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val assignRoleUseCase = AssignRoleUseCase()

    // =====================
    // ESTADO DEL JUEGO
    // =====================

    private val _game = mutableStateOf<Game?>(null)
    val game: State<Game?> = _game

    private var currentRound = 1
    private var currentConfig: GameConfig? = null
    private var totalRounds = 0

    // =====================
    // BANCO DE PALABRAS
    // =====================

    private val wordBank = mutableListOf<Word>()
    private val usedWords = mutableSetOf<String>()

    // =====================
    // ESTADO DE UI - Reactivo con Flow
    // =====================

    /**
     * StateFlow para categorías. Se actualiza automáticamente
     * cuando hay cambios en la base de datos.
     */
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Flow de categorías que se actualiza automáticamente
     */
    val categories: StateFlow<List<String>> = repository
        .observeCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Map de conteo de palabras por categoría
     */
    private val _wordCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val wordCounts: StateFlow<Map<String, Int>> = _wordCounts.asStateFlow()

    /**
     * Palabras de la categoría actualmente seleccionada
     */
    private val _currentCategoryWords = MutableStateFlow<List<String>>(emptyList())
    val currentCategoryWords: StateFlow<List<String>> = _currentCategoryWords.asStateFlow()

    /**
     * Error de gestión (agregar/eliminar categorías/palabras)
     */
    private val _managementError = MutableStateFlow<String?>(null)
    val managementError: StateFlow<String?> = _managementError.asStateFlow()

    // =====================
    // INIT
    // =====================

    init {
        observeCategoryChanges()
        checkInitialState()
    }

    /**
     * Observa cambios en categorías y actualiza contadores
     */
    private fun observeCategoryChanges() {
        viewModelScope.launch {
            categories.collect { categoryList ->
                // Actualizar contadores cuando cambian las categorías
                val counts = mutableMapOf<String, Int>()
                categoryList.forEach { category ->
                    counts[category] = repository.getWordCount(category)
                }
                _wordCounts.value = counts

                // Actualizar estado UI
                if (categoryList.isEmpty()) {
                    _uiState.value = UiState.Empty
                } else {
                    _uiState.value = UiState.Success
                }
            }
        }
    }

    /**
     * Verifica el estado inicial de la BD
     */
    private fun checkInitialState() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            // El Flow de categories se encargará del resto
        }
    }

    // =====================
    // LÓGICA DE JUEGO
    // =====================

    fun setupGame(config: GameConfig) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                wordBank.clear()
                wordBank.addAll(repository.getWords())

                val eligibleWords = wordBank.filter {
                    it.matchesCategoryMode(config.categoryMode)
                }

                when {
                    eligibleWords.isEmpty() -> {
                        _managementError.value = "Las categorías seleccionadas no tienen palabras"
                        _uiState.value = UiState.Success
                        return@launch
                    }
                    eligibleWords.size < 3 -> {
                        _managementError.value = "Necesitas al menos 3 palabras para jugar"
                        _uiState.value = UiState.Success
                        return@launch
                    }
                }

                currentConfig = config
                currentRound = 1
                usedWords.clear()
                totalRounds = eligibleWords.size

                startRound()
                _uiState.value = UiState.Success

            } catch (e: Exception) {
                Log.e("GameViewModel", "Error al configurar juego", e)
                _managementError.value = "Error al iniciar el juego: ${e.message}"
                _uiState.value = UiState.Error(e.message ?: "Error desconocido")
            }
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

        _game.value = if (next >= game.players.size) {
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
            _managementError.value = null
            repository.addCategory(name).fold(
                onSuccess = {
                    // El Flow se actualiza automáticamente
                    Log.d("GameViewModel", "Categoría agregada: $name")
                },
                onFailure = { error ->
                    _managementError.value = error.message
                }
            )
        }
    }

    fun deleteCategory(name: String, force: Boolean) {
        viewModelScope.launch {
            _managementError.value = null
            repository.deleteCategory(name, force).fold(
                onSuccess = {
                    // El Flow se actualiza automáticamente
                    Log.d("GameViewModel", "Categoría eliminada: $name")
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
            repository.observeWordsByCategory(categoryName)
                .collect { words ->
                    _currentCategoryWords.value = words
                }
        }
    }

    fun addWord(categoryName: String, word: String) {
        viewModelScope.launch {
            _managementError.value = null
            repository.addWord(categoryName, word).fold(
                onSuccess = {
                    // Actualizar contador
                    val currentCounts = _wordCounts.value.toMutableMap()
                    currentCounts[categoryName] = (currentCounts[categoryName] ?: 0) + 1
                    _wordCounts.value = currentCounts

                    Log.d("GameViewModel", "Palabra agregada: $word en $categoryName")
                },
                onFailure = { error ->
                    _managementError.value = error.message
                }
            )
        }
    }

    fun deleteWord(categoryName: String, word: String) {
        viewModelScope.launch {
            _managementError.value = null
            repository.deleteWord(categoryName, word).fold(
                onSuccess = {
                    // Actualizar contador
                    val currentCounts = _wordCounts.value.toMutableMap()
                    currentCounts[categoryName] = maxOf(0, (currentCounts[categoryName] ?: 0) - 1)
                    _wordCounts.value = currentCounts

                    Log.d("GameViewModel", "Palabra eliminada: $word de $categoryName")
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

    fun getWordCount(category: String): Int {
        return _wordCounts.value[category] ?: 0
    }
}

/**
 * Estados de la UI para manejar loading, empty, success y error
 */
sealed class UiState {
    object Loading : UiState()
    object Empty : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}