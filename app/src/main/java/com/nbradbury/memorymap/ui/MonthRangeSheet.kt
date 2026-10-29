package com.nbradbury.memorymap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbradbury.memorymap.ui.theme.Scrap
import com.nbradbury.memorymap.ui.theme.figtree
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthRangeSheet(
    picker: PickerUi,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onTapMonth: (YearMonth) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Scrap.Card,
        scrimColor = Scrap.Ink.copy(alpha = 0.4f),
        dragHandle = { Box(Modifier.padding(top = 12.dp).size(36.dp, 4.dp).background(Scrap.Border, CircleShape)) },
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 36.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
                BasicText("Custom range", style = figtree(21, 800, letterSpacing = (-0.21).sp), modifier = Modifier.weight(1f))
                YearButton("‹", "Previous year", picker.canPrev, onPrevYear)
                BasicText(
                    picker.year.toString(),
                    style = figtree(16, 700).copy(textAlign = TextAlign.Center),
                    modifier = Modifier.widthIn(min = 44.dp),
                )
                YearButton("›", "Next year", picker.canNext, onNextYear)
            }
            BasicText(
                "Tap a start month, then an end month. Dots mark months with photos.",
                style = figtree(14, 400, Scrap.Muted),
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )
            picker.months.chunked(4).forEachIndexed { i, row ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { MonthCell(it, onTapMonth, Modifier.weight(1f)) }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 22.dp).fillMaxWidth(),
            ) {
                BasicText(picker.draftLabel, style = figtree(16, 700), modifier = Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(44.dp).clip(CircleShape).clickable(onClick = onCancel).padding(horizontal = 16.dp),
                ) {
                    BasicText("Cancel", style = figtree(15, 600, Scrap.Muted))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(44.dp)
                        .background(Scrap.Ink, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onApply)
                        .padding(horizontal = 22.dp),
                ) {
                    BasicText("Apply", style = figtree(15, 600, Scrap.OnInk))
                }
            }
        }
    }
}

@Composable
private fun YearButton(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.3f)
            .semantics { contentDescription = label },
    ) {
        BasicText(glyph, style = figtree(22, 400))
    }
}

@Composable
private fun MonthCell(m: MonthCellUi, onTap: (YearMonth) -> Unit, modifier: Modifier) {
    val bg = when {
        m.endpoint -> Scrap.Accent
        m.inRange -> Scrap.RangeFill
        else -> Scrap.Header
    }
    val fg = when {
        m.endpoint -> Scrap.Polaroid
        !m.enabled -> Scrap.Disabled
        else -> Scrap.Ink
    }
    val dot = when {
        !m.hasPhotos -> Color.Transparent
        m.endpoint -> Scrap.Polaroid
        else -> Scrap.Accent
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = m.enabled) { onTap(m.ym) },
    ) {
        BasicText(m.label, style = figtree(15, 600, fg))
        Box(Modifier.size(5.dp).background(dot, CircleShape))
    }
}
