package com.finnvek.active40.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing

/** Koko rivin kokoinen kytkin (napautusalue koko rivi). onLightSurface valitsee värit vaalealle pinnalle. */
@Composable
fun SwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onLightSurface: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Active40Spacing.minTouchTarget)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (supportingText != null) {
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (onLightSurface) Active40Colors.onSurfaceMuted else Active40Colors.onGlassSecondary,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors =
                if (onLightSurface) {
                    SwitchDefaults.colors(
                        checkedTrackColor = Active40Colors.inkDark,
                        checkedThumbColor = Active40Colors.surfaceWhite,
                        uncheckedTrackColor = Active40Colors.surfaceWhite,
                        uncheckedThumbColor = Active40Colors.onSurfaceMuted,
                        uncheckedBorderColor = Active40Colors.onSurfaceMuted,
                    )
                } else {
                    SwitchDefaults.colors(
                        checkedTrackColor = Active40Colors.surfaceWhite,
                        checkedThumbColor = Active40Colors.inkDark,
                        uncheckedTrackColor = Active40Colors.surfaceGlass,
                        uncheckedThumbColor = Active40Colors.onBackground,
                        uncheckedBorderColor = Active40Colors.onBackground,
                    )
                },
        )
    }
}
