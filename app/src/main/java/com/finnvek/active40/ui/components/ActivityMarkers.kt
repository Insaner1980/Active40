package com.finnvek.active40.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Lajit erottuvat merkissä myös muodon perusteella, ei pelkän värin: voima = neliö, juoksu = ympyrä,
// päivän kirjaus = rengas.
private val defaultMarkerSize = 7.dp

@Composable
fun StrengthMarker(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = defaultMarkerSize,
) {
    Box(modifier.size(size).background(color, RoundedCornerShape(size / 5)))
}

@Composable
fun RunMarker(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = defaultMarkerSize,
) {
    Box(modifier.size(size).background(color, CircleShape))
}

@Composable
fun CheckInMarker(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = defaultMarkerSize,
) {
    Box(modifier.size(size).border(1.dp, color, CircleShape))
}
