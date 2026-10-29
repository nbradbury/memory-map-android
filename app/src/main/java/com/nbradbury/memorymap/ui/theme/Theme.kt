package com.nbradbury.memorymap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.nbradbury.memorymap.R

object Scrap {
    val Ink = Color(0xFF2B2219)
    val Accent = Color(0xFFC2573A)
    val AccentSoft = Color(0xFFF0A88E)
    val Header = Color(0xFFF3EBDD)
    val HeaderBorder = Color(0xFFDCCDB4)
    val Card = Color(0xFFFBF7EF)
    val Border = Color(0xFFD8C9B0)
    val Muted = Color(0xFF6E5F4E)
    val Faint = Color(0xFF8A7964)
    val Disabled = Color(0xFFBFB29C)
    val Paper = Color(0xFFEADFCB)
    val MapBg = Color(0xFFEFE6CC)
    val Polaroid = Color(0xFFFFFDF8)
    val PhotoBg = Color(0xFFE2D6C2)
    val RangeFill = Color(0xFFF0DACD)
    val OnInk = Color(0xFFF6EFE3)
    val Shadow = Color(0xFF462D14)
}

@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: Int) = Font(
    R.font.figtree,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Figtree = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700), figtree(800))

fun figtree(size: Int, weight: Int, color: Color = Scrap.Ink, letterSpacing: TextUnit = TextUnit.Unspecified) =
    TextStyle(
        fontFamily = Figtree,
        fontSize = size.sp,
        fontWeight = FontWeight(weight),
        color = color,
        letterSpacing = letterSpacing,
    )

/** CSS `sepia(a) saturate(s)`, used to warm photo thumbnails to match the map. */
fun sepiaFilter(a: Float, saturate: Float = 1f): ColorFilter {
    val k = 1 - a
    val sepia = ColorMatrix(
        floatArrayOf(
            0.393f + 0.607f * k, 0.769f - 0.769f * k, 0.189f - 0.189f * k, 0f, 0f,
            0.349f - 0.349f * k, 0.686f + 0.314f * k, 0.168f - 0.168f * k, 0f, 0f,
            0.272f - 0.272f * k, 0.534f - 0.534f * k, 0.131f + 0.869f * k, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    sepia.timesAssign(ColorMatrix().apply { setToSaturation(saturate) })
    return ColorFilter.colorMatrix(sepia)
}

@Composable
fun MemoryMapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Scrap.Ink,
            onPrimary = Scrap.OnInk,
            secondary = Scrap.Accent,
            background = Scrap.Paper,
            surface = Scrap.Card,
            onSurface = Scrap.Ink,
        ),
        content = content,
    )
}
