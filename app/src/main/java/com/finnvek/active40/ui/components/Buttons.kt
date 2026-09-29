package com.finnvek.active40.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Shapes
import com.finnvek.active40.ui.theme.Active40Spacing

private val buttonPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
private val largeButtonHeight = 56.dp

/** Täytetty pill-painike. Oletuksena tumma (inkDark); aksenttiväreillä teksti on aina onSurface. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Active40Colors.inkDark,
    contentColor: Color = Active40Colors.onBackground,
    @DrawableRes iconRes: Int? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = largeButtonHeight),
        enabled = enabled,
        shape = Active40Shapes.pill,
        contentPadding = buttonPadding,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = 0.4f),
                disabledContentColor = contentColor.copy(alpha = 0.6f),
            ),
    ) {
        ButtonContent(text, iconRes)
    }
}

/** Ääriviivallinen pill-painike vihreälle taustalle. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Active40Spacing.minTouchTarget),
        enabled = enabled,
        shape = Active40Shapes.pill,
        contentPadding = buttonPadding,
        border = BorderStroke(1.dp, Active40Colors.outlineGlass),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = Active40Colors.surfaceGlass,
                contentColor = Active40Colors.onBackground,
                disabledContentColor = Active40Colors.onBackground.copy(alpha = 0.5f),
            ),
    ) {
        ButtonContent(text, iconRes)
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    @DrawableRes iconRes: Int?,
) {
    if (iconRes != null) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
    }
    Text(text, textAlign = TextAlign.Center)
}

/** Pyöreä ikonipainike lasipinnalla (takaisin, valikko). contentDescription on pakollinen. */
@Composable
fun GlassIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(Active40Spacing.minTouchTarget),
        shape = Active40Shapes.pill,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = Active40Colors.surfaceGlass,
                contentColor = Active40Colors.onBackground,
            ),
    ) {
        Icon(painterResource(iconRes), contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

/** Tavallinen ikonipainike, väri perii ympäristöstä. */
@Composable
fun PlainIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier.size(Active40Spacing.minTouchTarget), enabled = enabled) {
        Icon(painterResource(iconRes), contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}
