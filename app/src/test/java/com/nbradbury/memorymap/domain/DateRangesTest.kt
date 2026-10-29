package com.nbradbury.memorymap.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class DateRangesTest {
    private val today = LocalDate.of(2026, 10, 29)
    private val custom = MonthRange(YearMonth.of(2026, 3), YearMonth.of(2026, 6))

    @Test
    fun `fixed frames match the design`() {
        val thisMonth = dateRange(Frame.ThisMonth, custom, today)
        assertEquals("October 2026", thisMonth.desc)
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), thisMonth.from)
        assertEquals(today.atTime(LocalTime.MAX), thisMonth.to)

        val lastMonth = dateRange(Frame.LastMonth, custom, today)
        assertEquals("September 2026", lastMonth.desc)
        assertEquals(LocalDate.of(2026, 9, 30).atTime(LocalTime.MAX), lastMonth.to)

        assertEquals("Jan 1 – Oct 29, 2026", dateRange(Frame.ThisYear, custom, today).desc)
        assertEquals("Jan 1 – Dec 31, 2025", dateRange(Frame.LastYear, custom, today).desc)
    }

    @Test
    fun `last month wraps across January`() {
        assertEquals("December 2025", dateRange(Frame.LastMonth, custom, LocalDate.of(2026, 1, 15)).desc)
    }

    @Test
    fun `custom range covers whole months and is clamped to today`() {
        val r = dateRange(Frame.Custom, custom, today)
        assertEquals("Mar – Jun 2026", r.desc)
        assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0), r.from)
        assertEquals(LocalDate.of(2026, 6, 30).atTime(LocalTime.MAX), r.to)

        val current = dateRange(Frame.Custom, MonthRange(YearMonth.of(2026, 9), YearMonth.of(2026, 10)), today)
        assertEquals(today.atTime(LocalTime.MAX), current.to)
    }

    @Test
    fun monthSpan() {
        assertEquals("June 2026", monthSpan(MonthRange(YearMonth.of(2026, 6), YearMonth.of(2026, 6))))
        assertEquals("Mar – Jun 2026", monthSpan(custom))
        assertEquals("Nov 2025 – Feb 2026", monthSpan(MonthRange(YearMonth.of(2025, 11), YearMonth.of(2026, 2))))
    }

    @Test
    fun fmtSpan() {
        val a = LocalDateTime.of(2026, 6, 8, 9, 0)
        assertEquals("Jun 8, 2026", fmtSpan(a, a.plusHours(5)))
        assertEquals("Jun 8 – 11, 2026", fmtSpan(a, a.plusDays(3)))
        assertEquals("Jun 8 – Jul 2, 2026", fmtSpan(a, LocalDateTime.of(2026, 7, 2, 9, 0)))
        assertEquals("Dec 27, 2025 – Jan 2, 2026", fmtSpan(LocalDateTime.of(2025, 12, 27, 9, 0), LocalDateTime.of(2026, 1, 2, 9, 0)))
    }

    @Test
    fun fmtTime() {
        assertEquals("12:05 AM", fmtTime(LocalDateTime.of(2026, 1, 1, 0, 5)))
        assertEquals("9:30 AM", fmtTime(LocalDateTime.of(2026, 1, 1, 9, 30)))
        assertEquals("12:00 PM", fmtTime(LocalDateTime.of(2026, 1, 1, 12, 0)))
        assertEquals("11:59 PM", fmtTime(LocalDateTime.of(2026, 1, 1, 23, 59)))
    }
}
