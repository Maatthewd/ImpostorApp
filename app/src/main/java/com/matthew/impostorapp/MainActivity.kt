package com.matthew.impostorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.impostorapp.data.local.db.DatabaseProvider
import com.matthew.impostorapp.data.repository.GameRepository
import com.matthew.impostorapp.domain.model.GameState
import com.matthew.impostorapp.ui.screen.*
import com.matthew.impostorapp.viewmodel.GameViewModel
import com.matthew.impostorapp.ui.theme.ImpostorAppTheme
import com.matthew.impostorapp.viewmodel.GameViewModelFactory

class MainActivity : ComponentActivity() {

    @ExperimentalLayoutApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = DatabaseProvider.getDatabase(this)
        val repository = GameRepository(
            db.categoryDao(),
            db.wordDao()
        )

        val factory = GameViewModelFactory(repository)

        setContent {
            val viewModel: GameViewModel = viewModel(factory = factory)
            val game by viewModel.game
            MaterialTheme {
                when (val current = game) {

                    null -> {
                        LobbyScreen(
                            categories = viewModel.categoryList,
                            onStartGame = { viewModel.setupGame(it) }
                        )
                    }

                    else -> when (current.state) {

                        GameState.LOBBY -> {
                            LobbyScreen(
                                categories = viewModel.categoryList,
                                onStartGame = { viewModel.setupGame(it) }
                            )
                        }

                        GameState.REVEAL -> {
                            val player = current.players[current.currentPlayerIndex]

                            RevealScreen(
                                playerIndex = current.currentPlayerIndex,
                                totalPlayers = current.players.size,
                                role = player.role,
                                word = current.word.value,
                                category = current.word.category,
                                currentRound = current.round,
                                totalRounds = viewModel.getTotalRounds(),
                                onNext = { viewModel.nextPlayer() }
                            )
                        }

                        GameState.ROUND_END -> {
                            RoundEndScreen(
                                onNextRound = { viewModel.nextRound() },
                                onConfig = { viewModel.onConfig() },
                                onEndGame = { viewModel.endGame() }
                            )
                        }

                        GameState.CONFIG -> {
                            LobbyScreen(
                                categories = viewModel.categoryList,
                                onStartGame = { viewModel.setupGame(it) }
                            )
                        }

                        GameState.GAME_OVER -> {
                            GameOverScreen(
                                totalRounds = viewModel.getTotalRounds(),
                                onRestart = { viewModel.resetGame() }
                            )
                        }
                    }
                }
            }

        }
    }
}
