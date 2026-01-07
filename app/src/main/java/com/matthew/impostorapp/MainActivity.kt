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
import com.matthew.impostorapp.ui.theme.ImpostorAppTheme
import com.matthew.impostorapp.viewmodel.GameViewModelFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            val categories by viewModel.categories.collectAsState()
            val wordCounts by viewModel.wordCounts.collectAsState()
            val managementError by viewModel.managementError.collectAsState()
            val currentCategoryWords by viewModel.currentCategoryWords.collectAsState()

            var currentScreen by remember { mutableStateOf<Screen>(Screen.Lobby) }

            ImpostorAppTheme {
                MainContent(
                    viewModel = viewModel,
                    game = game,
                    categories = categories,
                    wordCounts = wordCounts,
                    currentCategoryWords = currentCategoryWords,
                    managementError = managementError,
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainContent(
    viewModel: GameViewModel,
    game: com.matthew.impostorapp.domain.model.Game?,
    categories: List<String>,
    wordCounts: Map<String, Int>,
    currentCategoryWords: List<String>,
    managementError: String?,
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit
) {
    when (val screen = currentScreen) {
        is Screen.Lobby -> {
            when (val current = game) {
                null -> {
                    LobbyScreen(
                        categories = categories,
                        onStartGame = { viewModel.setupGame(it) },
                        onManageCategories = {
                            viewModel.clearError()
                            onScreenChange(Screen.ManageCategories)
                        },
                        getWordCount = { category ->
                            wordCounts[category] ?: 0
                        },
                        errorMessage = managementError,
                        savedConfig = viewModel.getSavedConfig(),
                        hasSavedState = viewModel.hasSavedState()
                    )
                }

                else -> when (current.state) {
                    GameState.LOBBY -> {
                        LobbyScreen(
                            categories = categories,
                            onStartGame = { viewModel.setupGame(it) },
                            onManageCategories = {
                                viewModel.clearError()
                                onScreenChange(Screen.ManageCategories)
                            },
                            getWordCount = { category ->
                                wordCounts[category] ?: 0
                            },
                            errorMessage = managementError,
                            savedConfig = viewModel.getSavedConfig(),
                            hasSavedState = viewModel.hasSavedState()
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
                            currentRound = viewModel.getCurrentRound(),
                            totalRounds = viewModel.getTotalRounds(),
                            onNextRound = { viewModel.nextRound() },
                            onConfig = {
                                viewModel.goToConfig()
                                onScreenChange(Screen.Lobby)
                            },
                            onEndGame = { viewModel.endGame() }
                        )
                    }

                    GameState.CONFIG -> {
                        LobbyScreen(
                            categories = categories,
                            onStartGame = { viewModel.setupGame(it) },
                            onManageCategories = {
                                viewModel.clearError()
                                onScreenChange(Screen.ManageCategories)
                            },
                            getWordCount = { category ->
                                wordCounts[category] ?: 0
                            },
                            errorMessage = managementError,
                            savedConfig = viewModel.getSavedConfig(),
                            hasSavedState = viewModel.hasSavedState()
                        )
                    }

                    GameState.GAME_OVER -> {
                        GameOverScreen(
                            totalRounds = viewModel.getTotalRounds(),
                            onRestart = {
                                viewModel.resetGame()
                                onScreenChange(Screen.Lobby)
                            }
                        )
                    }
                }
            }
        }

        is Screen.ManageCategories -> {
            ManageCategoriesScreen(
                categories = categories,
                onBack = {
                    viewModel.clearError()
                    onScreenChange(Screen.Lobby)
                },
                onAddCategory = { viewModel.addCategory(it) },
                onDeleteCategory = { category, force ->
                    viewModel.deleteCategory(category, force)
                },
                onManageWords = { category ->
                    viewModel.clearError()
                    viewModel.loadWordsForCategory(category)
                    onScreenChange(Screen.ManageWords(category))
                },
                errorMessage = managementError
            )
        }

        is Screen.ManageWords -> {
            ManageWordsScreen(
                category = screen.category,
                words = currentCategoryWords,
                onBack = {
                    viewModel.clearError()
                    onScreenChange(Screen.ManageCategories)
                },
                onAddWord = { word ->
                    viewModel.addWord(screen.category, word)
                },
                onDeleteWord = { word ->
                    viewModel.deleteWord(screen.category, word)
                },
                errorMessage = managementError
            )
        }
    }
}