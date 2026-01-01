package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.matthew.impostorapp.viewmodel.GameViewModel

@Composable
fun SetupScreen(vm: GameViewModel) {

    var players by remember { mutableStateOf("") }
    var impostors by remember { mutableStateOf("") }
    var word by remember { mutableStateOf("") }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {

            Text("Impostor", color = Color.White, style = MaterialTheme.typography.headlineLarge)

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(players, { players = it }, label = { Text("Jugadores") })
            OutlinedTextField(impostors, { impostors = it }, label = { Text("Impostores") })

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                label = { Text("Agregar palabra") }
            )

            Button(
                onClick = {
                    if (word.isNotBlank()) {
                        vm.addWord(word)
                        word = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar palabra")
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    vm.setupGame(players.toInt(), impostors.toInt())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar juego")
            }
        }
    }
}
