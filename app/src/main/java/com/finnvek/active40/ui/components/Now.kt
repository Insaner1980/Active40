package com.finnvek.active40.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import java.time.Instant

private const val TICK_MS = 1_000L

/** Nykyhetki, joka päivittyy sekunnin välein (ajastinnäytöt). */
@Composable
fun rememberNow(): State<Instant> =
    produceState(Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(TICK_MS - System.currentTimeMillis() % TICK_MS)
        }
    }
