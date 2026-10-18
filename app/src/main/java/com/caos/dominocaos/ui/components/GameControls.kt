package com.caos.dominocaos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Robar, pasar, reiniciar y salir. No decide nada: recibe que acciones estan permitidas. */
@Composable
fun GameControls(
    canDraw: Boolean,
    canPass: Boolean,
    drawPileSize: Int,
    onDraw: () -> Unit,
    onPass: () -> Unit,
    onRestart: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onDraw, enabled = canDraw) { Text("Robar ($drawPileSize)") }
        OutlinedButton(onClick = onPass, enabled = canPass) { Text("Pasar") }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onRestart) { Text("Reiniciar") }
            TextButton(
                onClick = onLeave,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Salir") }
        }
    }
}