package com.nbradbury.memorymap.ui

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nbradbury.memorymap.domain.PinGroup
import com.nbradbury.memorymap.ui.theme.Scrap
import com.nbradbury.memorymap.ui.theme.figtree
import com.nbradbury.memorymap.ui.theme.sepiaFilter
import kotlin.math.roundToInt

/** Per-card (rotation°, dx, dy) for stacks of 1, 2 and 3 polaroids. */
private val STACKS = listOf(
    listOf(Triple(-3f, 0, 0)),
    listOf(Triple(-7f, -10, 2), Triple(6f, 10, 0)),
    listOf(Triple(-10f, -17, 5), Triple(9f, 17, 3), Triple(-1f, 0, 0)),
)

private val ThumbFilter = sepiaFilter(0.18f, 0.92f)

@Composable
fun PinOverlay(groups: List<PinGroup>, project: (Double, Double) -> PointF, onClick: (PinGroup) -> Unit) {
    val density = LocalDensity.current
    val anchorX = with(density) { 60.dp.toPx() }
    val anchorY = with(density) { 111.dp.toPx() }
    Box(Modifier.fillMaxSize()) {
        groups.forEach { g ->
            key(g.key) {
                Pin(
                    g,
                    Modifier
                        .offset {
                            val p = project(g.lat, g.lng)
                            IntOffset((p.x - anchorX).roundToInt(), (p.y - anchorY).roundToInt())
                        }
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onClick(g) },
                )
            }
        }
    }
}

@Composable
private fun Pin(g: PinGroup, modifier: Modifier) {
    Box(modifier.size(120.dp, 118.dp)) {
        STACKS[g.thumbs.size - 1].zip(g.thumbs).forEach { (pos, photo) ->
            val (rot, dx, dy) = pos
            Box(
                Modifier
                    .offset((30 + dx).dp, (6 + dy).dp)
                    .size(60.dp, 70.dp)
                    .rotate(rot)
                    .shadow(4.dp, RoundedCornerShape(2.dp), spotColor = Scrap.Shadow, ambientColor = Scrap.Shadow)
                    .background(Scrap.Polaroid)
                    .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
            ) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ThumbFilter,
                    modifier = Modifier.fillMaxSize().background(Scrap.PhotoBg),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 84.dp)
                .wrapContentWidth(unbounded = true)
                .shadow(3.dp, CircleShape)
                .background(Scrap.Ink, CircleShape)
                .padding(horizontal = 9.dp, vertical = 5.dp),
        ) {
            BasicText(g.label, style = figtree(12, 600, Scrap.OnInk), maxLines = 1, softWrap = false)
            BasicText(g.count.toString(), style = figtree(12, 600, Scrap.AccentSoft), maxLines = 1)
        }
        Box(
            Modifier
                .offset(54.dp, 105.dp)
                .size(12.dp)
                .shadow(2.dp, CircleShape)
                .background(Scrap.Accent, CircleShape)
                .border(2.5.dp, Scrap.Polaroid, CircleShape),
        )
    }
}
