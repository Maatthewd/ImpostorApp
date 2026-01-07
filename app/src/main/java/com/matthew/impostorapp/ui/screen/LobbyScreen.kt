package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.impostorapp.domain.model.CategoryMode
import com.matthew.impostorapp.domain.model.GameConfig

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    categories: List<String>,
    onStartGame: (GameConfig) -> Unit,
    onManageCategories: () -> Unit,
    getWordCount: (String) -> Int = { 0 },
    errorMessage: String? = null
) {
    var players by remember { mutableStateOf(6) }
    var impostors by remember { mutableStateOf(1) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IMPOSTOR") },
                actions = {
                    IconButton(onClick = onManageCategories) {
                        Icon(Icons.Default.Settings, "Gestionar categorías")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Mostrar errores
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Categorías", style = MaterialTheme.typography.titleMedium)

                    if (categories.isEmpty()) {
                        Text(
                            "No hay categorías. Agregá algunas desde el menú de gestión.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    categories.forEach { category ->
                                        val wordCount = getWordCount(category)

                                        FilterChip(
                                            selected = category in selectedCategories,
                                            onClick = {
                                                selectedCategories =
                                                    if (category in selectedCategories)
                                                        selectedCategories - category
                                                    else
                                                        selectedCategories + category
                                            },
                                            label = { Text("$category ($wordCount)") },
                                            enabled = wordCount > 0
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

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
}