package com.finnvek.active40.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UnitsTest {
    private val eps = 1e-9

    @Test
    fun `kg to lb and back`() {
        assertEquals(220.462262, WeightUnit.LB.fromKg(100.0), 1e-6)
        assertEquals(100.0, WeightUnit.LB.toKg(WeightUnit.LB.fromKg(100.0)), eps)
        assertEquals(45.359237, WeightUnit.LB.toKg(100.0), eps)
    }

    @Test
    fun `kg unit is identity`() {
        assertEquals(82.5, WeightUnit.KG.fromKg(82.5), eps)
        assertEquals(82.5, WeightUnit.KG.toKg(82.5), eps)
    }

    @Test
    fun `meters to km and miles`() {
        assertEquals(5.0, DistanceUnit.KM.fromMeters(5000.0), eps)
        assertEquals(1.0, DistanceUnit.MI.fromMeters(1609.344), eps)
        assertEquals(8046.72, DistanceUnit.MI.toMeters(5.0), eps)
        assertEquals(10500.0, DistanceUnit.KM.toMeters(10.5), eps)
    }

    @Test
    fun `distance formatting`() {
        assertEquals("5.24", formatDistance(5240.0, DistanceUnit.KM))
        assertEquals("18.4", formatDistance(18420.0, DistanceUnit.KM, maxDecimals = 1))
        assertEquals("3.11", formatDistance(5000.0, DistanceUnit.MI))
    }

    @Test
    fun `default units follow locale country`() {
        assertEquals(WeightUnit.LB, defaultWeightUnit(Locale.US))
        assertEquals(DistanceUnit.MI, defaultDistanceUnit(Locale.US))
        assertEquals(WeightUnit.KG, defaultWeightUnit(Locale.UK))
        assertEquals(DistanceUnit.MI, defaultDistanceUnit(Locale.UK))
        val finland =
            Locale
                .Builder()
                .setLanguage("fi")
                .setRegion("FI")
                .build()
        assertEquals(WeightUnit.KG, defaultWeightUnit(finland))
        assertEquals(DistanceUnit.KM, defaultDistanceUnit(finland))
    }
}
