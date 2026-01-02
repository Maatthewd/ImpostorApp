package com.matthew.impostorapp.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.matthew.impostorapp.domain.model.Game
import com.matthew.impostorapp.domain.model.GameState
import com.matthew.impostorapp.usecase.AssignRoleUseCase

class GameViewModel : ViewModel() {

    private val assignRoleUseCase = AssignRoleUseCase()

    private val _game = mutableStateOf<Game?>(null)
    val game: State<Game?> = _game

    private var playerCount = 0
    private var impostorCount = 0

    // 🔹 palabras disponibles y usadas
    private val availableWords = mutableListOf<String>()
    private val usedWords = mutableListOf<String>()

    // 🔹 para que la UI pueda leerlas
    val words: List<String> get() = availableWords

    fun addWord(word: String) {
        if (word.isNotBlank() && word !in availableWords && word !in usedWords) {
            availableWords.add(word)
        }
    }

    fun removeWord(word: String) {
        availableWords.remove(word)
    }

    fun setupGame(players: Int, impostors: Int) {
        playerCount = players
        impostorCount = impostors
        startRound()
    }

    fun startRound() {
        if (availableWords.isEmpty()) return

        val players = assignRoleUseCase.execute(playerCount, impostorCount)

        val randomWord = availableWords.random()
        availableWords.remove(randomWord)
        usedWords.add(randomWord)

        _game.value = Game(
            players = players,
            word = randomWord,
            currentPlayerIndex = 0,
            state = GameState.REVEAL
        )
    }

    fun resetGame() {
        _game.value = null
        playerCount = 0
        impostorCount = 0
        availableWords.clear()
        usedWords.clear()
    }

    fun currentPlayer() =
        _game.value?.players?.get(_game.value!!.currentPlayerIndex)

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
}
