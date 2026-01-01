package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.impostorapp.domain.model.Player
import com.matthew.impostorapp.domain.model.Role

@Composable
fun RevealScreen(
    player: Player,
    playerIndex: Int,
    totalPlayers: Int,
    word: String,
    onNext: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text("Jugador ${playerIndex + 1} de $totalPlayers")

        Spacer(Modifier.height(32.dp))

        Button(onClick = { revealed = true }) {
            Text("Mantener para revelar")
        }

        if (revealed) {
            Spacer(Modifier.height(24.dp))

            if (player.role == Role.IMPOSTOR) {
                Text("SOS EL IMPOSTOR", color = MaterialTheme.colorScheme.error)
            } else {
                Text("Palabra:")
                Text(word, style = MaterialTheme.typography.headlineLarge)
            }

            Spacer(Modifier.height(32.dp))

            Button(onClick = {
                revealed = false
                onNext()
            }) {
                Text("Siguiente jugador")
            }
        }
    }
}
