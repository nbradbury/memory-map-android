package com.nbradbury.memorymap.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class PlaceFilterTest {
    private val all = LocalDateTime.MIN to LocalDateTime.MAX

    @Test
    fun `keeps only places and photos inside the range`() {
        val sf = place("sf", 37.7, -122.4, photo(2026, 3, 1), photo(2026, 7, 1))
        val lis = place("lis", 38.7, -9.1, photo(2025, 6, 1))
        val ps = filterPlaces(listOf(sf, lis), LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 5, 1, 0, 0))
        assertEquals(listOf("sf"), ps.map { it.id })
        assertEquals(1, ps[0].photos.size)
    }

    @Test
    fun `unwraps across the Pacific when that is the tighter fit`() {
        val ps = filterPlaces(
            listOf(
                place("tyo", 35.7, 139.7, photo(2025, 11, 3)),
                place("hnl", 21.3, -157.9, photo(2025, 12, 27)),
                place("sf", 37.7, -122.4, photo(2025, 12, 1)),
            ),
            all.first, all.second,
        ).associateBy { it.id }
        assertEquals(139.7, ps.getValue("tyo").lngU, 1e-9)
        assertEquals(-157.9 + 360, ps.getValue("hnl").lngU, 1e-9)
        assertEquals(-122.4 + 360, ps.getValue("sf").lngU, 1e-9)
    }

    @Test
    fun `leaves longitudes alone when the antimeridian is already the biggest gap`() {
        val ps = filterPlaces(
            listOf(place("sf", 37.7, -122.4, photo(2026, 6, 1)), place("lis", 38.7, -9.1, photo(2026, 6, 8))),
            all.first, all.second,
        )
        assertEquals(ps.map { it.lng }, ps.map { it.lngU })
    }
}
