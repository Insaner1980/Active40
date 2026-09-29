package com.finnvek.active40.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.finnvek.active40.ui.theme.Active40Colors

private val backgroundBrush =
    Brush.verticalGradient(listOf(Active40Colors.backgroundTop, Active40Colors.backgroundBottom))

/** Vihreä liukuväritausta kaikkien näkymien alle. Oletustekstiväri on onBackground. */
@Composable
fun GreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(backgroundBrush)) {
        CompositionLocalProvider(LocalContentColor provides Active40Colors.onBackground, content = content)
    }
}
