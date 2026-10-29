package com.nbradbury.memorymap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.nbradbury.memorymap.domain.Photo
import com.nbradbury.memorymap.domain.PinGroup

/** Null until a people-free top photo has been picked, so the pin doesn't flash its original stack first. */
@Composable
fun rememberPinThumbs(g: PinGroup): List<Photo>? {
    val context = LocalContext.current
    val thumbs by produceState<List<Photo>?>(null, g) { value = NoFaceFeature.arrange(context, g) }
    return thumbs
}
