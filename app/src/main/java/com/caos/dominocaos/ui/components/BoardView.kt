package com.caos.dominocaos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

private sealed interface BoardCell {
    data class TileCell(val tile: Tile) : BoardCell
    data class TargetCell(val side: BoardSide) : BoardCell
}

@Composable
fun BoardView(
    board: List<Tile>,
    modifier: Modifier = Modifier,
    highlightSides: Set<BoardSide> = emptySet(),
    onSideClick: (BoardSide) -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (board.isEmpty() && highlightSides.isEmpty()) {
            Text(
                text = "Tablero vacio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val cells = buildList<BoardCell> {
                if (board.isNotEmpty() && BoardSide.LEFT in highlightSides) add(
                    BoardCell.TargetCell(
                        BoardSide.LEFT
                    )
                )
                board.forEach { add(BoardCell.TileCell(it)) }
                if (BoardSide.RIGHT in highlightSides) add(BoardCell.TargetCell(BoardSide.RIGHT))
            }
            val gap = 4.dp
            val tileSize = fitTileSize(cells.size, maxWidth, maxHeight, gap)
            val perRow = tilesPerRow(maxWidth, tileSize, gap)

            Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                cells.chunked(perRow).forEachIndexed { rowIndex, rowCells ->
                    val reversed = rowIndex % 2 == 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            gap,
                            if (reversed) Alignment.End else Alignment.Start
                        )
                    ) {
                        (if (reversed) rowCells.reversed() else rowCells).forEach { cell ->
                            when (cell) {
                                is BoardCell.TileCell -> DominoTileView(
                                    tile = if (reversed) cell.tile.copy(
                                        left = cell.tile.right,
                                        right = cell.tile.left
                                    ) else cell.tile,
                                    horizontal = true,
                                    size = tileSize
                                )

                                is BoardCell.TargetCell -> SideTarget(
                                    width = tileSize * 2,
                                    height = tileSize,
                                    onClick = { onSideClick(cell.side) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun tilesPerRow(maxW: Dp, size: Dp, gap: Dp): Int =
    ((maxW + gap) / (size * 2 + gap)).toInt().coerceAtLeast(1)

private fun fitTileSize(count: Int, maxW: Dp, maxH: Dp, gap: Dp): Dp {
    var size = 36
    while (size > 16) {
        val perRow = tilesPerRow(maxW, size.dp, gap)
        val rows = (count + perRow - 1) / perRow
        if (size.dp * rows + gap * (rows - 1) <= maxH) break
        size -= 2
    }
    return size.dp
}

@Composable
private fun SideTarget(width: Dp, height: Dp, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(width = width, height = height)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "+",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}