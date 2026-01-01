package com.matthew.impostorapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RoundEndScreen(onNextRound: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Fin de la ronda")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNextRound) {
            Text("Siguiente ronda")
        }
    }
}
