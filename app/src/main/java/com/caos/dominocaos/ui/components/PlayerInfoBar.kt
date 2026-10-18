package com.caos.dominocaos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.caos.dominocaos.logic.model.Player

/** Tarjeta de jugador: nombre, fichas y puntaje. El jugador en turno lleva borde de acento. */
@Composable
fun PlayerInfoBar(
    player: Player,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    isMe: Boolean = false
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (isCurrent) 1.5.dp else 1.dp,
            if (isCurrent) accent else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "${player.name} (tu)" else player.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isCurrent) "Turno - ${player.hand.size} fichas" else "${player.hand.size} fichas",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = player.score.toString(), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "puntos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}