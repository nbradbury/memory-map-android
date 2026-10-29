package com.nbradbury.memorymap.domain

import java.time.LocalDateTime
import java.time.YearMonth

/** A geotagged photo. [uri] is a MediaStore content URI string, which Coil loads directly. */
data class Photo(val id: Long, val uri: String, val date: LocalDateTime)

/**
 * A named place and its photos, oldest first. [lngU] is the longitude "unwrapped" across the antimeridian
 * so that every place in a filtered set fits inside one contiguous bounds (see [filterPlaces]).
 */
data class Place(
    val id: String,
    val name: String,
    val country: String,
    val lat: Double,
    val lng: Double,
    val photos: List<Photo>,
    val lngU: Double = lng,
)

enum class Frame { ThisMonth, LastMonth, ThisYear, LastYear, Custom }

data class MonthRange(val from: YearMonth, val to: YearMonth)

/** An inclusive date-time range plus the human description shown in the bottom card. */
data class DateRange(val from: LocalDateTime, val to: LocalDateTime, val desc: String)

data class ScreenPoint(val x: Float, val y: Float)
