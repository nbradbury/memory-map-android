package com.nbradbury.memorymap.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MediaStorePhotoSource(private val resolver: ContentResolver) {
    /** [taken] is null when MediaStore couldn't date the image (EXIF without a time-zone offset). */
    data class Row(val id: Long, val taken: Long?, val modified: Long)

    /** [taken] is EXIF DateTimeOriginal in epoch ms, read as local time when the file has no offset. */
    data class Exif(val lat: Double?, val lng: Double?, val taken: Long?)

    fun uri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

    fun query(): List<Row> {
        val cols = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
        )
        val rows = ArrayList<Row>()
        resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                rows += Row(c.getLong(0), c.getLong(1).takeIf { it > 0 }, c.getLong(2))
            }
        }
        return rows
    }

    /** Reads the unredacted GPS tag. Requires ACCESS_MEDIA_LOCATION, otherwise setRequireOriginal throws. */
    fun readExif(id: Long): Exif? = try {
        resolver.openInputStream(MediaStore.setRequireOriginal(uri(id)))?.use { stream ->
            val exif = ExifInterface(stream)
            val ll = exif.latLong?.takeUnless { it[0] == 0.0 && it[1] == 0.0 }
            Exif(ll?.get(0), ll?.get(1), exifTime(exif))
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Parses DateTimeOriginal ourselves: ExifInterface.getDateTimeOriginal() treats offset-less times as UTC
     * (they're wall-clock local time) and shares a static SimpleDateFormat that corrupts parallel reads.
     */
    private fun exifTime(exif: ExifInterface): Long? {
        val local = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?.let { runCatching { LocalDateTime.parse(it.trim(), EXIF_TIME) }.getOrNull() } ?: return null
        val zone = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
            ?.let { runCatching { ZoneOffset.of(it.trim()) }.getOrNull() } ?: ZoneId.systemDefault()
        return local.atZone(zone).toInstant().toEpochMilli()
    }

    private companion object {
        val EXIF_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    }
}
