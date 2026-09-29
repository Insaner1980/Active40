package com.finnvek.active40.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Kaaviot piirretään Compose Canvasilla (ei kaaviokirjastoa). Jokaisella kaaviolla on tekstikuvaus TalkBackille.

private val chartHeight = 72.dp
private val lineWidth = 3.dp
private val pointRadius = 5.dp
private val barRadius = 6.dp
private val barGap = 6.dp
private const val VALUE_PADDING = 0.1f

/**
 * Viivakaavio tasaisin välein. null-arvot ohitetaan ja viiva yhdistää olemassa olevat pisteet.
 * invert = true kääntää akselin (esim. vauhti, jossa pienempi arvo on parempi, näkyy nousuna).
 */
@Composable
fun LineChart(
    values: List<Double?>,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    invert: Boolean = false,
) {
    val points = values.withIndex().filter { it.value != null }.map { it.index to it.value!! }
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(chartHeight)
                .semantics { contentDescription = description },
    ) {
        if (points.isEmpty()) return@Canvas
        val min = points.minOf { it.second }
        val max = points.maxOf { it.second }
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val pad = pointRadius.toPx()
        val usableHeight = size.height - 2 * pad
        val stepX = if (values.size > 1) (size.width - 2 * pad) / (values.size - 1) else 0f

        fun y(v: Double): Float {
            val fraction = if (max == min) 0.5f else ((v - min) / span).toFloat() * (1 - 2 * VALUE_PADDING) + VALUE_PADDING
            val shown = if (invert) 1 - fraction else fraction
            return pad + usableHeight * (1 - shown)
        }
        val offsets = points.map { (i, v) -> Offset(pad + i * stepX, y(v)) }
        val path = Path().apply { offsets.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(path, color, style = Stroke(width = lineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = pointRadius.toPx(), center = offsets.last())
    }
}

/** Pylväskaavio; viimeinen pylväs (kuluva jakso) korostetaan. */
@Composable
fun BarChart(
    values: List<Double>,
    barColor: Color,
    highlightColor: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(chartHeight)
                .semantics { contentDescription = description },
    ) {
        if (values.isEmpty()) return@Canvas
        val max = values.max().takeIf { it > 0 } ?: 1.0
        val gap = barGap.toPx().coerceAtMost(size.width / values.size / 3)
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        val radius = CornerRadius(barRadius.toPx().coerceAtMost(barWidth / 2))
        values.forEachIndexed { i, v ->
            val height = (v / max).toFloat() * size.height
            if (height <= 0f) return@forEachIndexed
            drawRoundRect(
                color = if (i == values.lastIndex) highlightColor else barColor,
                topLeft = Offset(i * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = radius,
            )
        }
    }
}

/** Kaksiosainen jakopalkki (esim. voima / juoksu harjoitusajasta). */
@Composable
fun SplitBar(
    firstShare: Double,
    firstColor: Color,
    secondColor: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(12.dp)
                .semantics { contentDescription = description },
    ) {
        val gap = 3.dp.toPx()
        val radius = CornerRadius(size.height / 2)
        val firstWidth = ((size.width - gap) * firstShare.toFloat()).coerceIn(0f, size.width)
        if (firstWidth > 0f) drawRoundRect(firstColor, size = Size(firstWidth, size.height), cornerRadius = radius)
        val secondStart = if (firstWidth > 0f) firstWidth + gap else 0f
        if (secondStart < size.width && firstShare < 1.0) {
            drawRoundRect(
                secondColor,
                topLeft = Offset(secondStart, 0f),
                size = Size(size.width - secondStart, size.height),
                cornerRadius = radius,
            )
        }
    }
}
