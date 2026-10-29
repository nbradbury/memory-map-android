package com.nbradbury.memorymap

import android.app.Application
import com.nbradbury.memorymap.data.AndroidPlaceNamer
import com.nbradbury.memorymap.data.IndexCache
import com.nbradbury.memorymap.data.MediaStorePhotoSource
import com.nbradbury.memorymap.data.PhotoRepository
import com.nbradbury.memorymap.data.PlaceGrouper
import kotlinx.coroutines.MainScope
import org.maplibre.android.MapLibre
import java.io.File

class MemoryMapApp : Application() {
    val repository by lazy {
        PhotoRepository(
            context = this,
            source = MediaStorePhotoSource(contentResolver),
            grouper = PlaceGrouper(AndroidPlaceNamer(this)),
            cache = IndexCache(File(filesDir, "photo-index.json")),
            scope = MainScope(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
