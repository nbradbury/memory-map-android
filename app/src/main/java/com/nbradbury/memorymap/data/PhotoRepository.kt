package com.nbradbury.memorymap.data

import android.content.Context
import com.nbradbury.memorymap.domain.Photo
import com.nbradbury.memorymap.domain.Place
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

sealed interface IndexState {
    data object NeedsPermission : IndexState
    data class Scanning(val done: Int, val total: Int) : IndexState
    data class Ready(
        val places: List<Place>,
        val monthsWithPhotos: Set<YearMonth>,
        val minYear: Int?,
        val partialAccess: Boolean,
    ) : IndexState
}

interface PhotoIndex {
    val state: StateFlow<IndexState>
    fun refresh()
}

class PhotoRepository(
    private val context: Context,
    private val source: MediaStorePhotoSource,
    private val grouper: PlaceGrouper,
    private val cache: IndexCache,
    private val scope: CoroutineScope,
) : PhotoIndex {
    private val _state = MutableStateFlow<IndexState>(IndexState.Scanning(0, 0))
    override val state: StateFlow<IndexState> = _state.asStateFlow()
    private var job: Job? = null

    /** Rescans incrementally. While a previous result is showing it stays up until the new one is ready. */
    override fun refresh() {
        if (job?.isActive == true) return
        val access = MediaPermissions.access(context)
        if (access == MediaAccess.None) {
            _state.value = IndexState.NeedsPermission
            return
        }
        job = scope.launch { _state.value = scan(access == MediaAccess.Partial) }
    }

    private suspend fun scan(partial: Boolean): IndexState.Ready = withContext(Dispatchers.IO) {
        val old = cache.load()
        val rows = source.query()
        val todo = rows.filter { old.photos[it.id]?.modified != it.modified }
        val keepShowing = _state.value is IndexState.Ready
        val done = AtomicInteger()
        if (!keepShowing && todo.isNotEmpty()) _state.value = IndexState.Scanning(0, todo.size)

        val scanned = todo.chunked((todo.size / 4).coerceAtLeast(1)).map { chunk ->
            async {
                chunk.map { r ->
                    val exif = source.readExif(r.id)
                    val n = done.incrementAndGet()
                    if (!keepShowing && n % 25 == 0) _state.value = IndexState.Scanning(n, todo.size)
                    val taken = r.taken ?: exif?.taken ?: (r.modified * 1000)
                    r.id to CachedPhoto(r.modified, taken, exif?.lat, exif?.lng)
                }
            }
        }.awaitAll().flatten().toMap()

        // Rebuilding from rows drops images that were deleted (or are no longer visible to us).
        val photos = rows.associate { r -> r.id to (scanned[r.id] ?: old.photos.getValue(r.id)) }
        val zone = ZoneId.systemDefault()
        val geo = photos.mapNotNull { (id, p) ->
            if (p.lat == null || p.lng == null) return@mapNotNull null
            val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(p.taken), zone)
            GeoPhoto(Photo(id, source.uri(id).toString(), date), p.lat, p.lng)
        }
        val cells = old.cells.toMutableMap()
        val places = grouper.group(geo, cells)
        cache.save(IndexFile(photos, cells))

        IndexState.Ready(
            places = places,
            monthsWithPhotos = geo.mapTo(HashSet()) { YearMonth.from(it.photo.date) },
            minYear = geo.minOfOrNull { it.photo.date.year },
            partialAccess = partial,
        )
    }
}
