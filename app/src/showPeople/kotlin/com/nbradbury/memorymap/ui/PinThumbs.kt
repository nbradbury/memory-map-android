package com.nbradbury.memorymap.ui

import androidx.compose.runtime.Composable
import com.nbradbury.memorymap.domain.Photo
import com.nbradbury.memorymap.domain.PinGroup

/** The photos a pin stacks, last on top. Builds with `-PhidePeople` swap in a version that keeps people off the top. */
@Composable
fun rememberPinThumbs(g: PinGroup): List<Photo>? = g.thumbs
