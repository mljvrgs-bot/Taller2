package com.caos.dominocaos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.caos.dominocaos.logic.engine.BoardSide
import com.caos.dominocaos.logic.model.Tile

/**
 * Cadena de fichas con scroll horizontal. Los dobles van de pie.
 * highlightSides: extremos donde la ficha elegida se puede colocar (muestra "+").
 */
@Composable
fun BoardView(
    board: List<Tile>,
    modifier: Modifier = Modifier,
    tileSize: Dp = 32.dp,
    highlightSides: Set<BoardSide> = emptySet(),
    onSideClick: (BoardSide) -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxWidth().height(tileSize * 2 + 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (board.isEmpty()) {
            if (highlightSides.isNotEmpty()) {
                SideTarget(tileSize * 2) { onSideClick(BoardSide.RIGHT) }
            } else {
                Text(
                    text = "Tablero vacio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (BoardSide.LEFT in highlightSides) {
                    item(key = "target-left") { SideTarget(tileSize * 2) { onSideClick(BoardSide.LEFT) } }
                }
                items(board, key = { it.id }) { tile ->
                    DominoTileView(tile = tile, horizontal = !tile.isDouble, size = tileSize)
                }
                if (BoardSide.RIGHT in highlightSides) {
                    item(key = "target-right") { SideTarget(tileSize * 2) { onSideClick(BoardSide.RIGHT) } }
                }
            }
        }
    }
}

/** Destino tocable en un extremo del tablero. */
@Composable
private fun SideTarget(height: Dp, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(width = 40.dp, height = height).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
            Text("+", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        }
    }
}