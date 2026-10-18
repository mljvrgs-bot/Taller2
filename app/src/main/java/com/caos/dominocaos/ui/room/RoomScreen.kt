package com.caos.dominocaos.ui.screens.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caos.dominocaos.viewmodel.RoomViewModel

/** Sala de espera: codigo para compartir, jugadores conectados y boton de inicio para el anfitrion. */
@Composable
fun RoomScreen(
    code: String,
    onStarted: () -> Unit,
    onLeave: () -> Unit,
    viewModel: RoomViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsState()

    LaunchedEffect(code) { viewModel.observe(code) }
    // Cuando el anfitrion inicia, todos pasan al tablero.
    LaunchedEffect(state.game?.started) {
        if (state.game?.started == true) onStarted()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Text("Codigo de la sala", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(code, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)

            Text("Jugadores (${state.game?.players?.size ?: 0}/4)", style = MaterialTheme.typography.titleMedium)
            state.game?.players?.forEach {
                Text(if (it.id == state.game?.hostId) "${it.name} (anfitrion)" else it.name)
            }

            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            if (state.isHost) {
                Button(
                    onClick = viewModel::start,
                    enabled = state.canStart,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.canStart) "Iniciar partida" else "Esperando jugadores...") }
            } else {
                Text("Esperando a que el anfitrion inicie...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onLeave) { Text("Salir") }
        }
    }
}