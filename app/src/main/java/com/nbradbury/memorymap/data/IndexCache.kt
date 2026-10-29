package com.nbradbury.memorymap.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** What we learned from one image's EXIF. [lat]/[lng] are null when it has no GPS tag. */
@Serializable
data class CachedPhoto(val modified: Long, val taken: Long, val lat: Double? = null, val lng: Double? = null)

@Serializable
data class IndexFile(
    val photos: Map<Long, CachedPhoto> = emptyMap(),
    val cells: Map<String, PlaceName> = emptyMap(),
)

/** Persists the scan results so later launches only read EXIF from new or changed images. */
class IndexCache(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): IndexFile = try {
        if (file.exists()) json.decodeFromString(IndexFile.serializer(), file.readText()) else IndexFile()
    } catch (_: Exception) {
        IndexFile()
    }

    fun save(index: IndexFile) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(IndexFile.serializer(), index))
        tmp.renameTo(file)
    }
}
