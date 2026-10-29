package com.nbradbury.memorymap.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

val MON = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val MONTH = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private fun mon(d: LocalDate) = MON[d.monthValue - 1]
private fun mon(ym: YearMonth) = MON[ym.monthValue - 1]
private fun month(ym: YearMonth) = MONTH[ym.monthValue - 1]
private fun LocalDate.endOfDay(): LocalDateTime = atTime(LocalTime.MAX)

fun fmtDate(d: LocalDateTime) = "${mon(d.toLocalDate())} ${d.dayOfMonth}, ${d.year}"

fun fmtTime(d: LocalDateTime): String {
    val h = d.hour
    return "${(h % 12).takeIf { it != 0 } ?: 12}:${d.minute.toString().padStart(2, '0')} ${if (h < 12) "AM" else "PM"}"
}

fun fmtSpan(a: LocalDateTime, b: LocalDateTime): String {
    val da = a.toLocalDate()
    val db = b.toLocalDate()
    return when {
        da == db -> fmtDate(a)
        da.year == db.year && da.month == db.month -> "${mon(da)} ${da.dayOfMonth} – ${db.dayOfMonth}, ${da.year}"
        da.year == db.year -> "${mon(da)} ${da.dayOfMonth} – ${mon(db)} ${db.dayOfMonth}, ${da.year}"
        else -> "${fmtDate(a)} – ${fmtDate(b)}"
    }
}

fun monthSpan(c: MonthRange): String {
    val a = c.from
    val b = c.to
    return when {
        a == b -> "${month(a)} ${a.year}"
        a.year == b.year -> "${mon(a)} – ${mon(b)} ${a.year}"
        else -> "${mon(a)} ${a.year} – ${mon(b)} ${b.year}"
    }
}

fun dateRange(frame: Frame, custom: MonthRange, today: LocalDate): DateRange {
    val thisMonth = YearMonth.from(today)
    return when (frame) {
        Frame.ThisMonth -> DateRange(thisMonth.atDay(1).atStartOfDay(), today.endOfDay(), "${month(thisMonth)} ${today.year}")
        Frame.LastMonth -> {
            val last = thisMonth.minusMonths(1)
            DateRange(last.atDay(1).atStartOfDay(), last.atEndOfMonth().endOfDay(), "${month(last)} ${last.year}")
        }
        Frame.ThisYear -> DateRange(
            LocalDate.of(today.year, 1, 1).atStartOfDay(),
            today.endOfDay(),
            "Jan 1 – ${mon(today)} ${today.dayOfMonth}, ${today.year}",
        )
        Frame.LastYear -> {
            val y = today.year - 1
            DateRange(LocalDate.of(y, 1, 1).atStartOfDay(), LocalDate.of(y, 12, 31).endOfDay(), "Jan 1 – Dec 31, $y")
        }
        Frame.Custom -> {
            val end = minOf(custom.to.atEndOfMonth(), today)
            DateRange(custom.from.atDay(1).atStartOfDay(), end.endOfDay(), monthSpan(custom))
        }
    }
}
