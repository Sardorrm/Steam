package com.example.donttrustthehouse.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.donttrustthehouse.core.HouseManager
import com.example.donttrustthehouse.model.Door
import com.example.donttrustthehouse.model.GameItem
import com.example.donttrustthehouse.model.HideSpot
import com.example.donttrustthehouse.model.ItemType
import com.example.donttrustthehouse.model.LightSwitch
import com.example.donttrustthehouse.model.MonsterData
import com.example.donttrustthehouse.model.MonsterState
import com.example.donttrustthehouse.model.PlayerState
import com.example.donttrustthehouse.model.Room
import com.example.donttrustthehouse.model.Vector2D
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HouseCanvasView(
    modifier: Modifier = Modifier,
    houseManager: HouseManager,
    playerState: PlayerState,
    monsterData: MonsterData?,
    dangerProximity: Float,
    sanity: Float = 1.0f
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08060A))
    ) {
        val screenCenterX = size.width / 2f
        val screenCenterY = size.height / 2f

        // Camera scale: pixels per world unit
        val scale = (size.width / 36f).coerceIn(16f, 28f)

        fun toScreen(worldPos: Vector2D): Offset {
            val dx = worldPos.x - playerState.position.x
            val dy = worldPos.y - playerState.position.y
            return Offset(
                screenCenterX + dx * scale,
                screenCenterY + dy * scale
            )
        }

        // Draw rooms
        houseManager.rooms.values.forEach { room ->
            drawRoom(room, scale, ::toScreen, houseManager.isRoomLit(room.id))
        }

        // Draw doors
        houseManager.doors.forEach { door ->
            drawDoor(door, scale, ::toScreen)
        }

        // Draw light switches
        houseManager.lightSwitches.forEach { sw ->
            drawLightSwitch(sw, scale, ::toScreen)
        }

        // Draw hide spots
        houseManager.hideSpots.forEach { spot ->
            drawHideSpot(spot, scale, ::toScreen, spot.id == playerState.currentHideSpotId)
        }

        // Draw uncollected items
        houseManager.items.forEach { item ->
            if (!item.isCollected) {
                drawItem(item, scale, ::toScreen)
            }
        }

        // Draw noise rings
        if (playerState.noiseLevel > 0.05f && !playerState.isHiding) {
            val pCenter = toScreen(playerState.position)
            val ringRadius = (playerState.noiseLevel * 18f * scale).coerceAtLeast(10f)
            val ringColor = if (playerState.isCrouching) Color(0x5543A047) else Color(0x77E53935)
            drawCircle(
                color = ringColor,
                radius = ringRadius,
                center = pCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Draw monster if present
        monsterData?.let { monster ->
            drawMonster(monster, scale, ::toScreen)
        }

        // Draw player
        drawPlayer(playerState, scale, ::toScreen)

        // Draw atmospheric horror vignette overlay & insanity tunnel vision
        val insanity = (1.0f - sanity).coerceIn(0f, 1f)
        val vignetteColor = when {
            dangerProximity > 0.5f -> Color(0x88400000)
            insanity > 0.6f -> Color(0x992B000B)
            else -> Color(0x77000000)
        }

        val tunnelFactor = (1.0f - insanity * 0.45f).coerceIn(0.35f, 1.0f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, vignetteColor),
                center = Offset(screenCenterX, screenCenterY),
                radius = (size.width * (0.8f - dangerProximity * 0.3f) * tunnelFactor).coerceAtLeast(size.width * 0.25f)
            ),
            size = size
        )

        // Chromatic aberration fringes on 2D map when sanity is fractured
        if (insanity > 0.2f) {
            val fringeAlpha = (insanity * 0.35f).coerceIn(0f, 0.5f)
            val fringeWidth = (size.width * (0.02f + insanity * 0.04f)).coerceIn(6.dp.toPx(), 35.dp.toPx())

            // Red left fringe
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFF1744).copy(alpha = fringeAlpha), Color.Transparent),
                    startX = 0f,
                    endX = fringeWidth
                ),
                size = Size(fringeWidth, size.height)
            )

            // Cyan right fringe
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF00E5FF).copy(alpha = fringeAlpha)),
                    startX = size.width - fringeWidth,
                    endX = size.width
                ),
                topLeft = Offset(size.width - fringeWidth, 0f),
                size = Size(fringeWidth, size.height)
            )
        }
    }
}

private fun DrawScope.drawRoom(
    room: Room,
    scale: Float,
    toScreen: (Vector2D) -> Offset,
    isLit: Boolean
) {
    val topLeftWorld = Vector2D(room.center.x - room.width / 2f, room.center.y - room.height / 2f)
    val topLeft = toScreen(topLeftWorld)
    val wPx = room.width * scale
    val hPx = room.height * scale

    // Floor
    val floorCol = if (isLit) Color(room.floorColor) else Color(0xFF100E14)
    drawRect(
        color = floorCol,
        topLeft = topLeft,
        size = Size(wPx, hPx)
    )

    // Room tile grid lines
    val gridStep = 4f * scale
    var gx = topLeft.x
    while (gx < topLeft.x + wPx) {
        drawLine(
            color = Color(0x15FFFFFF),
            start = Offset(gx, topLeft.y),
            end = Offset(gx, topLeft.y + hPx),
            strokeWidth = 1f
        )
        gx += gridStep
    }
    var gy = topLeft.y
    while (gy < topLeft.y + hPx) {
        drawLine(
            color = Color(0x15FFFFFF),
            start = Offset(topLeft.x, gy),
            end = Offset(topLeft.x + wPx, gy),
            strokeWidth = 1f
        )
        gy += gridStep
    }

    // Walls (Outer boundary with stroke)
    drawRect(
        color = Color(0xFF4A4458),
        topLeft = topLeft,
        size = Size(wPx, hPx),
        style = Stroke(width = 3.dp.toPx())
    )

    // Darkness shadow if unlit
    if (!isLit) {
        drawRect(
            color = Color(0x99000000),
            topLeft = topLeft,
            size = Size(wPx, hPx)
        )
    }

    // Room Title
    val paint = Paint().apply {
        color = if (isLit) android.graphics.Color.argb(120, 200, 200, 220) else android.graphics.Color.argb(60, 150, 150, 170)
        textSize = 14.dp.toPx()
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val centerScreen = toScreen(room.center)
    drawContext.canvas.nativeCanvas.drawText(
        room.name.uppercase(),
        centerScreen.x,
        centerScreen.y + 5.dp.toPx(),
        paint
    )
}

private fun DrawScope.drawDoor(
    door: Door,
    scale: Float,
    toScreen: (Vector2D) -> Offset
) {
    val pos = toScreen(door.position)
    val doorWidth = 2.2f * scale
    val doorHeight = 0.6f * scale

    if (door.isExit) {
        // Front Exit Door: Glowing golden/green
        drawCircle(
            color = Color(0x55FFC107),
            radius = 16.dp.toPx(),
            center = pos
        )
        drawRect(
            color = Color(0xFFFFD54F),
            topLeft = Offset(pos.x - doorWidth / 2f, pos.y - doorHeight / 2f),
            size = Size(doorWidth, doorHeight)
        )
        val paint = Paint().apply {
            color = android.graphics.Color.argb(255, 255, 230, 100)
            textSize = 10.dp.toPx()
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        drawContext.canvas.nativeCanvas.drawText("EXIT", pos.x, pos.y - 12.dp.toPx(), paint)
    } else {
        // Interior Door
        val doorColor = if (door.isLocked) Color(0xFFD32F2F) else if (door.isOpen) Color(0xFF81C784) else Color(0xFFB0BEC5)
        if (door.isOpen) {
            // Open door drawn angled
            drawLine(
                color = doorColor,
                start = Offset(pos.x - doorWidth / 2f, pos.y),
                end = Offset(pos.x, pos.y - doorWidth * 0.7f),
                strokeWidth = 3.dp.toPx()
            )
        } else {
            // Closed door
            drawRect(
                color = doorColor,
                topLeft = Offset(pos.x - doorWidth / 2f, pos.y - doorHeight / 2f),
                size = Size(doorWidth, doorHeight)
            )
        }
    }
}

private fun DrawScope.drawLightSwitch(
    sw: LightSwitch,
    scale: Float,
    toScreen: (Vector2D) -> Offset
) {
    val pos = toScreen(sw.position)
    val glowColor = if (sw.isOn) Color(0x88FFEE58) else Color(0x33616161)
    val boxColor = if (sw.isOn) Color(0xFFFFF176) else Color(0xFF757575)

    drawCircle(color = glowColor, radius = 8.dp.toPx(), center = pos)
    drawCircle(color = boxColor, radius = 4.dp.toPx(), center = pos)
}

private fun DrawScope.drawHideSpot(
    spot: HideSpot,
    scale: Float,
    toScreen: (Vector2D) -> Offset,
    isPlayerInside: Boolean
) {
    val pos = toScreen(spot.position)
    val spotColor = if (isPlayerInside) Color(0xFF43A047) else Color(0xFF5C6BC0)

    // Hide icon box
    drawRect(
        color = spotColor.copy(alpha = 0.35f),
        topLeft = Offset(pos.x - 10.dp.toPx(), pos.y - 10.dp.toPx()),
        size = Size(20.dp.toPx(), 20.dp.toPx())
    )
    drawRect(
        color = spotColor,
        topLeft = Offset(pos.x - 10.dp.toPx(), pos.y - 10.dp.toPx()),
        size = Size(20.dp.toPx(), 20.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )

    val paint = Paint().apply {
        color = android.graphics.Color.argb(200, 180, 190, 220)
        textSize = 9.dp.toPx()
        textAlign = Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText("HIDE", pos.x, pos.y + 4.dp.toPx(), paint)
}

private fun DrawScope.drawPlayer(
    player: PlayerState,
    scale: Float,
    toScreen: (Vector2D) -> Offset
) {
    val pos = toScreen(player.position)
    val pRadius = if (player.isCrouching) 7.dp.toPx() else 9.dp.toPx()

    // Flashlight cone if not hiding
    if (!player.isHiding) {
        val coneLen = 6f * scale
        val coneAngle = 0.55f // radians
        val leftAngle = player.facingAngle - coneAngle
        val rightAngle = player.facingAngle + coneAngle

        val path = Path().apply {
            moveTo(pos.x, pos.y)
            lineTo(
                pos.x + cos(leftAngle) * coneLen,
                pos.y + sin(leftAngle) * coneLen
            )
            lineTo(
                pos.x + cos(rightAngle) * coneLen,
                pos.y + sin(rightAngle) * coneLen
            )
            close()
        }

        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FFFDE7), Color(0x00FFFDE7)),
                center = pos,
                radius = coneLen
            )
        )
    }

    // Stealth Aura / Crouch ring
    if (player.isCrouching) {
        drawCircle(
            color = Color(0x6643A047),
            radius = pRadius + 6.dp.toPx(),
            center = pos,
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // Player body
    val bodyColor = if (player.isHiding) Color(0xFF66BB6A) else Color(0xFF42A5F5)
    drawCircle(
        color = bodyColor,
        radius = pRadius,
        center = pos
    )
    drawCircle(
        color = Color.White,
        radius = pRadius,
        center = pos,
        style = Stroke(width = 2.dp.toPx())
    )

    // Heading indicator dot
    val headX = pos.x + cos(player.facingAngle) * (pRadius + 3.dp.toPx())
    val headY = pos.y + sin(player.facingAngle) * (pRadius + 3.dp.toPx())
    drawCircle(
        color = Color.White,
        radius = 2.5.dp.toPx(),
        center = Offset(headX, headY)
    )

    // "YOU" Label
    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 9.dp.toPx()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val label = if (player.isHiding) "[HIDDEN]" else if (player.isCrouching) "[CROUCH]" else "YOU"
    drawContext.canvas.nativeCanvas.drawText(label, pos.x, pos.y - pRadius - 4.dp.toPx(), paint)
}

private fun DrawScope.drawMonster(
    monster: MonsterData,
    scale: Float,
    toScreen: (Vector2D) -> Offset
) {
    if (monster.state == MonsterState.IDLE) return

    val pos = toScreen(monster.position)
    val mRadius = 12.dp.toPx()

    // Vision cone (translucent red)
    val coneLen = 7f * scale
    val coneAngle = 0.65f
    val leftAngle = monster.facingAngle - coneAngle
    val rightAngle = monster.facingAngle + coneAngle

    val path = Path().apply {
        moveTo(pos.x, pos.y)
        lineTo(
            pos.x + cos(leftAngle) * coneLen,
            pos.y + sin(leftAngle) * coneLen
        )
        lineTo(
            pos.x + cos(rightAngle) * coneLen,
            pos.y + sin(rightAngle) * coneLen
        )
        close()
    }

    val coneBrush = Brush.radialGradient(
        colors = listOf(Color(0x55E53935), Color(0x05E53935)),
        center = pos,
        radius = coneLen
    )
    drawPath(path = path, brush = coneBrush)

    // Monster body aura
    drawCircle(
        color = Color(0x44D50000),
        radius = mRadius + 8.dp.toPx(),
        center = pos
    )

    // Monster shadow core
    drawCircle(
        color = Color(0xFF1A0505),
        radius = mRadius,
        center = pos
    )
    drawCircle(
        color = Color(0xFFB71C1C),
        radius = mRadius,
        center = pos,
        style = Stroke(width = 2.dp.toPx())
    )

    // Glowing red eyes
    val eyeDist = 4.dp.toPx()
    val eyeAngle = monster.facingAngle
    val perpAngle = eyeAngle + (Math.PI / 2.0).toFloat()

    val leftEye = Offset(
        pos.x + cos(eyeAngle) * 5.dp.toPx() + cos(perpAngle) * eyeDist,
        pos.y + sin(eyeAngle) * 5.dp.toPx() + sin(perpAngle) * eyeDist
    )
    val rightEye = Offset(
        pos.x + cos(eyeAngle) * 5.dp.toPx() - cos(perpAngle) * eyeDist,
        pos.y + sin(eyeAngle) * 5.dp.toPx() - sin(perpAngle) * eyeDist
    )

    drawCircle(color = Color(0xFFFF1744), radius = 2.5.dp.toPx(), center = leftEye)
    drawCircle(color = Color(0xFFFF1744), radius = 2.5.dp.toPx(), center = rightEye)

    // Alert status indicator above monster
    val statusSymbol = when (monster.state) {
        MonsterState.HUNTING -> "!"
        MonsterState.INVESTIGATING -> "?"
        else -> ""
    }

    if (statusSymbol.isNotEmpty()) {
        val paint = Paint().apply {
            color = if (monster.state == MonsterState.HUNTING) android.graphics.Color.RED else android.graphics.Color.YELLOW
            textSize = 14.dp.toPx()
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        drawContext.canvas.nativeCanvas.drawText(statusSymbol, pos.x, pos.y - mRadius - 4.dp.toPx(), paint)
    }
}

private fun DrawScope.drawItem(
    item: GameItem,
    scale: Float,
    toScreen: (Vector2D) -> Offset
) {
    val pos = toScreen(item.position)
    val color = when (item.type) {
        ItemType.KEY -> Color(0xFFFFD54F)
        ItemType.SEDATIVE -> Color(0xFF00E5FF)
        ItemType.BATTERY -> Color(0xFFFFEB3B)
        ItemType.TALISMAN -> Color(0xFFE040FB)
        ItemType.NOTE -> Color(0xFFFFCC80)
    }

    drawCircle(
        color = color.copy(alpha = 0.35f),
        radius = 7.dp.toPx(),
        center = pos
    )
    drawCircle(
        color = color,
        radius = 3.5.dp.toPx(),
        center = pos
    )

    val paint = Paint().apply {
        this.color = android.graphics.Color.argb(220, 255, 255, 255)
        textSize = 8.dp.toPx()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    drawContext.canvas.nativeCanvas.drawText(item.name.take(7), pos.x, pos.y - 7.dp.toPx(), paint)
}
