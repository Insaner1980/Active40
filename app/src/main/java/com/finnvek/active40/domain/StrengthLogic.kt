package com.finnvek.active40.domain

import com.finnvek.active40.data.db.SetEntry
import java.math.BigDecimal
import java.math.RoundingMode

/** Uuden sarjarivin esitäyttö. */
data class SetValues(
    val weightKg: Double,
    val reps: Int,
)

const val DEFAULT_SET_COUNT = 3

/**
 * Harjoitukseen lisätyn liikkeen alkusarjat.
 * Määrä: rutiinin tavoite, muuten edellisen kerran sarjamäärä, muuten DEFAULT_SET_COUNT.
 * Arvot: edellisen kerran vastaava sarja; jos sitä ei ole, edellinen rivi tässä harjoituksessa.
 * Toistojen oletus on rutiinin tavoite, jos edellistä kertaa ei ole.
 */
fun initialSetValues(
    targetSets: Int?,
    targetReps: Int?,
    lastTime: List<SetEntry>,
): List<SetValues> {
    val count = targetSets ?: lastTime.size.takeIf { it > 0 } ?: DEFAULT_SET_COUNT
    val result = mutableListOf<SetValues>()
    repeat(count) { index ->
        val previous = lastTime.getOrNull(index)
        result +=
            when {
                previous != null -> SetValues(previous.weightKg, targetReps ?: previous.reps)
                result.isNotEmpty() -> result.last()
                else -> SetValues(0.0, targetReps ?: 0)
            }
    }
    return result
}

/** Uuden, käsin lisätyn sarjan esitäyttö: edellinen sarja tässä harjoituksessa, muuten edellisen kerran ensimmäinen. */
fun nextSetValues(
    currentSets: List<SetEntry>,
    lastTime: List<SetEntry>,
): SetValues {
    val source = currentSets.maxByOrNull { it.position } ?: lastTime.firstOrNull()
    return source?.let { SetValues(it.weightKg, it.reps) } ?: SetValues(0.0, 0)
}

/** "Last time" -rivin rakenne. Käyttöliittymä muotoilee tekstin merkkijonoresursseista. */
sealed interface LastTimeSummary {
    /** Kaikki sarjat samoilla arvoilla, esim. 3 × 10 at 80 kg. */
    data class Uniform(
        val sets: Int,
        val reps: Int,
        val weightKg: Double,
    ) : LastTimeSummary

    /** Sama paino, eri toistot, esim. 10, 9, 8 at 82.5 kg. */
    data class SameWeight(
        val reps: List<Int>,
        val weightKg: Double,
    ) : LastTimeSummary

    /** Eri painot, esim. 10 at 80 kg, 8 at 85 kg. */
    data class Mixed(
        val sets: List<SetValues>,
    ) : LastTimeSummary
}

/** Tiivistää edellisen kerran tehdyt sarjat. Null, jos tehtyjä sarjoja ei ole ("First time"). */
fun summarizeLastTime(sets: List<SetEntry>): LastTimeSummary? {
    val done = sets.filter { it.isCompleted }.sortedBy { it.position }
    if (done.isEmpty()) return null
    val weights = done.map { it.weightKg }.distinct()
    val reps = done.map { it.reps }
    return when {
        weights.size == 1 && reps.distinct().size == 1 -> LastTimeSummary.Uniform(done.size, reps.first(), weights.first())
        weights.size == 1 -> LastTimeSummary.SameWeight(reps, weights.first())
        else -> LastTimeSummary.Mixed(done.map { SetValues(it.weightKg, it.reps) })
    }
}

/** Näyttää luvun enintään maxDecimals desimaalilla ilman turhia nollia: 80, 82.5, 181.88. */
fun formatDecimal(
    value: Double,
    maxDecimals: Int = 2,
): String =
    BigDecimal
        .valueOf(value)
        .setScale(maxDecimals, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

/** Painon esitys valitussa yksikössä ilman yksikkömerkintää. */
fun formatWeight(
    kg: Double,
    unit: WeightUnit,
): String = formatDecimal(unit.fromKg(kg))

/** Lukee käyttäjän syöttämän desimaaliluvun; hyväksyy sekä pisteen että pilkun. */
fun parseDecimal(text: String): Double? =
    text
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it >= 0.0 && it.isFinite() }
