package com.nbradbury.memorymap.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.nbradbury.memorymap.data.MediaPermissions
import com.nbradbury.memorymap.domain.Frame
import com.nbradbury.memorymap.ui.theme.Scrap
import com.nbradbury.memorymap.ui.theme.figtree

@Composable
fun MapScreen(state: UiState, vm: MainViewModel) {
    val density = LocalDensity.current
    var headerPx by rememberSaveable { mutableIntStateOf(0) }
    var cardPx by rememberSaveable { mutableIntStateOf(0) }
    val bottomPx = cardPx + with(density) { 30.dp.roundToPx() } + WindowInsets.navigationBars.getBottom(density)
    val requestAccess = rememberMediaAccessRequest(onResult = vm::refresh)

    BackHandler(enabled = state.carousel != null) { vm.closePlace() }

    Box(Modifier.fillMaxSize().background(Scrap.Paper)) {
        ScrapbookMap(
            places = state.places,
            fit = state.fit,
            topInsetPx = headerPx,
            bottomInsetPx = bottomPx,
            onPlaceClick = vm::openPlace,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0.6f to Color(0x005A3C1E),
                        1f to Color(0x1F5A3C1E),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.maxDimension * 0.6f,
                    ),
                )
            },
        )

        Header(
            state = state,
            onChip = vm::pick,
            onChangeAccess = requestAccess.launch,
            modifier = Modifier.onSizeChanged { headerPx = it.height },
        )

        when {
            state.needsPermission -> PermissionCard(
                openSettings = requestAccess.openSettings,
                onClick = requestAccess.launch,
                modifier = Modifier.align(Alignment.Center),
            )
            state.noPlaces -> NoticeCard(
                title = "No places in this range",
                body = "Only photos with a location show up here. Try a wider range.",
                modifier = Modifier.align(Alignment.Center),
            )
        }

        RangeCard(
            state = state,
            onRecenter = { vm.fit() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 30.dp)
                .onSizeChanged { cardPx = it.height },
        )

        state.picker?.let { p ->
            MonthRangeSheet(
                picker = p,
                onPrevYear = { vm.shiftPickerYear(-1) },
                onNextYear = { vm.shiftPickerYear(1) },
                onTapMonth = vm::tapMonth,
                onCancel = vm::closePicker,
                onApply = vm::applyPicker,
            )
        }

        state.carousel?.let { PhotoCarousel(it, onBack = vm::closePlace) }
    }
}

@Composable
private fun Header(
    state: UiState,
    onChip: (Frame) -> Unit,
    onChangeAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, spotColor = Scrap.Shadow, ambientColor = Color.Transparent)
            .background(Scrap.Header)
            .drawBehind {
                val y = size.height - 0.5.dp.toPx()
                drawLine(Scrap.HeaderBorder, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            .statusBarsPadding(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(start = 20.dp, end = 18.dp, top = 10.dp),
        ) {
            BasicText("MemoryMap", style = figtree(25, 800, letterSpacing = (-0.5).sp))
            if (state.partialAccess) {
                BasicText(
                    "Showing selected photos · Change",
                    style = figtree(13, 600, Scrap.Accent),
                    modifier = Modifier.clickable(onClick = onChangeAccess),
                )
            } else {
                BasicText("From your photo library", style = figtree(13, 500, Scrap.Muted))
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 14.dp),
        ) {
            items(state.chips, key = { it.frame }) { c ->
                // The applied custom span is the widest chip and sits last, so keep the selection on screen.
                val bring = remember { BringIntoViewRequester() }
                LaunchedEffect(c.selected, c.label) { if (c.selected) bring.bringIntoView() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .bringIntoViewRequester(bring)
                        .height(40.dp)
                        .background(if (c.selected) Scrap.Ink else Scrap.Card, CircleShape)
                        .border(1.dp, if (c.selected) Scrap.Ink else Scrap.Border, CircleShape)
                        .clip(CircleShape)
                        .clickable { onChip(c.frame) }
                        .padding(horizontal = 16.dp),
                ) {
                    BasicText(c.label, style = figtree(14, 600, if (c.selected) Scrap.OnInk else Scrap.Ink), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RangeCard(state: UiState, onRecenter: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(22.dp), spotColor = Scrap.Shadow, ambientColor = Color.Transparent)
            .background(Scrap.Card, RoundedCornerShape(22.dp))
            .padding(start = 18.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(state.rangeDesc, style = figtree(18, 700, letterSpacing = (-0.18).sp), maxLines = 1)
            BasicText(state.statLine, style = figtree(14, 400, Scrap.Muted), maxLines = 1)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(Scrap.Header, CircleShape)
                .border(1.dp, Scrap.Border, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onRecenter)
                .semantics { contentDescription = "Fit all places" },
        ) {
            Box(Modifier.size(18.dp).border(2.dp, Scrap.Ink, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(5.dp).background(Scrap.Ink, CircleShape))
            }
        }
    }
}

@Composable
private fun NoticeCard(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .padding(horizontal = 40.dp)
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(20.dp), spotColor = Scrap.Shadow, ambientColor = Color.Transparent)
            .background(Scrap.Card, RoundedCornerShape(20.dp))
            .padding(22.dp),
    ) {
        BasicText(title, style = figtree(17, 700).copy(textAlign = TextAlign.Center))
        BasicText(body, style = figtree(14, 400, Scrap.Muted).copy(textAlign = TextAlign.Center, lineHeight = 19.6.sp))
        action()
    }
}

@Composable
private fun PermissionCard(openSettings: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    NoticeCard(
        title = "See where your photos were taken",
        body = "MemoryMap reads the location saved in your photos. Nothing leaves your device.",
        modifier = modifier,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(44.dp)
                .background(Scrap.Ink, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 22.dp),
        ) {
            BasicText(if (openSettings) "Open settings" else "Allow photo access", style = figtree(15, 600, Scrap.OnInk))
        }
    }
}

class MediaAccessRequest(val launch: () -> Unit, val openSettings: Boolean)

/** Requests photo + media-location access, and falls back to app settings once the user has said "don't ask". */
@Composable
private fun rememberMediaAccessRequest(onResult: () -> Unit): MediaAccessRequest {
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val activity = context as Activity
        denied = result.values.none { it } &&
            MediaPermissions.request.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        onResult()
    }
    return MediaAccessRequest(
        launch = {
            if (denied) {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            } else {
                launcher.launch(MediaPermissions.request)
            }
        },
        openSettings = denied,
    )
}
