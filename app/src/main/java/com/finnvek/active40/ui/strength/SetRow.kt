package com.finnvek.active40.ui.strength

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finnvek.active40.R
import com.finnvek.active40.data.db.SetEntry
import com.finnvek.active40.domain.WeightUnit
import com.finnvek.active40.domain.formatWeight
import com.finnvek.active40.ui.components.scaledWidth
import com.finnvek.active40.ui.label
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing

private val setNumberWidth = 28.dp
private val cellShape = RoundedCornerShape(16.dp)
private const val MAX_WEIGHT_CHARS = 6
private const val MAX_REPS_CHARS = 3
private const val MAX_RPE_CHARS = 2

@Composable
fun SetHeaderRow(unit: WeightUnit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        val style = MaterialTheme.typography.bodySmall
        val color = Active40Colors.onSurfaceMuted
        Text(stringResource(R.string.set_header_set), style = style, color = color, modifier = Modifier.width(scaledWidth(setNumberWidth)))
        Text(unit.label(), style = style, color = color, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.set_header_reps), style = style, color = color, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.set_header_rpe), style = style, color = color, modifier = Modifier.weight(1f))
        Box(Modifier.width(Active40Spacing.minTouchTarget))
    }
}

/**
 * Yksi sarjarivi. Kentät pitävät oman tekstitilansa (kursori ei hyppää), ja jokainen muutos tallennetaan heti.
 * Tehdyksi merkitseminen vaatii toistot.
 */
@Composable
fun SetRow(
    set: SetEntry,
    number: Int,
    unit: WeightUnit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRpeChange: (String) -> Unit,
    onToggleDone: () -> Unit,
) {
    val unitLabel = unit.label()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = Active40Colors.onSurfaceMuted,
            modifier = Modifier.width(scaledWidth(setNumberWidth)),
        )
        NumberCell(
            initialText = if (set.weightKg == 0.0) "" else formatWeight(set.weightKg, unit),
            placeholder = "0",
            description = stringResource(R.string.set_weight_description, number, unitLabel),
            keyboardType = KeyboardType.Decimal,
            filter = { it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(MAX_WEIGHT_CHARS) },
            onChange = onWeightChange,
            modifier = Modifier.weight(1f),
        )
        NumberCell(
            initialText = if (set.reps == 0) "" else set.reps.toString(),
            description = stringResource(R.string.set_reps_description, number),
            filter = { it.filter(Char::isDigit).take(MAX_REPS_CHARS) },
            onChange = onRepsChange,
            modifier = Modifier.weight(1f),
        )
        NumberCell(
            initialText = set.rpe?.toString().orEmpty(),
            description = stringResource(R.string.set_rpe_description, number),
            filter = { it.filter(Char::isDigit).take(MAX_RPE_CHARS) },
            onChange = onRpeChange,
            modifier = Modifier.weight(1f),
        )
        OutlinedIconToggleButton(
            checked = set.isCompleted,
            onCheckedChange = { onToggleDone() },
            enabled = set.isCompleted || set.reps > 0,
            modifier = Modifier.size(Active40Spacing.minTouchTarget),
            colors =
                IconButtonDefaults.outlinedIconToggleButtonColors(
                    contentColor = Active40Colors.onSurfaceMuted,
                    checkedContainerColor = Active40Colors.inkDark,
                    checkedContentColor = Active40Colors.onBackground,
                ),
            border = if (set.isCompleted) null else BorderStroke(1.dp, Active40Colors.onSurfaceMuted),
        ) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.set_mark_done, number),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NumberCell(
    initialText: String,
    description: String,
    filter: (String) -> String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    var text by remember { mutableStateOf(initialText) }
    val style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = Active40Colors.onSurface)
    BasicTextField(
        value = text,
        onValueChange = {
            val filtered = filter(it)
            text = filtered
            onChange(filtered)
        },
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(Active40Colors.onSurface),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = modifier.semantics { contentDescription = description },
        decorationBox = { inner ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier =
                    Modifier
                        .heightIn(min = Active40Spacing.minTouchTarget)
                        .background(Active40Colors.surfaceWhite, cellShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (text.isEmpty() && placeholder != null) {
                    Text(placeholder, style = style.copy(color = Active40Colors.onSurfaceMuted, fontWeight = FontWeight.Normal))
                }
                inner()
            }
        },
    )
}
