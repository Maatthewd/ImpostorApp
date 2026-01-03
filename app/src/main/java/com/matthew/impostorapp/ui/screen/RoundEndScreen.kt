package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RoundEndScreen(
    onNextRound: () -> Unit,
    onConfig: () -> Unit,
    onEndGame: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Fin de ronda",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )

        Button(
            onClick = onNextRound,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Siguiente ronda")
        }

        OutlinedButton(
            onClick = onConfig,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Configurar")
        }

        OutlinedButton(
            onClick = onEndGame,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finalizar Juego")
        }
    }
}
