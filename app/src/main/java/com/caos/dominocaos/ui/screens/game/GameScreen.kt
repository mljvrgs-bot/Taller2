package com.caos.dominocaos.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caos.dominocaos.ui.components.BoardView
import com.caos.dominocaos.ui.components.GameControls
import com.caos.dominocaos.ui.components.PlayerHand
import com.caos.dominocaos.ui.components.PlayerInfoBar
import com.caos.dominocaos.ui.components.PowerBar
import com.caos.dominocaos.viewmodel.GameViewModel

/** Pantalla de juego. Solo dibuja el estado y reenvia los toques al ViewModel. */
@Composable
fun GameScreen(
    gameCode: String?,          // null = offline
    onLeave: () -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val stateOrNull by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Carga la partida una sola vez (offline u online segun el codigo).
    LaunchedEffect(gameCode) { viewModel.start(gameCode) }

    val state = stateOrNull
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cargando...") }
        return
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Oponentes arriba (turno y puntaje siempre visibles, lo exige el enunciado).
            state.opponents.forEach { opponent ->
                PlayerInfoBar(
                    player = opponent,
                    isCurrent = state.game.currentPlayer?.id == opponent.id,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Tablero: ocupa el espacio sobrante.
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BoardView(
                    board = state.game.board,
                    highlightSides = state.highlightSides,
                    onSideClick = viewModel::onSideClick
                )
            }

            state.hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            PlayerInfoBar(
                player = state.me,
                isCurrent = state.isMyTurn,
                isMe = true,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            PowerBar(
                player = state.me,
                activePower = state.game.activePower,
                isMyTurn = state.isMyTurn,
                onPowerClick = viewModel::onPowerClick
            )
            PlayerHand(
                hand = state.me.hand,
                selectedId = state.selectedTileId,
                playableIds = state.playableIds,
                onTileClick = viewModel::onTileClick
            )
            GameControls(
                canDraw = state.canDraw,
                canPass = state.canPass,
                drawPileSize = state.game.drawPile.size,
                onDraw = viewModel::onDraw,
                onPass = viewModel::onPass,
                onRestart = viewModel::restart,
                onLeave = onLeave
            )
        }
    }

    // Fin de partida: resultado y opciones.
    if (state.game.finished) {
        val winner = state.winner
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (winner?.id == state.myId) "Ganaste" else "Gano ${winner?.name ?: "nadie"}") },
            text = { Text(state.game.players.joinToString("\n") { "${it.name}: ${it.score} puntos" }) },
            confirmButton = { TextButton(onClick = viewModel::restart) { Text("Jugar de nuevo") } },
            dismissButton = { TextButton(onClick = onLeave) { Text("Salir") } }
        )
    }
}