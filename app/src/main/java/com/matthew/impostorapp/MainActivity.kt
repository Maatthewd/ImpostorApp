package com.matthew.impostorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.impostorapp.domain.model.GameState
import com.matthew.impostorapp.ui.screen.*
import com.matthew.impostorapp.viewmodel.GameViewModel
import com.matthew.impostorapp.ui.theme.ImpostorAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ImpostorAppTheme {

                val vm: GameViewModel = viewModel()
                val game = vm.game.value

                when (game?.state ?: GameState.SETUP) {
                    GameState.SETUP -> SetupScreen(vm)

                    GameState.REVEAL -> RevealScreen(
                        playerIndex = game!!.currentPlayerIndex,
                        totalPlayers = game.players.size,
                        role = vm.currentPlayer()!!.role,
                        word = game.word,
                        onNext = { vm.nextPlayer() }
                    )

                    GameState.ROUND_END ->
                        RoundEndScreen(
                            hasWords = vm.words.isNotEmpty(),
                            onNextRound = { vm.startRound() },
                            onRestartGame = { vm.resetGame()},
                            onConfig = {vm.openConfig()}
                        )

                    GameState.CONFIG -> SetupScreen(
                        vm = vm,
                        isReconfig = true,
                        onConfirm = { p, i -> vm.applyConfigAndStart(p, i) }
                    )

                }
            }
        }
    }
}
