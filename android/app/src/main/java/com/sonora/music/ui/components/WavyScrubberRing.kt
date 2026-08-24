package com.sonora.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WavyScrubberRing(
    progressPercent: Float, // 0f to 1f
    isDarkTheme: Boolean,
    onSeekPercent: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackBgColor = if (isDarkTheme) Color(0xFF2A2824) else Color(0xFFDED8CD)
    val progressColor = if (isDarkTheme) Color.White else Color(0xFF121212)
    val thumbColor = if (isDarkTheme) Color.White else Color(0xFF121212)

    val petalCount = 8
    val amplitudeFactor = 0.045f

    Box(
        modifier = modifier
            .size(320.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var angle = atan2(offset.y - cy, offset.x - cx)
                    angle += (PI / 2f).toFloat() // Align top as 0
                    if (angle < 0) angle += (2f * PI).toFloat()
                    val pct = (angle / (2f * PI.toFloat())).coerceIn(0f, 1f)
                    onSeekPercent(pct)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val offset = change.position
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var angle = atan2(offset.y - cy, offset.x - cx)
                    angle += (PI / 2f).toFloat()
                    if (angle < 0) angle += (2f * PI).toFloat()
                    val pct = (angle / (2f * PI.toFloat())).coerceIn(0f, 1f)
                    onSeekPercent(pct)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = (minOf(cx, cy) - 16.dp.toPx()) * (1f - amplitudeFactor)

            // Background full ring path
            val bgPath = Path()
            val steps = 240
            for (i in 0..steps) {
                val angle = (i.toFloat() / steps) * 2f * PI.toFloat() - (PI / 2f).toFloat()
                val r = baseR * (1f + amplitudeFactor * cos(petalCount * (angle + PI.toFloat() / 2f)))
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) bgPath.moveTo(x, y) else bgPath.lineTo(x, y)
            }
            bgPath.close()

            drawPath(
                path = bgPath,
                color = trackBgColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active Progress Path
            if (progressPercent > 0.005f) {
                val activePath = Path()
                val activeSteps = (steps * progressPercent.coerceIn(0f, 1f)).toInt()
                var thumbPos = Offset(cx, cy)

                for (i in 0..activeSteps) {
                    val angle = (i.toFloat() / steps) * 2f * PI.toFloat() - (PI / 2f).toFloat()
                    val r = baseR * (1f + amplitudeFactor * cos(petalCount * (angle + PI.toFloat() / 2f)))
                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)
                    if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
                    if (i == activeSteps) thumbPos = Offset(x, y)
                }

                drawPath(
                    path = activePath,
                    color = progressColor,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Indicator Thumb Dot
                drawCircle(
                    color = thumbColor,
                    radius = 7.dp.toPx(),
                    center = thumbPos
                )
            }
        }
    }
}
