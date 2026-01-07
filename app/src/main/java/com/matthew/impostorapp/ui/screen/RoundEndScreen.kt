package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RoundEndScreen(
    currentRound: Int,
    totalRounds: Int,
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
            text = "Fin de ronda $currentRound",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )

        Text(
            text = "Quedan ${totalRounds - currentRound} rondas",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

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
            Text("Cambiar configuración")
        }

        OutlinedButton(
            onClick = onEndGame,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Finalizar Juego")
        }
    }
}