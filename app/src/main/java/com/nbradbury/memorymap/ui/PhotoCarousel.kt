package com.nbradbury.memorymap.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nbradbury.memorymap.ui.theme.Scrap
import com.nbradbury.memorymap.ui.theme.figtree
import com.nbradbury.memorymap.ui.theme.sepiaFilter
import kotlinx.coroutines.launch

private const val GOOGLE_PHOTOS = "com.google.android.apps.photos"
private val SlideWidth = 316.dp
private val SlideFilter = sepiaFilter(0.12f, 0.95f)

@Composable
fun PhotoCarousel(c: CarouselUi, onBack: () -> Unit) {
    val context = LocalContext.current
    val pager = rememberPagerState { c.slides.size }
    val strip = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val hasGooglePhotos = remember { context.packageManager.getLaunchIntentForPackage(GOOGLE_PHOTOS) != null }

    LaunchedEffect(pager.currentPage) { strip.animateScrollToItem((pager.currentPage - 3).coerceAtLeast(0)) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Scrap.Header)
            // Swallow taps so they don't reach the map underneath.
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = "Back to map" },
            ) {
                BasicText("←", style = figtree(24, 400))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(c.name, style = figtree(25, 800, letterSpacing = (-0.5).sp), maxLines = 1)
                BasicText(c.subtitle, style = figtree(14, 400, Scrap.Muted), maxLines = 1)
            }
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            HorizontalPager(
                state = pager,
                pageSize = PageSize.Fixed(SlideWidth),
                contentPadding = PaddingValues(horizontal = ((maxWidth - SlideWidth) / 2).coerceAtLeast(0.dp)),
                beyondViewportPageCount = 1,
                key = { c.slides[it].uri },
            ) { i -> Polaroid(c.slides[i], rotation = if (i % 2 == 1) 1.2f else -1.4f) }
        }

        BasicText(
            "${pager.currentPage + 1} of ${c.slides.size}",
            style = figtree(13, 600, Scrap.Muted).copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(
            state = strip,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            itemsIndexed(c.slides, key = { _, s -> s.uri }) { j, s ->
                val on = j == pager.currentPage
                AsyncImage(
                    model = s.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .alpha(if (on) 1f else 0.7f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Scrap.PhotoBg)
                        .border(2.dp, if (on) Scrap.Accent else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { scope.launch { pager.animateScrollToPage(j) } },
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 36.dp), contentAlignment = Alignment.Center) {
            BasicText(
                if (hasGooglePhotos) "Open in Google Photos" else "Open photo",
                style = figtree(15, 700, Scrap.Accent),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { openPhoto(context, c.slides[pager.currentPage].uri, hasGooglePhotos) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun Polaroid(s: SlideUi, rotation: Float) {
    Box(Modifier.padding(horizontal = 8.dp)) {
        Column(
            Modifier
                .rotate(rotation)
                .shadow(16.dp, RoundedCornerShape(3.dp), spotColor = Scrap.Shadow, ambientColor = Scrap.Shadow)
                .background(Scrap.Polaroid, RoundedCornerShape(3.dp))
                .padding(start = 10.dp, top = 10.dp, end = 10.dp),
        ) {
            AsyncImage(
                model = s.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = SlideFilter,
                modifier = Modifier.fillMaxWidth().height(364.dp).background(Scrap.PhotoBg),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                BasicText(s.date, style = figtree(14, 700), modifier = Modifier.weight(1f))
                BasicText(s.time, style = figtree(14, 400, Scrap.Faint))
            }
        }
    }
}

private fun openPhoto(context: Context, uri: String, preferGooglePhotos: Boolean) {
    val view = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "image/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(if (preferGooglePhotos) Intent(view).setPackage(GOOGLE_PHOTOS) else view)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent.createChooser(view, null))
    }
}
