package com.finnvek.active40.domain

import java.util.Locale

/** Kellomuoto: m:ss alle tunnin, muuten h:mm:ss. Negatiiviset arvot näytetään nollana. */
fun formatClock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val seconds = s % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(Locale.ROOT, hours, minutes, seconds)
    } else {
        "%d:%02d".format(Locale.ROOT, minutes, seconds)
    }
}
