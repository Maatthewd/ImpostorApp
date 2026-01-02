package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.matthew.impostorapp.viewmodel.GameViewModel

@Composable
fun SetupScreen(vm: GameViewModel,
                isReconfig: Boolean = false,
                onConfirm: ((Int, Int) -> Unit)? = null) {

    var players by remember { mutableStateOf("") }
    var impostors by remember { mutableStateOf("") }
    var word by remember { mutableStateOf("") }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {

            Text("Impostor", color = Color.White, style = MaterialTheme.typography.headlineLarge)

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(players, { players = it }, label = { Text("Jugadores") })
            OutlinedTextField(impostors, { impostors = it }, label = { Text("Impostores") })

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                label = { Text("Nueva palabra") }
            )

            Button(
                onClick = {
                    vm.addWord(word)
                    word = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar palabra")
            }

            Spacer(Modifier.height(16.dp))

            // LISTA DE PALABRAS AGREGADAS
            Text("Palabras cargadas:", color = Color.White)

            vm.words.forEach { w ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• $w", color = Color.White)

                    TextButton(onClick = { vm.removeWord(w) }) {
                        Text("Borrar", color = Color.Red)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val p = players.toIntOrNull()
                    val i = impostors.toIntOrNull()
                    if (p != null && i != null) {
                        if (isReconfig) {
                            onConfirm?.invoke(p, i)
                        } else {
                            vm.setupGame(p, i)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = vm.words.isNotEmpty()
            ) {
                Text(if (isReconfig) "Guardar configuración" else "Iniciar juego")
            }

        }
    }
}
