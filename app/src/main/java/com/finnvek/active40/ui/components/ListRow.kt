package com.finnvek.active40.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finnvek.active40.R
import com.finnvek.active40.ui.theme.Active40Colors

private val rowPadding = PaddingValues(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)

/** Tiivis lasikorttirivi: otsikko, valinnainen lisärivi ja toiminnot oikealla. */
@Composable
fun GlassListRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = rowPadding) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (supportingText != null) {
                    Text(supportingText, style = MaterialTheme.typography.bodySmall, color = Active40Colors.onGlassSecondary)
                }
            }
            trailing()
        }
    }
}

data class MenuAction(
    val label: String,
    val onClick: () -> Unit,
)

/** Kolmen pisteen valikko. contentDescription kertoo, minkä kohteen valikko on. */
@Composable
fun OverflowMenu(
    contentDescription: String,
    actions: List<MenuAction>,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PlainIconButton(R.drawable.ic_more, contentDescription, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
