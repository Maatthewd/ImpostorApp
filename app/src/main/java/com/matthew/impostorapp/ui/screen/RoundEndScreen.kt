package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RoundEndScreen(
    hasWords: Boolean,
    onNextRound: () -> Unit,
    onRestartGame: () -> Unit,
    onConfig: () -> Unit
) {
    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {

            Text("Fin de ronda", color = Color.White, style = MaterialTheme.typography.headlineLarge)

            Spacer(Modifier.height(32.dp))

            if (hasWords) {
                Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                    Text("Nueva ronda")
                }
            } else {
                Text("No quedan palabras", color = Color.White)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onRestartGame,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al inicio")
            }

            OutlinedButton(
                onClick = onConfig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Configurar proximas rondas")
            }
        }
    }
}
