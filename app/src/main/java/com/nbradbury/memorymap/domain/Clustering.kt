package com.nbradbury.memorymap.domain

import kotlin.math.abs

/** A map pin: one place, or several places whose pins would overlap on screen. */
data class PinGroup(
    val lat: Double,
    val lng: Double,
    val members: List<Place>,
    val count: Int,
    val thumbs: List<Photo>,
    val label: String,
) {
    val key: String get() = members.first().id
}

/**
 * Greedy screen-space clustering. Places are visited largest first, and each joins the first group whose leader
 * is within [w]×[h] pixels of it, so the busiest place anchors (and names) each group.
 */
fun cluster(places: List<Place>, project: (Place) -> ScreenPoint, w: Float, h: Float): List<PinGroup> {
    class Group(val pt: ScreenPoint, val lead: Place) {
        val members = mutableListOf(lead)
    }

    val groups = mutableListOf<Group>()
    places.sortedByDescending { it.photos.size }.forEach { p ->
        val pt = project(p)
        val g = groups.find { abs(it.pt.x - pt.x) < w && abs(it.pt.y - pt.y) < h }
        if (g != null) g.members += p else groups += Group(pt, p)
    }
    return groups.map { g ->
        val ms = g.members
        val thumbs = if (ms.size == 1) {
            val ph = ms[0].photos
            val n = minOf(3, ph.size)
            List(n) { i -> ph[((i + 0.5) * ph.size / n).toInt()] }
        } else {
            ms.take(3).map { it.photos[it.photos.size / 2] }
        }
        PinGroup(
            lat = g.lead.lat,
            lng = g.lead.lngU,
            members = ms,
            count = ms.sumOf { it.photos.size },
            thumbs = thumbs,
            label = if (ms.size > 1) "${ms[0].name} +${ms.size - 1}" else ms[0].name,
        )
    }
}
