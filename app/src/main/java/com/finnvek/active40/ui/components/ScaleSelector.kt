package com.finnvek.active40.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Shapes

/** Valitun ja valitsemattoman pillin värit. Valinta näkyy täyttönä ja lihavointina, ei pelkkänä värinä. */
data class ChipColors(
    val selectedContainer: Color,
    val selectedContent: Color,
    val unselectedContainer: Color,
    val unselectedContent: Color,
    val unselectedBorder: Color,
)

/** Pillit vihreällä taustalla: valittu valkoinen. */
val chipColorsOnBackground =
    ChipColors(
        selectedContainer = Active40Colors.surfaceWhite,
        selectedContent = Active40Colors.onSurface,
        unselectedContainer = Active40Colors.surfaceGlass,
        unselectedContent = Active40Colors.onBackground,
        unselectedBorder = Active40Colors.outlineGlass,
    )

/** Pillit vaalealla pinnalla: valittu tumma. */
val chipColorsOnLight =
    ChipColors(
        selectedContainer = Active40Colors.inkDark,
        selectedContent = Active40Colors.onBackground,
        unselectedContainer = Active40Colors.surfaceWhite,
        unselectedContent = Active40Colors.onSurface,
        unselectedBorder = Color.Transparent,
    )

/** Valittava pilli (chip). */
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    colors: ChipColors,
    modifier: Modifier = Modifier,
    semanticLabel: String? = null,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .then(if (semanticLabel != null) Modifier.semantics { contentDescription = semanticLabel } else Modifier),
        shape = Active40Shapes.pill,
        color = if (selected) colors.selectedContainer else colors.unselectedContainer,
        contentColor = if (selected) colors.selectedContent else colors.unselectedContent,
        border = if (selected) null else BorderStroke(1.dp, colors.unselectedBorder),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

/**
 * Asteikkovalitsin (esim. rasitus 1 to 10, uni 1 to 5). Valitun napauttaminen uudelleen poistaa valinnan,
 * koska kaikki asteikot ovat valinnaisia.
 */
@Composable
fun ScaleSelector(
    range: IntRange,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    colors: ChipColors,
    modifier: Modifier = Modifier,
    columns: Int = range.count(),
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        range.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { value ->
                    SelectableChip(
                        text = value.toString(),
                        selected = value == selected,
                        onClick = { onSelect(if (value == selected) null else value) },
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
