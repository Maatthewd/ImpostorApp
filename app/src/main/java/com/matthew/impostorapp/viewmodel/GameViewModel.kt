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

    private var playerCount: Int = 0
    private var impostorCount: Int = 0
    private var words: MutableList<String> = mutableListOf()

    /* ================= CONFIGURACIÓN ================= */

    fun configurePlayers(players: Int, impostors: Int) {
        playerCount = players
        impostorCount = impostors
    }

    fun addWord(word: String) {
        words.add(word)
    }

    /* ================= INICIO DE JUEGO ================= */

    fun startGame() {
        require(words.isNotEmpty()) { "No hay palabras cargadas" }

        val players = assignRoleUseCase.execute(
            playerCount = playerCount,
            impostorCount = impostorCount
        )

        val randomWord = words.random()

        _game.value = Game(
            players = players,
            word = randomWord,
            currentPlayerIndex = 0,
            state = GameState.REVEAL
        )
    }

    /* ================= RONDA ================= */

    fun currentPlayer() =
        _game.value?.players?.get(_game.value!!.currentPlayerIndex)

    fun nextPlayer() {
        val game = _game.value ?: return
        val nextIndex = game.currentPlayerIndex + 1

        _game.value =
            if (nextIndex >= game.players.size) {
                game.copy(state = GameState.ROUND_END)
            } else {
                game.copy(currentPlayerIndex = nextIndex)
            }
    }
}
