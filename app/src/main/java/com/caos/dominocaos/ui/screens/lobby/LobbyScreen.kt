package com.caos.dominocaos.ui.screens.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caos.dominocaos.viewmodel.AuthViewModel
import com.caos.dominocaos.viewmodel.LobbyViewModel

/** Lobby: crear sala, unirse con codigo, jugar offline y cerrar sesion. */
@Composable
fun LobbyScreen(
    onRoom: (String) -> Unit,
    onPlayOffline: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: LobbyViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsState()
    var code by rememberSaveable { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Text("Hola, ${viewModel.userLabel()}", style = MaterialTheme.typography.headlineMedium)

            Button(
                onClick = { viewModel.create(onRoom) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.loading) "Cargando..." else "Crear partida") }

            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().take(5) },
                label = { Text("Codigo de la sala") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { viewModel.join(code, onRoom) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Unirme") }

            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            TextButton(onClick = onPlayOffline) { Text("Jugar offline contra el rival") }
            TextButton(onClick = {
                authViewModel.signOut()
                onSignOut()
            }) { Text("Cerrar sesion") }
        }
    }
}