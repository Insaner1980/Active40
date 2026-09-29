package com.finnvek.active40.domain

enum class RunType { EASY, TEMPO, INTERVALS, LONG, OTHER }

/** NONE on oma valinta ("ei arkuutta"), joka poistaa muut valinnat. */
enum class SorenessArea { NONE, LEGS, BACK, SHOULDERS, ARMS, WHOLE_BODY }

/** Arkuusvalinnan vaihto: "None" poistaa muut valinnat ja muun alueen valinta poistaa "None"-valinnan. */
fun toggleSoreness(
    current: Set<SorenessArea>,
    area: SorenessArea,
): Set<SorenessArea> =
    when {
        area in current -> current - area
        area == SorenessArea.NONE -> setOf(SorenessArea.NONE)
        else -> current - SorenessArea.NONE + area
    }
