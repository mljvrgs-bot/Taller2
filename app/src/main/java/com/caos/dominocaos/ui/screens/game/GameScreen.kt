package com.caos.dominocaos.ui.screens.game

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun GameScreen(
    gameCode: String?,          // null = offline
    onLeave: () -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val stateOrNull by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(gameCode) { viewModel.start(gameCode) }

    val state = stateOrNull
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cargando...") }
        return
    }

    // Salir (boton o atras): pide confirmacion si la partida sigue en curso.
    val requestLeave: () -> Unit = {
        if (state.game.finished) {
            onLeave()
        } else {
            confirmLeave = true
        }
    }
    BackHandler(onBack = requestLeave)

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ronda ${state.round}/${state.game.maxRounds}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            state.opponents.forEach { opponent ->
                PlayerInfoBar(
                    player = opponent,
                    isCurrent = state.game.currentPlayer?.id == opponent.id,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Tablero: ocupa el espacio sobrante.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BoardView(
                    board = state.game.board,
                    highlightSides = state.highlightSides,
                    onSideClick = viewModel::onSideClick
                )
            }
            state.game.events.lastOrNull()?.let {
                Text(
                    text = it.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
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
                onLeave = requestLeave
            )
        }
    }

    state.pendingEvent?.let { event ->
        AlertDialog(
            onDismissRequest = viewModel::dismissEvent,
            title = { Text("Evento aleatorio") },
            text = { Text(event.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissEvent) { Text("Continuar") } }
        )
    }

    // Confirmacion para abandonar.
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Abandonar la partida") },
            text = { Text("Si sales ahora, la partida se pierde.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    viewModel.leave()
                    onLeave()
                }) { Text("Abandonar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmLeave = false
                }) { Text("Seguir jugando") }
            }
        )
    }

    // Fin de partida (normal o por abandono): resultado y opciones.
    if (state.game.finished) {
        val winner = state.winner
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    when {
                        winner == null -> "Empate"
                        winner.id == state.myId -> "Ganaste"
                        else -> "Gano ${winner.name}"
                    }
                )
            },
            text = {
                Text(
                    state.game.events.lastOrNull()?.message.orEmpty() + "\n\n" +
                            state.game.players.joinToString("\n") { "${it.name}: ${it.score} puntos" }
                )
            },
            confirmButton = {
                // Tras un abandono falta un jugador, asi que no se ofrece jugar de nuevo.
                if (state.abandoned) TextButton(onClick = onLeave) { Text("Salir") }
                else TextButton(onClick = viewModel::restart) { Text("Jugar de nuevo") }
            },
            dismissButton = {
                if (!state.abandoned) TextButton(onClick = onLeave) { Text("Salir") }
            }
        )
    }
}