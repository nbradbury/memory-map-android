package com.nbradbury.memorymap.domain

import java.time.LocalDateTime

private var nextId = 0L

fun photo(y: Int, m: Int, d: Int, h: Int = 12) =
    Photo(nextId++, "content://media/$nextId", LocalDateTime.of(y, m, d, h, 0))

fun place(id: String, lat: Double, lng: Double, vararg photos: Photo) =
    Place(id, id, "", lat, lng, photos.sortedBy { it.date })
