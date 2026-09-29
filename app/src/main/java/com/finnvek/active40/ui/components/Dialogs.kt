package com.finnvek.active40.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.finnvek.active40.R
import com.finnvek.active40.ui.theme.Active40Colors

/** Vaalea dialogi, jonka värit tulevat tokeneista (Material-oletusväreillä teksti olisi valkoista vaalealla). */
@Composable
fun Active40Dialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissText: String = stringResource(R.string.action_cancel),
    content: (@Composable () -> Unit)? = null,
) {
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = Active40Colors.onSurface)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = content,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled, colors = buttonColors) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss, colors = buttonColors) { Text(dismissText) } },
        containerColor = Active40Colors.surfaceLight,
        titleContentColor = Active40Colors.onSurface,
        textContentColor = Active40Colors.onSurface,
    )
}

/** Vahvistusdialogi, esim. poistolle. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Active40Dialog(title = title, confirmText = confirmText, onConfirm = onConfirm, onDismiss = onDismiss) {
        Text(message)
    }
}

/**
 * Dialogi yhden tekstin syöttämiseen (lisää, nimeä uudelleen, muistiinpano). validate palauttaa virheilmoituksen
 * tai null. Tyhjä arvo kelpaa vain, jos allowBlank (esim. muistiinpanon tyhjennys).
 */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialValue: String = "",
    singleLine: Boolean = true,
    allowBlank: Boolean = false,
    validate: (String) -> String? = { null },
) {
    var value by rememberSaveable { mutableStateOf(initialValue) }
    val error = if (value.isBlank()) null else validate(value.trim())
    Active40Dialog(
        title = title,
        confirmText = confirmText,
        onConfirm = { onConfirm(value.trim()) },
        onDismiss = onDismiss,
        confirmEnabled = (allowBlank || value.isNotBlank()) && error == null,
    ) {
        Column {
            Active40TextField(
                value = value,
                onValueChange = { value = it },
                label = label,
                onLightSurface = true,
                singleLine = singleLine,
                capitalization = KeyboardCapitalization.Sentences,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Text(error, color = Active40Colors.onSurface)
            }
        }
    }
}
