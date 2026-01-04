package com.matthew.impostorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.impostorapp.data.local.db.DatabaseProvider
import com.matthew.impostorapp.data.repository.GameRepository
import com.matthew.impostorapp.domain.model.GameState
import com.matthew.impostorapp.ui.screen.*
import com.matthew.impostorapp.viewmodel.GameViewModel
import com.matthew.impostorapp.ui.theme.ImpostorAppTheme  // CORREGIDO
import com.matthew.impostorapp.viewmodel.GameViewModelFactory

sealed class Screen {
    object Lobby : Screen()
    object ManageCategories : Screen()
    data class ManageWords(val category: String) : Screen()
}

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
            val managementError by viewModel.managementError
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Lobby) }


            ImpostorAppTheme {
                when (val screen = currentScreen) {
                    is Screen.Lobby -> {
                        when (val current = game) {
                            null -> {
                                LobbyScreen(
                                    categories = viewModel.categoryList,
                                    onStartGame = { viewModel.setupGame(it) },
                                    onManageCategories = {
                                        viewModel.clearError()  // NUEVO
                                        currentScreen = Screen.ManageCategories
                                    },
                                    getWordCount = { viewModel.getWordCount(it) },  // NUEVO
                                    errorMessage = managementError  // NUEVO
                                )
                            }

                            else -> when (current.state) {
                                GameState.LOBBY -> {
                                    LobbyScreen(
                                        categories = viewModel.categoryList,
                                        onStartGame = { viewModel.setupGame(it) },
                                        onManageCategories = {
                                            viewModel.clearError()  // NUEVO
                                            currentScreen = Screen.ManageCategories
                                        },
                                        getWordCount = { viewModel.getWordCount(it) },  // NUEVO
                                        errorMessage = managementError  // NUEVO
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
                                        onConfig = {
                                            viewModel.resetGame()  // CORREGIDO
                                            currentScreen = Screen.Lobby
                                        },
                                        onEndGame = { viewModel.endGame() }
                                    )
                                }

                                GameState.CONFIG -> {
                                    LobbyScreen(
                                        categories = viewModel.categoryList,
                                        onStartGame = { viewModel.setupGame(it) },
                                        onManageCategories = {
                                            viewModel.clearError()  // NUEVO
                                            currentScreen = Screen.ManageCategories
                                        },
                                        getWordCount = { viewModel.getWordCount(it) },  // NUEVO
                                        errorMessage = managementError  // NUEVO
                                    )
                                }

                                GameState.GAME_OVER -> {
                                    GameOverScreen(
                                        totalRounds = viewModel.getTotalRounds(),
                                        onRestart = {
                                            viewModel.resetGame()
                                            currentScreen = Screen.Lobby
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is Screen.ManageCategories -> {
                        ManageCategoriesScreen(
                            categories = viewModel.categoryList,
                            onBack = {
                                viewModel.clearError()  // NUEVO
                                currentScreen = Screen.Lobby
                            },
                            onAddCategory = { viewModel.addCategory(it) },
                            onDeleteCategory = { category, force ->
                                viewModel.deleteCategory(category, force)
                            },
                            onManageWords = { category ->
                                viewModel.clearError()  // NUEVO
                                viewModel.loadWordsForCategory(category)
                                currentScreen = Screen.ManageWords(category)
                            },
                            errorMessage = managementError  // NUEVO
                        )
                    }

                    is Screen.ManageWords -> {
                        ManageWordsScreen(
                            category = screen.category,
                            words = viewModel.wordsInCategory,
                            onBack = {
                                viewModel.clearError()  // NUEVO
                                currentScreen = Screen.ManageCategories
                            },
                            onAddWord = { word ->
                                viewModel.addWord(screen.category, word)
                            },
                            onDeleteWord = { word ->
                                viewModel.deleteWord(screen.category, word)
                            },
                            errorMessage = managementError  // NUEVO
                        )
                    }
                }
            }
        }
    }
}