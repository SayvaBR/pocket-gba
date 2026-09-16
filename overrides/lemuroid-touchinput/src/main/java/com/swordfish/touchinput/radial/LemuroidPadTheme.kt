package com.swordfish.touchinput.radial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class LemuroidPadTheme(
    private val style: String = "minimal",
    private val accent: Color = Color(0xFF4DA3FF),
) {
    private fun gray(luminosity: Float, opacity: Float): Color =
        Color(luminosity, luminosity, luminosity, opacity)

    val foregroundPadding: Dp = 8.dp
    val padding: Dp = 4.dp

    private val icons =
        when (style) {
            "classic" -> Color.White.copy(alpha = 0.78f)
            "glow" -> accent.copy(alpha = 0.96f)
            else -> Color(0xFFD7E4F2).copy(alpha = 0.78f)
        }
    private val iconsPressed = Color.White

    private val level3Fill =
        when (style) {
            "classic" -> Color(0xFFB9C0CA).copy(alpha = 0.80f)
            "glow" -> accent.copy(alpha = 0.48f)
            else -> Color(0xFF15263A).copy(alpha = 0.92f)
        }
    private val level3FillPressed = accent.copy(alpha = 0.85f)
    val level3Shadow =
        if (style == "glow") accent.copy(alpha = 0.38f) else DefaultShadowColor.copy(alpha = 0.16f)
    val level3ShadowWidth = if (style == "glow") 8.dp else 3.dp

    private val level2Fill =
        when (style) {
            "classic" -> Color(0xFF414A56).copy(alpha = 0.88f)
            "glow" -> Color(0xFF101F32).copy(alpha = 0.96f)
            else -> Color(0xFF0F1D2C).copy(alpha = 0.88f)
        }
    private val level2FillPressed = accent.copy(alpha = 0.55f)
    val level2Shadow =
        if (style == "glow") accent.copy(alpha = 0.30f) else DefaultShadowColor.copy(alpha = 0.12f)
    val level2ShadowWidth = if (style == "glow") 7.dp else 3.dp

    val level1Fill =
        when (style) {
            "classic" -> Color(0xFF26303B).copy(alpha = 0.92f)
            "glow" -> accent.copy(alpha = 0.12f)
            else -> Color(0xFF0C1826).copy(alpha = 0.72f)
        }
    val level1Shadow =
        if (style == "glow") accent.copy(alpha = 0.24f) else DefaultShadowColor.copy(alpha = 0.10f)
    val level1ShadowWidth = if (style == "glow") 6.dp else 2.dp

    val level0CornerRadius = 24.dp
    val level0Fill = Color(0xFF07111E).copy(alpha = if (style == "classic") 0.30f else 0.12f)
    val level0Shadow = Color.Transparent
    val level0ShadowWidth = 0.dp

    fun compositeFill(pressed: Boolean): Color =
        if (pressed) level2FillPressed else level2Fill

    fun foregroundFill(pressed: Boolean): Color =
        if (pressed) level3FillPressed else level3Fill

    fun icons(pressed: Boolean): Color =
        if (pressed) iconsPressed else icons
}

val LocalLemuroidPadTheme =
    compositionLocalOf<LemuroidPadTheme> {
        error("LemuroidPadTheme is missing")
    }
