package com.caos.dominocaos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.caos.dominocaos.logic.model.Tile

/** Mano del jugador local. playableIds: fichas jugables (las demas se atenuan). */
@Composable
fun PlayerHand(
    hand: List<Tile>,
    selectedId: String?,
    playableIds: Set<String>,
    onTileClick: (Tile) -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp = 44.dp
) {
    if (hand.isEmpty()) {
        Text(
            text = "Sin fichas",
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        // Espacio arriba para que la ficha elegida (que sube) no se recorte.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(hand, key = { it.id }) { tile ->
            DominoTileView(
                tile = tile,
                size = tileSize,
                selected = tile.id == selectedId,
                enabled = tile.id in playableIds,
                onClick = { onTileClick(tile) }
            )
        }
    }
}