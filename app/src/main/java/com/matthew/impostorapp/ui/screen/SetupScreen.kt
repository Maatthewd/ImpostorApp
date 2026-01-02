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
fun SetupScreen(
    vm: GameViewModel,
    isReconfig: Boolean = false,
    onConfirm: ((Int, Int) -> Unit)? = null
) {

    var players by remember { mutableStateOf("") }
    var impostors by remember { mutableStateOf("") }

    var category by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editedName by remember { mutableStateOf("") }

    var word by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                if (isReconfig) "Configurar próxima ronda" else "Impostor",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(players, { players = it }, label = { Text("Jugadores") })
            OutlinedTextField(impostors, { impostors = it }, label = { Text("Impostores") })

            Spacer(Modifier.height(24.dp))

            /* ===== CATEGORÍAS ===== */

            Text("Categorías", color = Color.White)

            OutlinedTextField(category, { category = it }, label = { Text("Nueva categoría") })

            Button(onClick = {
                vm.addCategory(category)
                category = ""
            }) {
                Text("Agregar categoría")
            }

            vm.categoryList.forEach { cat ->

                if (editingCategory == cat) {

                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Editar categoría") }
                    )

                    Row {
                        Button(onClick = {
                            vm.renameCategory(cat, editedName)
                            editingCategory = null
                            editedName = ""
                        }) {
                            Text("Guardar")
                        }

                        Spacer(Modifier.width(8.dp))

                        TextButton(onClick = {
                            editingCategory = null
                            editedName = ""
                        }) {
                            Text("Cancelar")
                        }
                    }

                } else {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• $cat", color = Color.White)

                        TextButton(onClick = {
                            editingCategory = cat
                            editedName = cat
                        }) {
                            Text("Editar", color = Color.Gray)
                        }
                    }
                }
            }


            Spacer(Modifier.height(24.dp))

            /* ===== PALABRAS ===== */

            Text("Palabras", color = Color.White)

            OutlinedTextField(word, { word = it }, label = { Text("Palabra") })

            DropdownMenuBox(
                options = vm.categoryList,
                selected = selectedCategory,
                onSelect = { selectedCategory = it }
            )

            Button(onClick = {
                vm.addWord(word, selectedCategory)
                word = ""
            }) {
                Text("Agregar palabra")
            }

            vm.words.forEach { w ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${w.value} (${w.category})", color = Color.White)
                    TextButton(onClick = { vm.removeWord(w) }) {
                        Text("Borrar", color = Color.Red)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val p = players.toIntOrNull() ?: return@Button
                    val i = impostors.toIntOrNull() ?: return@Button

                    if (isReconfig) {
                        onConfirm?.invoke(p, i)
                    } else {
                        vm.setupGame(p, i)
                    }
                },
                enabled = vm.words.isNotEmpty()
            ) {
                Text(if (isReconfig) "Confirmar cambios" else "Iniciar juego")
            }
        }
    }
}
@Composable
fun DropdownMenuBox(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(if (selected.isBlank()) "Elegir categoría" else selected)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}
