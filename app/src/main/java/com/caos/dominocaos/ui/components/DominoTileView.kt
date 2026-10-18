package com.caos.dominocaos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.caos.dominocaos.logic.model.Tile
import com.caos.dominocaos.ui.theme.Ink600
import com.caos.dominocaos.ui.theme.Ink700
import com.caos.dominocaos.ui.theme.TileFace
import com.caos.dominocaos.ui.theme.TilePip

/** Posiciones (0..1 dentro de una mitad) de los puntos de cada valor. Funcion pura. */
fun pipPositions(value: Int): List<Pair<Float, Float>> {
    val l = 0.25f
    val c = 0.5f
    val r = 0.75f
    return when (value) {
        1 -> listOf(c to c)
        2 -> listOf(l to l, r to r)
        3 -> listOf(l to l, c to c, r to r)
        4 -> listOf(l to l, r to l, l to r, r to r)
        5 -> listOf(l to l, r to l, c to c, l to r, r to r)
        6 -> listOf(l to l, r to l, l to c, r to c, l to r, r to r)
        else -> emptyList()
    }
}

/**
 * Ficha dibujada con Canvas.
 * horizontal: acostada (tablero). faceDown: reverso (ficha oculta).
 * selected: se eleva y se resalta. enabled=false: se atenua.
 * size: lado corto; el largo es el doble.
 */
@Composable
fun DominoTileView(
    tile: Tile,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    faceDown: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(size * 0.18f)
    val accent = MaterialTheme.colorScheme.primary
    val width = if (horizontal) size * 2 else size
    val height = if (horizontal) size else size * 2

    Box(
        modifier = modifier
            .size(width, height)
            .graphicsLayer {
                translationY = if (selected) -10.dp.toPx() else 0f
                alpha = if (enabled || selected) 1f else 0.4f
            }
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else Ink600, shape)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (faceDown) {
                drawRect(color = Ink700)
                drawDivider(horizontal, Ink600)
            } else {
                drawRect(color = TileFace)
                drawDivider(horizontal, Ink600)
                // Mitad 1: valor izquierdo. Mitad 2: valor derecho.
                val half = if (horizontal) Size(this.size.width / 2f, this.size.height)
                else Size(this.size.width, this.size.height / 2f)
                val second = if (horizontal) Offset(half.width, 0f) else Offset(0f, half.height)
                drawPips(tile.left, Offset.Zero, half, TilePip)
                drawPips(tile.right, second, half, TilePip)
            }
        }
    }
}

private fun DrawScope.drawDivider(horizontal: Boolean, color: Color) {
    if (horizontal) {
        drawLine(color, Offset(size.width / 2f, size.height * 0.15f), Offset(size.width / 2f, size.height * 0.85f), 1.5f)
    } else {
        drawLine(color, Offset(size.width * 0.15f, size.height / 2f), Offset(size.width * 0.85f, size.height / 2f), 1.5f)
    }
}

private fun DrawScope.drawPips(value: Int, origin: Offset, half: Size, color: Color) {
    val radius = minOf(half.width, half.height) * 0.1f
    pipPositions(value).forEach { (fx, fy) ->
        drawCircle(color, radius, Offset(origin.x + half.width * fx, origin.y + half.height * fy))
    }
}