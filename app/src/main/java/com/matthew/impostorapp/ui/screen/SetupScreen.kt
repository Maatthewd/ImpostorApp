package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.impostorapp.viewmodel.GameViewModel

@Composable
fun SetupScreen(
    gameViewModel: GameViewModel,
    onContinue: () -> Unit
) {

    var players by remember { mutableStateOf("") }
    var impostors by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Impostor",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = players,
            onValueChange = { players = it },
            label = { Text("Jugadores") },
            singleLine = true
        )

        OutlinedTextField(
            value = impostors,
            onValueChange = { impostors = it },
            label = { Text("Impostores") },
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))

        Button(onClick = {
            val p = players.toIntOrNull()
            val i = impostors.toIntOrNull()

            error =
                if (p == null || i == null) "Valores inválidos"
                else if (p < 3) "Mínimo 3 jugadores"
                else if (i <= 0 || i >= p) "Cantidad de impostores inválida"
                else null

            if (error == null) {
                gameViewModel.configurePlayers(p!!, i!!)
                onContinue()
            }
        }) {
            Text("Continuar")
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }



    }


}
