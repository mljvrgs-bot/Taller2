package com.caos.dominocaos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.caos.dominocaos.logic.engine.PowerManager
import com.caos.dominocaos.logic.model.Player
import com.caos.dominocaos.logic.model.PowerType

private fun powerHint(power: PowerType): String = when (power) {
    PowerType.WILDCARD -> "Ignora un extremo"
    PowerType.SKIP -> "El siguiente pierde turno"
    PowerType.SWAP -> "Cambia una ficha"
    PowerType.REVERSE -> "Invierte el sentido"
    PowerType.NONE -> ""
}

/** Boton de poder. active: es el elegido ahora. enabled: se puede elegir. */
@Composable
fun PowerButton(
    power: PowerType,
    usesLeft: Int,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .alpha(if (enabled || active) 1f else 0.4f)
            .clickable(enabled = enabled || active, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (active) 1.5.dp else 1.dp,
            if (active) accent else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = PowerManager.label(power),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) accent else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "x$usesLeft",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = powerHint(power),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Poderes del jugador en cuadricula 2x2. Tocar el poder activo lo cancela. */
@Composable
fun PowerBar(
    player: Player,
    activePower: PowerType,
    isMyTurn: Boolean,
    onPowerClick: (PowerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PowerType.usable().chunked(2).forEach { rowPowers ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowPowers.forEach { power ->
                    PowerButton(
                        power = power,
                        usesLeft = player.usesLeft(power),
                        active = power == activePower,
                        enabled = isMyTurn && activePower == PowerType.NONE && player.usesLeft(power) > 0,
                        onClick = { onPowerClick(power) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}