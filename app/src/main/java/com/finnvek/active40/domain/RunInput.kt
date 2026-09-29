package com.finnvek.active40.domain

enum class RunInputError { DISTANCE, DURATION, TIME_PARTS }

private const val MAX_MINUTES_OR_SECONDS = 59

/** Kesto sekunteina tunneista, minuuteista ja sekunneista. Tyhjä kenttä on nolla. Null, jos osat ovat virheellisiä. */
fun parseDurationSeconds(
    hours: String,
    minutes: String,
    seconds: String,
): Long? {
    val h = hours.ifBlank { "0" }.toLongOrNull() ?: return null
    val m = minutes.ifBlank { "0" }.toLongOrNull() ?: return null
    val s = seconds.ifBlank { "0" }.toLongOrNull() ?: return null
    if (h < 0 || m !in 0..MAX_MINUTES_OR_SECONDS || s !in 0..MAX_MINUTES_OR_SECONDS) return null
    return h * 3600 + m * 60 + s
}

/** Tarkistaa juoksun syötteet: matkan ja keston on oltava suurempia kuin nolla. */
fun validateRun(
    distanceText: String,
    hours: String,
    minutes: String,
    seconds: String,
): Set<RunInputError> {
    val errors = mutableSetOf<RunInputError>()
    if ((parseDecimal(distanceText) ?: 0.0) <= 0.0) errors += RunInputError.DISTANCE
    val duration = parseDurationSeconds(hours, minutes, seconds)
    when {
        duration == null -> errors += RunInputError.TIME_PARTS
        duration <= 0 -> errors += RunInputError.DURATION
    }
    return errors
}
