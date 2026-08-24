package com.sonora.music.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance 8-Petal Symmetrical Organic Shape for Sonora Artwork and Avatars
 * Renders with zero overhead directly on GPU Canvas.
 */
class Organic8PetalShape(
    private val petalCount: Int = 8,
    private val amplitude: Float = 0.08f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = minOf(cx, cy) * (1f - amplitude)

        val steps = 180
        for (i in 0..steps) {
            val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
            val r = baseR * (1f + amplitude * cos(petalCount * angle))
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
