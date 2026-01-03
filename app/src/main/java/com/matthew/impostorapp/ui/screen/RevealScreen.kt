package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.matthew.impostorapp.domain.model.Role

@Composable
fun RevealScreen(
    playerIndex: Int,
    totalPlayers: Int,
    role: Role,
    word: String,
    category: String,
    currentRound: Int,
    totalRounds: Int,
    onNext: () -> Unit
) {
    var reveal by remember { mutableStateOf(false) }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {

            // RONDA

            Text(
                text = "Ronda $currentRound de ${totalRounds + 1}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            // JUGADOR

            Text(
                "Jugador ${playerIndex + 1} de $totalPlayers",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(32.dp))

            // CATEGORÍA

            Text(
                text = category.uppercase(),
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(24.dp))

            // REVELAR ROL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                reveal = true
                                tryAwaitRelease()
                                reveal = false
                            }
                        )
                    }
            ) {

                Text(
                    text = when {
                        !reveal -> "Mantené apretado para revelar"
                        role == Role.IMPOSTOR -> "SOS EL IMPOSTOR"
                        else -> word
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            Spacer(Modifier.height(48.dp))

            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Siguiente")
            }
        }
    }
}
