package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.impostorapp.domain.model.CategoryMode
import com.matthew.impostorapp.domain.model.GameConfig

@ExperimentalLayoutApi
@Composable
fun LobbyScreen(
    categories: List<String>,
    onStartGame: (GameConfig) -> Unit
) {
    var players by remember { mutableStateOf(6) }
    var impostors by remember { mutableStateOf(1) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            "IMPOSTOR",
            style = MaterialTheme.typography.headlineLarge
        )

        // ===== JUGADORES =====
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Jugadores", style = MaterialTheme.typography.titleMedium)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { if (players > 3) players-- }) { Text("−") }
                    Text(players.toString(), style = MaterialTheme.typography.headlineMedium)
                    Button(onClick = { players++ }) { Text("+") }
                }
            }
        }

        // ===== IMPOSTORES =====
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Impostores", style = MaterialTheme.typography.titleMedium)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { if (impostors > 1) impostors-- }
                    ) { Text("−") }

                    Text(impostors.toString(), style = MaterialTheme.typography.headlineMedium)

                    Button(
                        onClick = { if (impostors < players - 1) impostors++ }
                    ) { Text("+") }
                }
            }
        }

        // ===== CATEGORÍAS =====
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Categorías", style = MaterialTheme.typography.titleMedium)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category in selectedCategories,
                            onClick = {
                                selectedCategories =
                                    if (category in selectedCategories)
                                        selectedCategories - category
                                    else
                                        selectedCategories + category
                            },
                            label = { Text(category) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ===== START =====
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedCategories.isNotEmpty(),
            onClick = {
                onStartGame(
                    GameConfig(
                        players = players,
                        impostors = impostors,
                        categoryMode =
                            if (selectedCategories.size == 1)
                                CategoryMode.Single(selectedCategories.first())
                            else
                                CategoryMode.Multiple(selectedCategories)
                    )
                )
            }
        ) {
            Text("INICIAR PARTIDA")
        }
    }
}
