package com.nbradbury.memorymap.ui

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nbradbury.memorymap.domain.Place
import com.nbradbury.memorymap.domain.ScreenPoint
import com.nbradbury.memorymap.domain.cluster
import com.nbradbury.memorymap.ui.theme.Scrap
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import kotlin.math.min

/**
 * Leaflet zooms are one higher than MapLibre's for the same scale (256px vs 512px world at z0), so the design's
 * minZoom 1.5 / maxZoom 12 / fit cap 9 / cluster cap 10 become these.
 */
private const val MIN_ZOOM = 0.5
private const val MAX_ZOOM = 11.0
private const val FIT_MAX_ZOOM = 8.0
private const val CLUSTER_MAX_ZOOM = 9.0

private val STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "natgeo": {
      "type": "raster",
      "tiles": ["https://server.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/{z}/{y}/{x}"],
      "tileSize": 256,
      "maxzoom": 16,
      "attribution": "Tiles © Esri — National Geographic, Esri, DeLorme, NAVTEQ, UNEP-WCMC, USGS, NASA, ESA, METI, NRCAN, GEBCO, NOAA, iPC"
    }
  },
  "layers": [
    { "id": "paper", "type": "background", "paint": { "background-color": "#EFE6CC" } },
    {
      "id": "natgeo", "type": "raster", "source": "natgeo",
      "paint": { "raster-saturation": -0.22, "raster-contrast": 0.04, "raster-fade-duration": 0 }
    }
  ]
}
""".trimIndent()

/** Raster paint can't do a true sepia, so a translucent warm wash over the tiles finishes the look. */
private val SepiaWash = Color(0x2EB98B4E)

/**
 * The map plus its polaroid pins. [topInsetPx]/[bottomInsetPx] are the heights covered by the header and the
 * bottom card, so fits keep places (and the pins standing above them) in the visible gap.
 */
@Composable
fun ScrapbookMap(
    places: List<Place>,
    fit: FitRequest,
    topInsetPx: Int,
    bottomInsetPx: Int,
    onPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val mapView = rememberMapView()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val camera = remember { mutableIntStateOf(0) }
    var idle by remember { mutableIntStateOf(0) }

    DisposableEffect(mapView) {
        mapView.getMapAsync { m ->
            m.setStyle(Style.Builder().fromJson(STYLE_JSON))
            m.addOnCameraMoveListener { camera.intValue++ }
            m.addOnCameraIdleListener { idle++ }
            map = m
        }
        onDispose { }
    }

    fun px(dp: Int) = with(density) { dp.dp.roundToPx() }

    LaunchedEffect(map, bottomInsetPx) {
        map?.uiSettings?.setAttributionMargins(px(10), 0, 0, bottomInsetPx + px(6))
    }

    val currentPlaces by rememberUpdatedState(places)
    LaunchedEffect(map, fit) {
        val m = map ?: return@LaunchedEffect
        val ps = currentPlaces
        if (ps.isEmpty()) return@LaunchedEffect
        val pad = intArrayOf(px(74), topInsetPx + px(115), px(74), bottomInsetPx + px(44))
        m.fitTo(ps.map { it.lat }, ps.map { it.lngU }, pad, FIT_MAX_ZOOM, if (fit.animate) 900 else null)
    }

    val groups = remember(map, places, idle) {
        val m = map ?: return@remember emptyList()
        val w = with(density) { 96.dp.toPx() }
        val h = with(density) { 104.dp.toPx() }
        cluster(places, { p -> m.projection.toScreenLocation(LatLng(p.lat, p.lngU)).let { ScreenPoint(it.x, it.y) } }, w, h)
            .sortedByDescending { it.lat }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(SepiaWash))
        map?.let { m ->
            PinOverlay(
                groups = groups,
                project = { lat, lng ->
                    camera.intValue // Re-place pins on every camera frame.
                    m.projection.toScreenLocation(LatLng(lat, lng))
                },
                onClick = { g ->
                    if (g.members.size > 1) {
                        val pad = intArrayOf(px(80), topInsetPx + px(120), px(80), bottomInsetPx + px(64))
                        m.fitTo(g.members.map { it.lat }, g.members.map { it.lngU }, pad, CLUSTER_MAX_ZOOM, 800)
                    } else {
                        onPlaceClick(g.members[0].id)
                    }
                },
            )
        }
    }
}

/**
 * Note that MapLibre on Android won't zoom out past the point where the world is as tall as the view (there's no
 * ConstrainMode.NONE), so on a tall phone a fit that spans two continents stops at roughly zoom 0.85.
 */
private fun MapLibreMap.fitTo(lats: List<Double>, lngs: List<Double>, pad: IntArray, maxZoom: Double, durationMs: Int?) {
    val bounds = LatLngBounds.from(lats.max(), lngs.max(), lats.min(), lngs.min())
    val cam = getCameraForLatLngBounds(bounds, pad) ?: return
    val update = CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(cam).zoom(min(cam.zoom, maxZoom)).build())
    if (durationMs != null) easeCamera(update, durationMs) else moveCamera(update)
}

@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        val options = MapLibreMapOptions.createFromAttributes(context)
            .camera(CameraPosition.Builder().target(LatLng(30.0, -40.0)).zoom(MIN_ZOOM).build())
            .minZoomPreference(MIN_ZOOM)
            .maxZoomPreference(MAX_ZOOM)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false)
            .compassEnabled(false)
            .logoEnabled(false)
            .attributionEnabled(true)
            .attributionGravity(Gravity.BOTTOM or Gravity.START)
            .attributionTintColor(Scrap.Muted.toArgb())
            .foregroundLoadColor(Scrap.MapBg.toArgb())
        MapView(context, options).apply { onCreate(null) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    return mapView
}
