package com.nbradbury.memorymap.ui

import com.nbradbury.memorymap.data.IndexState
import com.nbradbury.memorymap.domain.Frame
import com.nbradbury.memorymap.domain.place
import com.nbradbury.memorymap.domain.photo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BuildUiStateTest {
    private val today = LocalDate.of(2026, 10, 29)
    private val lisbon = place("lis", 38.7, -9.1, photo(2026, 6, 8), photo(2026, 6, 9)).copy(name = "Lisbon", country = "Portugal")
    private val ready = IndexState.Ready(listOf(lisbon), setOf(YearMonth.of(2026, 6)), minYear = 2025, partialAccess = false)

    @Test
    fun `tapping months picks a start then an end, in either order`() {
        val p = Picker(YearMonth.of(2026, 3), YearMonth.of(2026, 6), 2026)
        val started = p.tap(YearMonth.of(2026, 8))
        assertEquals(Picker(YearMonth.of(2026, 8), null, 2026), started)
        assertEquals(YearMonth.of(2026, 2), started.tap(YearMonth.of(2026, 2)).from)
        assertEquals(YearMonth.of(2026, 8), started.tap(YearMonth.of(2026, 2)).to)
        assertEquals(YearMonth.of(2026, 9), started.tap(YearMonth.of(2026, 9)).to)
    }

    @Test
    fun `stat line and empty state`() {
        val s = defaultSelection(today)
        assertEquals("1 place · 2 photos", buildUiState(ready, s, today).statLine)
        val empty = buildUiState(ready, s.copy(frame = Frame.ThisMonth), today)
        assertTrue(empty.noPlaces)
        assertEquals("0 places · 0 photos", empty.statLine)
        assertFalse(buildUiState(IndexState.Scanning(0, 0), s, today).noPlaces)
        assertEquals("Scanning photos… 25 of 1,200", buildUiState(IndexState.Scanning(25, 1200), s, today).statLine)
    }

    @Test
    fun `custom chip shows the applied span`() {
        val s = defaultSelection(today).let { it.copy(frame = Frame.Custom) }
        assertEquals("Jul – Oct 2026", buildUiState(ready, s, today).chips.single { it.selected }.label)
    }

    @Test
    fun `picker disables future months and months before the first photo`() {
        val s = defaultSelection(today).copy(picker = Picker(YearMonth.of(2026, 6), null, 2026))
        val p = buildUiState(ready, s, today).picker!!
        assertEquals(listOf(true, true, true, true, true, true, true, true, true, true, false, false), p.months.map { it.enabled })
        assertTrue(p.months[5].hasPhotos && p.months[5].endpoint)
        assertTrue(p.canPrev)
        assertFalse(p.canNext)
        assertEquals("June 2026", p.draftLabel)
    }

    @Test
    fun `carousel describes the selected place`() {
        val s = defaultSelection(today).copy(placeId = "lis")
        val c = buildUiState(ready, s, today).carousel!!
        assertEquals("Portugal · Jun 8 – 9, 2026", c.subtitle)
        assertEquals(listOf("Jun 8, 2026", "Jun 9, 2026"), c.slides.map { it.date })
        assertNull(buildUiState(ready, s.copy(frame = Frame.LastYear), today).carousel)
    }
}
