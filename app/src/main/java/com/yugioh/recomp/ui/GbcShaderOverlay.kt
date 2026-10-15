package com.yugioh.recomp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * High-performance Game Boy Color LCD dot-matrix & scanline shader overlay.
 * Recreates the physical TFT LCD sub-pixel matrix of the original GBC handheld:
 * - Subtle horizontal scanlines
 * - Vertical sub-pixel separation grid
 * - Classic GBC color tint and LCD contrast
 */
@Composable
fun GbcShaderOverlay(
    modifier: Modifier = Modifier,
    alpha: Float = 0.22f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawGbcLcdGrid(alpha)
    }
}

private fun DrawScope.drawGbcLcdGrid(alpha: Float) {
    val pixelPitch = 3.5f // Dot-matrix pixel pitch in px
    val w = size.width
    val h = size.height

    val gridColor = Color(0x33000000)
    val scanlineColor = Color(0x22050E05)

    // Horizontal scanlines
    var y = 0f
    while (y < h) {
        drawLine(
            color = scanlineColor.copy(alpha = alpha * 0.9f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
        y += pixelPitch
    }

    // Vertical LCD grid lines
    var x = 0f
    while (x < w) {
        drawLine(
            color = gridColor.copy(alpha = alpha * 0.6f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 0.8f
        )
        x += pixelPitch
    }

    // Very subtle LCD backlight glow/warmth
    drawRect(
        color = Color(0x0660A5FA),
        topLeft = Offset.Zero,
        size = Size(w, h)
    )
}
