package com.matthew.impostorapp.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.matthew.impostorapp.domain.model.Game
import com.matthew.impostorapp.domain.model.GameState
import com.matthew.impostorapp.domain.model.Player
import com.matthew.impostorapp.usecase.AssignRoleUseCase

class GameViewModel : ViewModel() {

    private val assignRoleUseCase = AssignRoleUseCase()

    private val _game = mutableStateOf<Game?>(null)
    val game: State<Game?> = _game

    private var playerCount = 0
    private var impostorCount = 0
    private val words = mutableListOf<String>()

    fun addWord(word: String) {
        words.add(word)
    }

    fun setupGame(players: Int, impostors: Int) {
        playerCount = players
        impostorCount = impostors
        startRound()
    }

    fun startRound() {
        val players = assignRoleUseCase.execute(playerCount, impostorCount)
        val randomWord = words.shuffled().random()

        _game.value = Game(
            players = players,
            word = randomWord,
            currentPlayerIndex = 0,
            state = GameState.REVEAL
        )
    }

    fun currentPlayer(): Player? {
        return _game.value?.players?.get(_game.value!!.currentPlayerIndex)
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
}
