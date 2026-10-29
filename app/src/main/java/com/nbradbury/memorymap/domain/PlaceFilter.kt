package com.nbradbury.memorymap.domain

import java.time.LocalDateTime

/**
 * Keeps the places with photos in [from]..[to] (trimming each place's photos to the range) and sets [Place.lngU].
 * The largest gap between the remaining longitudes is treated as the "outside" of the set, and everything west
 * of it is shifted by +360°, so a trip spanning Tokyo and Honolulu fits across the Pacific, not around the globe.
 */
fun filterPlaces(places: List<Place>, from: LocalDateTime, to: LocalDateTime): List<Place> {
    val ps = places.mapNotNull { p ->
        p.photos.filter { it.date in from..to }.takeIf { it.isNotEmpty() }?.let { p.copy(photos = it) }
    }
    val ls = ps.map { it.lng }.distinct().sorted()
    var cut = Double.POSITIVE_INFINITY
    var gap = if (ls.isNotEmpty()) ls.first() + 360 - ls.last() else 0.0
    for (i in 1 until ls.size) {
        if (ls[i] - ls[i - 1] > gap) {
            gap = ls[i] - ls[i - 1]
            cut = ls[i]
        }
    }
    if (cut.isInfinite()) return ps.map { it.copy(lngU = it.lng) }
    return ps.map { it.copy(lngU = if (it.lng < cut) it.lng + 360 else it.lng) }
}
