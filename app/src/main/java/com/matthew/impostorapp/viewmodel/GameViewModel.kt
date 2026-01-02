package com.matthew.impostorapp.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.matthew.impostorapp.domain.model.*
import com.matthew.impostorapp.usecase.AssignRoleUseCase

class GameViewModel : ViewModel() {

    private val assignRoleUseCase = AssignRoleUseCase()

    private val _game = mutableStateOf<Game?>(null)
    val game: State<Game?> = _game

    private var playerCount = 0
    private var impostorCount = 0

    // CATEGORIAS
    private val categories = mutableStateListOf<String>()
    val categoryList: List<String> get() = categories

    fun addCategory(name: String) {
        if (name.isNotBlank() && name !in categories) {
            categories.add(name)
        }
    }

    fun removeCategory(name: String) {
        categories.remove(name)
        availableWords.removeAll { it.category == name }
        usedWords.removeAll { it.category == name }
    }

    fun renameCategory(oldName: String, newName: String) {
        if (newName.isBlank() || newName == oldName || newName in categories) return

        val index = categories.indexOf(oldName)
        if (index == -1) return

        categories[index] = newName

        // actualizar palabras disponibles
        availableWords.replaceAll {
            if (it.category == oldName) it.copy(category = newName) else it
        }

        // actualizar palabras usadas
        usedWords.replaceAll {
            if (it.category == oldName) it.copy(category = newName) else it
        }
    }

    fun categoryHasWords(category: String): Boolean {
        return availableWords.any { it.category == category } ||
                usedWords.any { it.category == category }
    }

    fun deleteCategory(category: String) {
        categories.remove(category)
        availableWords.removeAll { it.category == category }
        usedWords.removeAll { it.category == category }
    }


    // PALABRAS
    private val availableWords = mutableStateListOf<Word>()
    private val usedWords = mutableStateListOf<Word>()

    val words: List<Word> get() = availableWords

    fun addWord(value: String, category: String) {
        if (
            value.isBlank() ||
            category !in categories ||
            availableWords.any { it.value == value }
        ) return

        availableWords.add(Word(value, category))
    }

    fun removeWord(word: Word) {
        availableWords.remove(word)
    }

    // JUEGO
    fun setupGame(players: Int, impostors: Int) {
        playerCount = players
        impostorCount = impostors
        startRound()
    }

    fun startRound() {
        if (availableWords.isEmpty()) return

        val players = assignRoleUseCase.execute(playerCount, impostorCount)

        val selectedWord = availableWords.random()
        availableWords.remove(selectedWord)
        usedWords.add(selectedWord)

        _game.value = Game(
            players = players,
            word = selectedWord.value,
            category = selectedWord.category,
            currentPlayerIndex = 0,
            state = GameState.REVEAL
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

    // CONFIG

    fun openConfig() {
        val game = _game.value ?: return
        _game.value = game.copy(state = GameState.CONFIG)
    }

    fun applyConfigAndStart(players: Int, impostors: Int) {
        playerCount = players
        impostorCount = impostors
        startRound()
    }

    fun resetGame() {
        _game.value = null
        playerCount = 0
        impostorCount = 0
        availableWords.clear()
        usedWords.clear()
        categories.clear()
    }

    // INFO

    fun currentPlayer(): Player? =
        _game.value?.players?.getOrNull(_game.value!!.currentPlayerIndex)

    val currentRound: Int get() = usedWords.size
    val totalRounds: Int get() = availableWords.size + usedWords.size
}
