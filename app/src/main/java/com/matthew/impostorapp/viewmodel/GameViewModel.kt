package com.matthew.impostorapp.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.impostorapp.data.repository.GameRepository
import com.matthew.impostorapp.domain.model.*
import com.matthew.impostorapp.usecase.AssignRoleUseCase
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
    // INIT
    // =====================

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _categories.clear()
            _categories.addAll(repository.getCategories())
        }
    }

    // =====================
    // JUEGO
    // =====================

    private var totalRounds = 0
    fun setupGame(config: GameConfig) {
        currentConfig = config
        currentRound = 1
        usedWords.clear()

        viewModelScope.launch {
            wordBank.clear()
            wordBank.addAll(repository.getWords())
            startRound()
        }

        totalRounds = wordBank.count {
            it.matchesCategoryMode(config.categoryMode)
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

}
