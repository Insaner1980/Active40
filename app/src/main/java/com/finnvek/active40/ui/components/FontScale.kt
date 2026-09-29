package com.finnvek.active40.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val LARGE_FONT_SCALE = 1.5f

/** Suuri järjestelmän fonttikoko: vierekkäiset elementit pinotaan, jotta sanat eivät katkea kesken. */
@Composable
fun isLargeFontScale(): Boolean = LocalDensity.current.fontScale >= LARGE_FONT_SCALE

/**
 * Rajoittaa tekstin skaalautumisen (esim. alanavigaation otsikot, joilla on kiinteä leveys).
 * Teksti kasvaa edelleen järjestelmän asetuksen mukana, mutta enintään maxScale-kertaiseksi.
 */
@Composable
fun cappedFontSize(
    base: TextUnit,
    maxScale: Float,
): TextUnit {
    val scale = LocalDensity.current.fontScale
    return if (scale <= maxScale) base else (base.value * maxScale / scale).sp
}

/**
 * Rivi, joka muuttuu sarakkeeksi suurella fonttikoolla. Sisältö saa valmiin Modifierin
 * (rivissä tasaleveys, sarakkeessa koko leveys).
 */
@Composable
fun AdaptiveRow(
    spacing: Dp = 10.dp,
    content: @Composable (Modifier) -> Unit,
) {
    if (isLargeFontScale()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) { content(Modifier.fillMaxWidth()) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) { content(Modifier.weight(1f)) }
    }
}

/** Kiinteä leveys, joka kasvaa järjestelmän fonttikoon mukana (esim. sarjanumerosarake). */
@Composable
fun scaledWidth(base: Dp): Dp = base * LocalDensity.current.fontScale.coerceAtLeast(1f)
