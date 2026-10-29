package com.nbradbury.memorymap.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ClusteringTest {
    /** Treats lat/lng as screen x/y so distances are easy to reason about. */
    private val project = { p: Place -> ScreenPoint(p.lng.toFloat(), p.lat.toFloat()) }

    @Test
    fun `nearby places merge under the busiest one`() {
        val small = place("small", 0.0, 0.0, photo(2026, 1, 1))
        val big = place("big", 50.0, 50.0, photo(2026, 1, 1), photo(2026, 1, 2), photo(2026, 1, 3))
        val far = place("far", 0.0, 500.0, photo(2026, 1, 1))

        val gs = cluster(listOf(small, big, far), project, 96f, 104f)

        assertEquals(2, gs.size)
        assertEquals(listOf("big", "small"), gs[0].members.map { it.id })
        assertEquals("big +1", gs[0].label)
        assertEquals(4, gs[0].count)
        assertEquals(50.0, gs[0].lat, 0.0)
        assertEquals("far", gs[1].label)
    }

    @Test
    fun `single place thumbs are spread across its photos`() {
        val ps = List(9) { photo(2026, 1, it + 1) }
        val g = cluster(listOf(place("p", 0.0, 0.0, *ps.toTypedArray())), project, 96f, 104f).single()
        assertEquals(listOf(ps[1], ps[4], ps[7]), g.thumbs)
    }

    @Test
    fun `group thumbs are the middle photo of the first three members`() {
        val members = List(4) { i -> place("p$i", 0.0, i.toDouble(), *Array(4 - i) { photo(2026, 1, it + 1) }) }
        val g = cluster(members, project, 96f, 104f).single()
        assertEquals(members.take(3).map { it.photos[it.photos.size / 2] }, g.thumbs)
    }
}
