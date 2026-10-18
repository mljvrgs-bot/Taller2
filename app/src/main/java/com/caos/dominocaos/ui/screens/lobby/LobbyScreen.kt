package com.caos.dominocaos.ui.screens.lobby

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caos.dominocaos.viewmodel.AuthViewModel

/** Lobby minimo. Despues se agregan "Crear partida" y "Unirse con codigo". */
@Composable
fun LobbyScreen(
    onPlayOffline: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Text("Hola, ${viewModel.userLabel()}", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onPlayOffline, modifier = Modifier.fillMaxWidth()) {
                Text("Jugar contra el rival (offline)")
            }
            TextButton(onClick = {
                viewModel.signOut()
                onSignOut()
            }) { Text("Cerrar sesion") }
        }
    }
}