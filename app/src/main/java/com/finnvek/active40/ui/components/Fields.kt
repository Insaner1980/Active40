package com.finnvek.active40.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Shapes

/**
 * Täytetty pill-tekstikenttä. onLightSurface = true, kun kenttä on vaalealla kortilla (valkoinen kenttä),
 * muuten kenttä on vaalea vihreällä taustalla.
 */
@Composable
fun Active40TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onLightSurface: Boolean = false,
    placeholder: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Done,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val container = if (onLightSurface) Active40Colors.surfaceWhite else Active40Colors.surfaceLight
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        shape = if (singleLine) Active40Shapes.pill else Active40Shapes.cardSecondary,
        keyboardOptions =
            KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType, imeAction = imeAction),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                errorContainerColor = container,
                focusedTextColor = Active40Colors.onSurface,
                unfocusedTextColor = Active40Colors.onSurface,
                focusedLabelColor = Active40Colors.onSurfaceMuted,
                unfocusedLabelColor = Active40Colors.onSurfaceMuted,
                focusedPlaceholderColor = Active40Colors.onSurfaceMuted,
                unfocusedPlaceholderColor = Active40Colors.onSurfaceMuted,
                cursorColor = Active40Colors.onSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedSupportingTextColor = Active40Colors.onBackground,
                unfocusedSupportingTextColor = Active40Colors.onBackground,
                errorSupportingTextColor = Active40Colors.onBackground,
                errorLabelColor = Active40Colors.onSurfaceMuted,
                errorTextColor = Active40Colors.onSurface,
                errorCursorColor = Active40Colors.onSurface,
            ),
    )
}
