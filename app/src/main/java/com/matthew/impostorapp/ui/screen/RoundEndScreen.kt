package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RoundEndScreen(onNextRound: () -> Unit) {

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {

            Text("Fin de ronda", color = Color.White, style = MaterialTheme.typography.headlineLarge)

            Spacer(Modifier.height(32.dp))

            Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                Text("Nueva ronda")
            }
        }
    }
}
