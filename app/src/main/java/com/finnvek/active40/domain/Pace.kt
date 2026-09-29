package com.finnvek.active40.domain

import java.util.Locale
import kotlin.math.roundToLong

/** Vauhti sekunteina valittua matkayksikköä kohden. Null, jos matka tai kesto ei ole positiivinen. */
fun paceSecondsPerUnit(
    distanceMeters: Double,
    durationSeconds: Long,
    unit: DistanceUnit,
): Double? {
    if (distanceMeters <= 0.0 || durationSeconds <= 0L) return null
    return durationSeconds / unit.fromMeters(distanceMeters)
}

/** Muotoilee vauhdin muotoon m:ss. Pyöristys tehdään kokonaissekunteihin ennen jakoa, jotta ei synny 5:60. */
fun formatPace(secondsPerUnit: Double): String {
    val total = secondsPerUnit.roundToLong()
    return "%d:%02d".format(Locale.ROOT, total / 60, total % 60)
}
