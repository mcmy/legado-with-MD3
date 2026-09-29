package io.legado.app.domain.model.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickReadStyleTest {
    @Test
    fun `selected styles cycle and wrap`() {
        val selected = listOf(false, true, false, true)
        assertEquals(1, nextQuickStyleIndex(0, selected))
        assertEquals(3, nextQuickStyleIndex(1, selected))
        assertEquals(1, nextQuickStyleIndex(3, selected))
    }

    @Test
    fun `without selected styles fallback switches first two`() {
        assertEquals(1, nextQuickStyleIndex(0, listOf(false, false, false)))
        assertEquals(0, nextQuickStyleIndex(2, listOf(false, false, false)))
        assertEquals(0, nextQuickStyleIndex(0, listOf(false)))
        assertNull(nextQuickStyleIndex(0, emptyList()))
    }
}
