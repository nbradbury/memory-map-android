package com.nbradbury.memorymap.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

class AndroidPlaceNamer(context: Context) : PlaceNamer {
    private val geocoder = Geocoder(context)

    override suspend fun name(lat: Double, lng: Double): PlaceName? {
        if (!Geocoder.isPresent()) return null
        val addresses = try {
            lookup(lat, lng)
        } catch (_: IOException) {
            null
        } ?: return null
        val a = addresses.firstOrNull() ?: return PlaceName("", "")
        val name = a.locality ?: a.subAdminArea ?: a.adminArea ?: a.countryName ?: return PlaceName("", "")
        return PlaceName(name, a.countryName.orEmpty(), a.adminArea.orEmpty())
    }

    /** Null means the lookup itself failed; an empty list means there's nothing there to name. */
    private suspend fun lookup(lat: Double, lng: Double): List<Address>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { c ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = c.resume(addresses)
                    override fun onError(errorMessage: String?) = c.resume(null)
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)
            }
        }
}
