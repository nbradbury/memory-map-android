package com.nbradbury.memorymap.data

import com.nbradbury.memorymap.domain.photo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceGrouperTest {
    @Test
    fun `cells with the same name merge into one place`() = runBlocking {
        val namer = PlaceNamer { lat, _ -> if (lat > 30) PlaceName("San Francisco", "USA", "California") else null }
        val photos = listOf(
            GeoPhoto(photo(2026, 1, 2), 37.70, -122.40),
            GeoPhoto(photo(2026, 1, 1), 37.74, -122.45),
            GeoPhoto(photo(2026, 1, 3), 37.80, -122.10), // A different 0.25° cell, same city.
        )
        val cache = mutableMapOf<String, PlaceName>()
        val place = PlaceGrouper(namer).group(photos, cache).single()

        assertEquals("San Francisco", place.name)
        assertEquals("USA", place.country)
        assertEquals(3, place.photos.size)
        assertEquals(photos.map { it.photo }.sortedBy { it.date }, place.photos)
        assertEquals(2, cache.size)
    }

    @Test
    fun `unnamed cells fall back to coordinates and failures are not cached`() = runBlocking {
        val cache = mutableMapOf<String, PlaceName>()
        val places = PlaceGrouper { _, _ -> null }.group(listOf(GeoPhoto(photo(2026, 1, 1), -33.86, 151.21)), cache)
        assertEquals("33.86°S, 151.21°E", places.single().name)
        assertEquals("", places.single().country)
        assertEquals(0, cache.size)
    }

    @Test
    fun `cached names skip the namer`() = runBlocking {
        val key = PlaceGrouper.cellKey(48.85, 2.35)
        val cache = mutableMapOf(key to PlaceName("Paris", "France"))
        val places = PlaceGrouper { _, _ -> error("should not geocode") }
            .group(listOf(GeoPhoto(photo(2026, 1, 1), 48.85, 2.35)), cache)
        assertEquals("Paris", places.single().name)
    }
}
