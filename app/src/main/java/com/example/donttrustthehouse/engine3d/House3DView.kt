package com.example.donttrustthehouse.engine3d

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.donttrustthehouse.core.HouseManager
import com.example.donttrustthehouse.model.MonsterData
import com.example.donttrustthehouse.model.MonsterState
import com.example.donttrustthehouse.model.PlayerState
import com.example.donttrustthehouse.model.Vector2D
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun House3DView(
    modifier: Modifier = Modifier,
    raycastEngine: RaycastEngine,
    houseManager: HouseManager,
    playerState: PlayerState,
    monsterData: MonsterData?,
    dangerProximity: Float,
    flashlightOn: Boolean,
    headBob: Float,
    hasCrosshairTarget: Boolean,
    sanity: Float = 1.0f,
    gameTime: Float = 0f
) {
    // Synchronize open doors to raycast engine
    houseManager.doors.forEach { door ->
        raycastEngine.setDoorOpen(door.position.x.toInt(), door.position.y.toInt(), door.isOpen)
    }

    // Build list of 3D sprites to render
    val sprites = mutableListOf<Sprite3D>()

    // Add hide spots as 3D sprites
    houseManager.hideSpots.forEach { spot ->
        sprites.add(
            Sprite3D(
                x = spot.position.x,
                y = spot.position.y,
                texture = Textures.hideSpotSprite,
                scale = 0.85f,
                isMonster = false
            )
        )
    }

    // Add Monster 3D sprite if active
    if (monsterData != null && monsterData.state != MonsterState.IDLE) {
        sprites.add(
            Sprite3D(
                x = monsterData.position.x,
                y = monsterData.position.y,
                texture = Textures.monsterSprite,
                scale = 1.15f,
                isMonster = true
            )
        )
    }

    // Add uncollected items as 3D pickup sprites
    houseManager.items.forEach { item ->
        if (!item.isCollected) {
            sprites.add(
                Sprite3D(
                    x = item.position.x,
                    y = item.position.y,
                    texture = Textures.getItemSprite(item.type),
                    scale = 0.52f,
                    isMonster = false
                )
            )
        }
    }

    // Current room lighting status
    val currentRoom = houseManager.getRoomAt(playerState.position)
    val isRoomLit = currentRoom?.let { houseManager.isRoomLit(it.id) } ?: true

    // Render frame to bitmap with true raycast chromatic aberration
    val bitmap = raycastEngine.renderFrame(
        playerX = playerState.position.x,
        playerY = playerState.position.y,
        playerAngle = playerState.facingAngle,
        isCrouching = playerState.isCrouching,
        flashlightOn = flashlightOn,
        headBob = headBob,
        sprites = sprites,
        isLitRoom = isRoomLit,
        dangerProximity = dangerProximity,
        sanity = sanity,
        timeSec = gameTime
    )

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Render the 3D Raycasted Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val insanity = (1.0f - sanity).coerceIn(0f, 1f)

            // Dynamic camera jitter / psych shake when sanity is critical
            val jitterX = if (insanity > 0.6f && (gameTime * 20f).toInt() % 5 == 0) {
                (sin(gameTime * 30f) * (insanity * 6.dp.toPx()))
            } else 0f
            val jitterY = if (insanity > 0.7f && (gameTime * 20f).toInt() % 7 == 0) {
                (cos(gameTime * 28f) * (insanity * 4.dp.toPx()))
            } else 0f

            // Draw scaled 3D bitmap
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(jitterX.toInt(), jitterY.toInt()),
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )

            // 2. Screen-Space Chromatic Aberration Edge Glow & Channel Shift
            if (insanity > 0.15f) {
                val fringeAlpha = (insanity * 0.45f).coerceIn(0f, 0.6f)
                val fringeWidth = (size.width * (0.015f + insanity * 0.05f)).coerceIn(8.dp.toPx(), 45.dp.toPx())

                // Red fringe on left screen edge
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF1744).copy(alpha = fringeAlpha), Color.Transparent),
                        startX = 0f,
                        endX = fringeWidth
                    ),
                    size = Size(fringeWidth, size.height)
                )

                // Cyan fringe on right screen edge
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF00E5FF).copy(alpha = fringeAlpha)),
                        startX = size.width - fringeWidth,
                        endX = size.width
                    ),
                    topLeft = Offset(size.width - fringeWidth, 0f),
                    size = Size(fringeWidth, size.height)
                )

                // CRT / Glitch scanline horizontal tears when insanity is elevated
                if (insanity > 0.4f) {
                    val scanlineCount = 4 + (insanity * 8).toInt()
                    for (i in 0 until scanlineCount) {
                        val lineY = (size.height * ((sin(gameTime * 3.5f + i * 1.7f) * 0.5f + 0.5f))).coerceIn(0f, size.height)
                        val lineAlpha = if ((i + (gameTime * 15f).toInt()) % 3 == 0) (insanity * 0.25f) else (insanity * 0.10f)
                        val shiftOffset = if (i % 2 == 0) (insanity * 8.dp.toPx()) else (-insanity * 8.dp.toPx())
                        drawLine(
                            color = if (i % 2 == 0) Color(0xFFFF1744).copy(alpha = lineAlpha) else Color(0xFF00E5FF).copy(alpha = lineAlpha),
                            start = Offset(shiftOffset, lineY),
                            end = Offset(size.width + shiftOffset, lineY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            // 3. Flashlight beam specular bloom when flashlight is ON
            if (flashlightOn) {
                val center = Offset(size.width / 2f + jitterX, size.height * (if (playerState.isCrouching) 0.42f else 0.52f) + headBob * 2f + jitterY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x33FFF9C4), Color(0x10FFF9C4), Color.Transparent),
                        center = center,
                        radius = size.width * 0.42f
                    ),
                    radius = size.width * 0.42f,
                    center = center
                )
            }

            // 4. Vignette & Tunnel Vision Constriction (Sanity depletion + Monster proximity)
            val tunnelFactor = (1.0f - insanity * 0.45f).coerceIn(0.35f, 1.0f)
            val baseRadius = size.width * (0.85f - dangerProximity * 0.35f) * tunnelFactor
            val pulse = if (insanity > 0.25f) sin(gameTime * 3.8f) * (insanity * 20.dp.toPx()) else 0f
            val vignetteRadius = (baseRadius + pulse).coerceAtLeast(size.width * 0.22f)

            val vignetteColor = when {
                dangerProximity > 0.4f -> Color(0xAA4A0000)
                insanity > 0.6f -> Color(0xBB2A000A)
                else -> Color(0x99000000)
            }

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, vignetteColor),
                    center = Offset(size.width / 2f + jitterX, size.height / 2f + jitterY),
                    radius = vignetteRadius
                ),
                size = size
            )

            // 5. First-person Flashlight Hand / Weapon silhouette (bottom right corner)
            drawFirstPersonFlashlight(size.width, size.height, headBob, flashlightOn)

            // 6. Tactical Center Crosshair
            val crosshairCenter = Offset(size.width / 2f + jitterX, size.height / 2f + headBob * 1.5f + jitterY)
            val crosshairColor = if (hasCrosshairTarget) Color(0xFFFFD54F) else Color(0x77FFFFFF)
            val crosshairRadius = if (hasCrosshairTarget) 6.dp.toPx() else 3.dp.toPx()

            drawCircle(
                color = crosshairColor,
                radius = crosshairRadius,
                center = crosshairCenter,
                style = if (hasCrosshairTarget) Stroke(width = 2.dp.toPx()) else androidx.compose.ui.graphics.drawscope.Fill
            )

            if (hasCrosshairTarget) {
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = 2.dp.toPx(),
                    center = crosshairCenter
                )
            }

            // 6. Compact 3D Minimap Radar (top-right corner)
            drawMiniMap(
                canvasSize = size,
                raycastEngine = raycastEngine,
                playerState = playerState,
                monsterData = monsterData
            )
        }
    }
}

private fun DrawScope.drawFirstPersonFlashlight(
    w: Float,
    h: Float,
    headBob: Float,
    flashlightOn: Boolean
) {
    val baseX = w * 0.72f
    val baseY = h * 0.82f + headBob * 2f

    // Flashlight body (cylinder)
    val bodyColor = Color(0xFF263238)
    val rimColor = Color(0xFF455A64)
    val lensColor = if (flashlightOn) Color(0xFFFFF59D) else Color(0xFF546E7A)

    // Handle
    drawRect(
        color = bodyColor,
        topLeft = Offset(baseX + 20.dp.toPx(), baseY + 18.dp.toPx()),
        size = Size(28.dp.toPx(), 70.dp.toPx())
    )

    // Flashlight Head / Bezel
    drawRect(
        color = rimColor,
        topLeft = Offset(baseX + 10.dp.toPx(), baseY),
        size = Size(48.dp.toPx(), 22.dp.toPx())
    )

    // Glass Lens
    drawCircle(
        color = lensColor,
        radius = 16.dp.toPx(),
        center = Offset(baseX + 34.dp.toPx(), baseY + 11.dp.toPx())
    )

    // Glow aura from lens
    if (flashlightOn) {
        drawCircle(
            color = Color(0x66FFF59D),
            radius = 32.dp.toPx(),
            center = Offset(baseX + 34.dp.toPx(), baseY + 11.dp.toPx())
        )
    }
}

private fun DrawScope.drawMiniMap(
    canvasSize: Size,
    raycastEngine: RaycastEngine,
    playerState: PlayerState,
    monsterData: MonsterData?
) {
    val mapSize = 92.dp.toPx()
    val pad = 12.dp.toPx()
    val mapLeft = canvasSize.width - mapSize - pad
    val mapTop = 64.dp.toPx()

    // Background
    drawRect(
        color = Color(0xDD110E17),
        topLeft = Offset(mapLeft, mapTop),
        size = Size(mapSize, mapSize)
    )
    drawRect(
        color = Color(0x66FFFFFF),
        topLeft = Offset(mapLeft, mapTop),
        size = Size(mapSize, mapSize),
        style = Stroke(width = 1.dp.toPx())
    )

    val scale = mapSize / raycastEngine.mapWidth

    // Draw solid walls on minimap
    for (y in 0 until raycastEngine.mapHeight) {
        for (x in 0 until raycastEngine.mapWidth) {
            val tile = raycastEngine.getTile(x, y)
            if (tile in 1..7) {
                val wallColor = if (tile == 7) Color(0xFFFFD54F) else Color(0xFF5C5468)
                drawRect(
                    color = wallColor,
                    topLeft = Offset(mapLeft + x * scale, mapTop + y * scale),
                    size = Size(scale, scale)
                )
            }
        }
    }

    // Draw Monster on minimap if nearby
    if (monsterData != null && monsterData.state != MonsterState.IDLE) {
        val mx = mapLeft + monsterData.position.x * scale
        val my = mapTop + monsterData.position.y * scale
        drawCircle(
            color = Color(0xFFFF1744),
            radius = 3.dp.toPx(),
            center = Offset(mx, my)
        )
    }

    // Draw Player dot & orientation cone on minimap
    val px = mapLeft + playerState.position.x * scale
    val py = mapTop + playerState.position.y * scale

    drawCircle(
        color = Color(0xFF42A5F5),
        radius = 3.dp.toPx(),
        center = Offset(px, py)
    )

    // Player facing line
    val dirLen = 8.dp.toPx()
    val fx = px + cos(playerState.facingAngle) * dirLen
    val fy = py + sin(playerState.facingAngle) * dirLen
    drawLine(
        color = Color.White,
        start = Offset(px, py),
        end = Offset(fx, fy),
        strokeWidth = 2.dp.toPx()
    )
}
