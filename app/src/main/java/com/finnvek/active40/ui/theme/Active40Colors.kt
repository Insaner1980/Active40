package com.finnvek.active40.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Sovelluksen kaikki värit. Näkymät eivät käytä kovakoodattuja värejä, vaan viittaavat näihin tokeneihin.
 * Sama vihreä teema on käytössä sekä järjestelmän vaaleassa että tummassa tilassa.
 */
object Active40Colors {
    // Suunnitelman #557771 tummennettu, jotta onBackgroundMuted saavuttaa 4.5:1 kontrastin myös taustan yläosassa.
    val backgroundTop = Color(0xFF4B6963)
    val backgroundBottom = Color(0xFF2C4642)
    val onBackground = Color(0xFFFFFFFF)
    val onBackgroundMuted = Color(0xFFDCE7E3)

    val surfaceLight = Color(0xFFEAF0ED)
    val surfaceWhite = Color(0xFFFFFFFF)
    val onSurface = Color(0xFF16302C)
    val onSurfaceMuted = Color(0xFF46605B)

    val surfaceGlass = Color.White.copy(alpha = 0.12f)
    val outlineGlass = Color.White.copy(alpha = 0.20f)

    // Lasikortilla himmeä teksti jäisi alle 4.5:1 taustan yläosassa, joten toissijainen teksti on valkoista.
    // Hierarkia tehdään koolla ja painolla.
    val onGlassSecondary = onBackground

    val inkDark = Color(0xFF16302C)

    // Aksenttipari on vielä avoin päätös (suunnitelma 10). Vaihda vain nämä rivit.
    // Teksti aksenttivärin päällä on aina onSurface.
    val accentStrength = Color(0xFFF2A66F)
    val accentRun = Color(0xFFD3E57C)

    // Tummemmat variantit vaaleille pinnoille (esim. kaavioviivat ja merkit surfaceLight-kortilla).
    val accentStrengthOnLight = Color(0xFFC8672A)
    val accentRunOnLight = Color(0xFF6E8A12)
}
