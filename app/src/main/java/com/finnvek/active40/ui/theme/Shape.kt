package com.finnvek.active40.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Active40Shapes {
    /** Pääkortit (viikkokortti, aktiivinen liike). */
    val cardPrimary = RoundedCornerShape(28.dp)

    /** Toissijaiset kortit (lasikortit vihreällä taustalla). */
    val cardSecondary = RoundedCornerShape(24.dp)

    /** Painikkeet ja chipit: täysin pyöristetty. */
    val pill = CircleShape
}

/** Välistykset ja kosketusalueet. */
object Active40Spacing {
    val screenHorizontal = 20.dp
    val minTouchTarget = 48.dp
}

val Active40MaterialShapes =
    Shapes(
        small = Active40Shapes.pill,
        medium = Active40Shapes.cardSecondary,
        large = Active40Shapes.cardPrimary,
        extraLarge = Active40Shapes.cardPrimary,
    )
