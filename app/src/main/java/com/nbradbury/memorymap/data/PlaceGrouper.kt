package com.nbradbury.memorymap.data

import com.nbradbury.memorymap.domain.Photo
import com.nbradbury.memorymap.domain.Place
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

data class GeoPhoto(val photo: Photo, val lat: Double, val lng: Double)

/** A reverse-geocoded name. An empty [name] means "nowhere nameable" (open ocean, etc.) and is still cacheable. */
@Serializable
data class PlaceName(val name: String, val country: String, val region: String = "")

fun interface PlaceNamer {
    /** Returns null when naming failed and should be retried later (no geocoder, no network). */
    suspend fun name(lat: Double, lng: Double): PlaceName?
}

/**
 * Buckets photos into [CELL_DEGREES] grid cells, names each cell once, then merges cells that share a name, so
 * a city that straddles several cells becomes one place. [cellNames] is read and updated in place as a cache.
 */
class PlaceGrouper(private val namer: PlaceNamer) {
    suspend fun group(photos: List<GeoPhoto>, cellNames: MutableMap<String, PlaceName>): List<Place> {
        val cells = photos.groupBy { cellKey(it.lat, it.lng) }
        val sem = Semaphore(4)
        val names = coroutineScope {
            cells.map { (key, ps) ->
                val cached = cellNames[key]
                async {
                    key to (cached ?: sem.withPermit {
                        namer.name(ps.sumOf { it.lat } / ps.size, ps.sumOf { it.lng } / ps.size)
                    })
                }
            }.awaitAll()
        }
        names.forEach { (key, name) -> if (name != null) cellNames[key] = name }
        return names
            .groupBy({ (key, name) -> name?.takeIf { it.name.isNotEmpty() } ?: key }, { (key, _) -> cells.getValue(key) })
            .map { (id, groups) ->
                val ps = groups.flatten()
                val lat = ps.sumOf { it.lat } / ps.size
                val lng = ps.sumOf { it.lng } / ps.size
                val named = id as? PlaceName
                Place(
                    id = if (named != null) "${named.name}|${named.region}|${named.country}" else "cell:$id",
                    name = named?.name ?: coordLabel(lat, lng),
                    country = named?.country.orEmpty(),
                    lat = lat,
                    lng = lng,
                    photos = ps.map { it.photo }.sortedBy { it.date },
                )
            }
    }

    companion object {
        const val CELL_DEGREES = 0.25

        fun cellKey(lat: Double, lng: Double) =
            "${floor(lat / CELL_DEGREES).toInt()}:${floor(lng / CELL_DEGREES).toInt()}"

        fun coordLabel(lat: Double, lng: Double) = String.format(
            Locale.US, "%.2f°%s, %.2f°%s", abs(lat), if (lat >= 0) "N" else "S", abs(lng), if (lng >= 0) "E" else "W",
        )
    }
}
