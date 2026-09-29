package com.finnvek.active40.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Shapes

private val defaultCardPadding = PaddingValues(18.dp)

/** Vaalea pääkortti (esim. viikkokortti, aktiivinen liike). Teksti onSurface-väreillä. */
@Composable
fun LightCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = defaultCardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardSurface(
        modifier = modifier,
        onClick = onClick,
        color = Active40Colors.surfaceLight,
        contentColor = Active40Colors.onSurface,
        shape = Active40Shapes.cardPrimary,
        border = null,
        contentPadding = contentPadding,
        content = content,
    )
}

/** Läpikuultava toissijainen kortti vihreällä taustalla. Teksti valkoisella. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = defaultCardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardSurface(
        modifier = modifier,
        onClick = onClick,
        color = Active40Colors.surfaceGlass,
        contentColor = Active40Colors.onBackground,
        shape = Active40Shapes.cardSecondary,
        border = BorderStroke(1.dp, Active40Colors.outlineGlass),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
private fun CardSurface(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    color: Color,
    contentColor: Color,
    shape: Shape,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inner: @Composable () -> Unit = { Column(Modifier.padding(contentPadding), content = content) }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            border = border,
            content = inner,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            border = border,
            content = inner,
        )
    }
}
