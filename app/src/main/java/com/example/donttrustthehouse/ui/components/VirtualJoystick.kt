package com.example.donttrustthehouse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.donttrustthehouse.model.Vector2D
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    onMove: (Vector2D) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                val radius = this.size.width / 2f
                val maxDrag = radius * 0.75f

                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(radius, radius)
                        val diff = offset - center
                        val dist = diff.getDistance()
                        val clampedOffset = if (dist > maxDrag) {
                            diff * (maxDrag / dist)
                        } else {
                            diff
                        }
                        thumbOffset = clampedOffset
                        onMove(Vector2D(clampedOffset.x / maxDrag, clampedOffset.y / maxDrag))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(radius, radius)
                        val diff = change.position - center
                        val dist = diff.getDistance()
                        val clampedOffset = if (dist > maxDrag) {
                            diff * (maxDrag / dist)
                        } else {
                            diff
                        }
                        thumbOffset = clampedOffset
                        onMove(Vector2D(clampedOffset.x / maxDrag, clampedOffset.y / maxDrag))
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(Vector2D.ZERO)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(Vector2D.ZERO)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f - 4f
            val thumbRadius = outerRadius * 0.35f

            // Outer ring
            drawCircle(
                color = Color(0x33FFFFFF),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color(0x2216131A),
                radius = outerRadius,
                center = center
            )

            // Inner crosshairs
            val arm = outerRadius * 0.3f
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(center.x - arm, center.y),
                end = Offset(center.x + arm, center.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(center.x, center.y - arm),
                end = Offset(center.x, center.y + arm),
                strokeWidth = 2.dp.toPx()
            )

            // Thumb
            val currentThumbPos = center + thumbOffset
            drawCircle(
                color = Color(0x88E53935),
                radius = thumbRadius,
                center = currentThumbPos
            )
            drawCircle(
                color = Color(0xFFE53935),
                radius = thumbRadius * 0.6f,
                center = currentThumbPos
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = currentThumbPos,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
