package com.finnvek.active40.data.db

import com.finnvek.active40.domain.SorenessArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `soreness round trip keeps null and none distinct`() {
        assertNull(converters.stringToSoreness(converters.sorenessToString(null)))
        val none = setOf(SorenessArea.NONE)
        assertEquals(none, converters.stringToSoreness(converters.sorenessToString(none)))
        val areas = setOf(SorenessArea.LEGS, SorenessArea.WHOLE_BODY)
        assertEquals(areas, converters.stringToSoreness(converters.sorenessToString(areas)))
    }

    @Test
    fun `dates are stored as sortable iso strings`() {
        assertEquals("2026-09-05", converters.localDateToString(LocalDate.of(2026, 9, 5)))
        assertEquals(LocalDate.of(2026, 9, 5), converters.stringToLocalDate("2026-09-05"))
    }
}
