package com.finnvek.active40.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SorenessTest {
    @Test
    fun `none deselects others`() {
        assertEquals(setOf(SorenessArea.NONE), toggleSoreness(setOf(SorenessArea.LEGS, SorenessArea.BACK), SorenessArea.NONE))
    }

    @Test
    fun `area deselects none`() {
        assertEquals(setOf(SorenessArea.LEGS), toggleSoreness(setOf(SorenessArea.NONE), SorenessArea.LEGS))
    }

    @Test
    fun `tapping selected area removes it`() {
        assertEquals(setOf(SorenessArea.BACK), toggleSoreness(setOf(SorenessArea.LEGS, SorenessArea.BACK), SorenessArea.LEGS))
        assertEquals(emptySet<SorenessArea>(), toggleSoreness(setOf(SorenessArea.NONE), SorenessArea.NONE))
    }
}
