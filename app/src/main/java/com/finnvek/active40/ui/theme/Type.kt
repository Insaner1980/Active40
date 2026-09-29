package com.finnvek.active40.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.finnvek.active40.R

// Fontit ovat sovelluksen mukana muuttuvina fontteina (wght-akseli), ei ladata ajon aikana.
private fun variableFamily(resId: Int) =
    FontFamily(
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
            Font(
                resId = resId,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        },
    )

/** Kaikki teksti. */
val DmSans = variableFamily(R.font.dm_sans)

/**
 * Suuret mittariluvut ja ajastimet. DM Sansissa ei ole tasalevyisiä numeroita (tnum),
 * joten luvuille käytetään Plus Jakarta Sansia, jonka numeromuodot ovat lähellä DM Sansia.
 */
val NumberFont = variableFamily(R.font.plus_jakarta_sans)

private val base = TextStyle(fontFamily = DmSans)

/**
 * Tyyppiasteikko suunnitelman osion 6 mukaan. Kaikki koot sp-yksiköissä, jotta ne skaalautuvat.
 * - headlineMedium: näkymän otsikko 30sp
 * - titleMedium: osion otsikko 16sp
 * - bodyLarge / bodyMedium: leipäteksti 15sp
 * - bodySmall: toissijainen teksti 13sp
 * - displaySmall: suuret mittariluvut 34sp, NumberFont tasalevyisin numeroin
 */
val Active40Typography =
    Typography(
        displaySmall =
            base.copy(
                fontFamily = NumberFont,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                fontFeatureSettings = "tnum",
            ),
        headlineMedium =
            base.copy(
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.6).sp,
            ),
        titleLarge = base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = base.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = base.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
    )
