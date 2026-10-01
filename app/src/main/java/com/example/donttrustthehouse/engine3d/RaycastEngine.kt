package com.example.donttrustthehouse.engine3d

import android.graphics.Bitmap
import com.example.donttrustthehouse.model.Vector2D
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class RayHit(
    val mapX: Int,
    val mapY: Int,
    val wallType: Int,
    val distance: Float,
    val side: Int // 0 = vertical wall, 1 = horizontal wall
)

data class Sprite3D(
    val x: Float,
    val y: Float,
    val texture: IntArray,
    val scale: Float = 1.0f,
    val isMonster: Boolean = false
)

class RaycastEngine(
    val screenWidth: Int = 320,
    val screenHeight: Int = 200
) {
    val bitmap: Bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
    private val pixelBuffer = IntArray(screenWidth * screenHeight)
    private val aberrationBuffer = IntArray(screenWidth * screenHeight)
    private val zBuffer = FloatArray(screenWidth)

    // 32x32 House Grid Map
    // 0: empty, 1: Living Room, 2: Bedroom, 3: Kitchen, 4: Bathroom, 5: Basement, 6: Door, 7: Exit, 8: Open Door
    val mapWidth = 32
    val mapHeight = 32
    val map = IntArray(mapWidth * mapHeight)

    init {
        buildHouseGrid()
    }

    private fun setTile(x: Int, y: Int, tile: Int) {
        if (x in 0 until mapWidth && y in 0 until mapHeight) {
            map[y * mapWidth + x] = tile
        }
    }

    fun getTile(x: Int, y: Int): Int {
        if (x !in 0 until mapWidth || y !in 0 until mapHeight) return 1
        return map[y * mapWidth + x]
    }

    fun isWalkable(x: Float, y: Float): Boolean {
        val mx = x.toInt()
        val my = y.toInt()
        val tile = getTile(mx, my)
        return tile == 0 || tile == 8 // empty or open door
    }

    private fun buildHouseGrid() {
        // Outer boundary walls
        for (x in 0 until mapWidth) {
            setTile(x, 0, 1)
            setTile(x, mapHeight - 1, 5)
        }
        for (y in 0 until mapHeight) {
            setTile(0, y, 3)
            setTile(mapWidth - 1, y, 2)
        }

        // Room 1: Living Room (Center: x: 10..21, y: 10..21)
        // Walls surrounding living room
        for (x in 10..21) {
            setTile(x, 10, 1) // North wall (borders Bathroom)
            setTile(x, 21, 1) // South wall (Exit & borders Basement)
        }
        for (y in 10..21) {
            setTile(10, y, 1) // West wall (borders Kitchen)
            setTile(21, y, 1) // East wall (borders Bedroom)
        }

        // Room 2: Bedroom (East: x: 22..30, y: 10..21)
        for (x in 22..30) {
            setTile(x, 10, 2)
            setTile(x, 21, 2)
        }
        for (y in 10..21) {
            setTile(30, y, 2)
        }

        // Room 3: Kitchen (West: x: 1..9, y: 10..21)
        for (x in 1..9) {
            setTile(x, 10, 3)
            setTile(x, 21, 3)
        }
        for (y in 10..21) {
            setTile(1, y, 3)
        }

        // Room 4: Bathroom (North: x: 10..21, y: 1..9)
        for (x in 10..21) {
            setTile(x, 1, 4)
        }
        for (y in 1..9) {
            setTile(10, y, 4)
            setTile(21, y, 4)
        }

        // Room 5: Basement (South: x: 10..21, y: 22..30)
        for (x in 10..21) {
            setTile(x, 30, 5)
        }
        for (y in 22..30) {
            setTile(10, y, 5)
            setTile(21, y, 5)
        }

        // Doors
        setTile(16, 21, 7) // Front Exit Door (Living Room south)
        setTile(21, 16, 6) // Bedroom Door (Living Room east)
        setTile(10, 16, 6) // Kitchen Door (Living Room west)
        setTile(16, 10, 6) // Bathroom Door (Living Room north)
        setTile(12, 21, 6) // Basement Door (Living Room south-west)
    }

    fun setDoorOpen(mapX: Int, mapY: Int, open: Boolean) {
        val current = getTile(mapX, mapY)
        if (current == 6 && open) {
            setTile(mapX, mapY, 8) // Open door
        } else if (current == 8 && !open) {
            setTile(mapX, mapY, 6) // Closed door
        }
    }

    fun renderFrame(
        playerX: Float,
        playerY: Float,
        playerAngle: Float,
        isCrouching: Boolean,
        flashlightOn: Boolean,
        headBob: Float,
        sprites: List<Sprite3D>,
        isLitRoom: Boolean,
        dangerProximity: Float,
        sanity: Float = 1.0f,
        timeSec: Float = 0f
    ): Bitmap {
        // Direction and Camera plane (FOV ~ 66 degrees)
        val dirX = cos(playerAngle)
        val dirY = sin(playerAngle)
        val fov = 0.66f
        val planeX = -dirY * fov
        val planeY = dirX * fov

        val eyeHeight = if (isCrouching) 0.38f else 0.5f
        val horizon = (screenHeight * eyeHeight + headBob).toInt()

        // 1. Draw Ceiling and Floor with depth gradient
        renderCeilingAndFloor(horizon, dangerProximity)

        // 2. DDA Raycast for each column
        for (x in 0 until screenWidth) {
            val cameraX = 2f * x / screenWidth - 1f
            val rayDirX = dirX + planeX * cameraX
            val rayDirY = dirY + planeY * cameraX

            var mapX = playerX.toInt()
            var mapY = playerY.toInt()

            val deltaDistX = if (abs(rayDirX) < 1e-6f) 1e30f else abs(1f / rayDirX)
            val deltaDistY = if (abs(rayDirY) < 1e-6f) 1e30f else abs(1f / rayDirY)

            var stepX: Int
            var sideDistX: Float
            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (playerX - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1f - playerX) * deltaDistX
            }

            var stepY: Int
            var sideDistY: Float
            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (playerY - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1f - playerY) * deltaDistY
            }

            var hit = false
            var side = 0 // 0 = X-axis, 1 = Y-axis
            var wallType = 0

            var raySteps = 0
            while (!hit && raySteps < 48) {
                raySteps++
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }

                val tile = getTile(mapX, mapY)
                if (tile in 1..7) { // Solid wall or closed door
                    hit = true
                    wallType = tile
                }
            }

            val perpWallDist = if (side == 0) {
                (mapX - playerX + (1 - stepX) / 2f) / rayDirX
            } else {
                (mapY - playerY + (1 - stepY) / 2f) / rayDirY
            }.coerceAtLeast(0.1f)

            zBuffer[x] = perpWallDist

            // Calculate wall slice height
            val lineHeight = (screenHeight / perpWallDist).toInt()
            val drawStart = (horizon - lineHeight / 2).coerceIn(0, screenHeight - 1)
            val drawEnd = (horizon + lineHeight / 2).coerceIn(0, screenHeight - 1)

            // Wall texture coordinate
            var wallX = if (side == 0) playerY + perpWallDist * rayDirY else playerX + perpWallDist * rayDirX
            wallX -= wallX.toInt()

            var texX = (wallX * Textures.TEX_SIZE).toInt()
            if ((side == 0 && rayDirX > 0) || (side == 1 && rayDirY < 0)) {
                texX = Textures.TEX_SIZE - texX - 1
            }
            texX = texX.coerceIn(0, Textures.TEX_SIZE - 1)

            val texture = Textures.getWallTexture(wallType)

            // Lighting calculation (Flashlight spotlight + distance fog)
            val centerDist = abs(x - screenWidth / 2f) / (screenWidth / 2f)
            val flashlightIntensity = if (flashlightOn) {
                val spot = (1f - centerDist * 1.6f).coerceIn(0f, 1f)
                (spot * 1.5f / (1f + perpWallDist * 0.15f))
            } else 0.1f

            val baseAmbient = if (isLitRoom) 0.35f else 0.12f
            val distanceDecay = (1f / (1f + perpWallDist * 0.22f)).coerceIn(0f, 1f)
            val totalLight = (baseAmbient * distanceDecay + flashlightIntensity).coerceIn(0.04f, 1.25f)
            val sideDim = if (side == 1) 0.78f else 1.0f

            val step = 1.0f * Textures.TEX_SIZE / lineHeight
            var texPos = (drawStart - horizon + lineHeight / 2f) * step

            for (y in drawStart..drawEnd) {
                val texY = texPos.toInt().coerceIn(0, Textures.TEX_SIZE - 1)
                texPos += step
                val texColor = texture[texY * Textures.TEX_SIZE + texX]
                pixelBuffer[y * screenWidth + x] = applyLighting(texColor, totalLight * sideDim, dangerProximity)
            }
        }

        // 3. Render 3D Billboard Sprites (Monster, Closets, etc.)
        renderSprites(playerX, playerY, dirX, dirY, planeX, planeY, horizon, sprites, flashlightOn, isLitRoom, dangerProximity)

        // 4. Psychological Distortion & Chromatic Aberration based on Sanity depletion
        if (sanity < 0.96f) {
            applyChromaticAberration(sanity, timeSec)
        }

        // Transfer pixel buffer to Bitmap
        bitmap.setPixels(pixelBuffer, 0, screenWidth, 0, 0, screenWidth, screenHeight)
        return bitmap
    }

    /**
     * Splits red and blue spectral channels with lateral displacement and subtle scanline warping
     * as player sanity depletes, simulating psychological visual breakdown.
     */
    private fun applyChromaticAberration(sanity: Float, timeSec: Float) {
        val insanity = (1.0f - sanity).coerceIn(0f, 1f)
        // Shift distance increases non-linearly with mental breakdown (1 to 10 pixels)
        val jitter = if (insanity > 0.55f && (sin(timeSec * 22.0) > 0.65)) 2 else 0
        val shiftX = ((insanity * insanity) * 7.5f).toInt() + jitter
        if (shiftX <= 0) return

        System.arraycopy(pixelBuffer, 0, aberrationBuffer, 0, pixelBuffer.size)

        val hasVerticalWave = insanity > 0.35f
        for (y in 0 until screenHeight) {
            val rowOffset = y * screenWidth
            val vOffset = if (hasVerticalWave) {
                (sin(y * 0.12 + timeSec * 6.0) * (insanity * 2.2f)).toInt()
            } else 0

            val targetY = (y + vOffset).coerceIn(0, screenHeight - 1)
            val targetRowOffset = targetY * screenWidth

            for (x in 0 until screenWidth) {
                val redX = (x + shiftX).coerceIn(0, screenWidth - 1)
                val blueX = (x - shiftX).coerceIn(0, screenWidth - 1)

                val redPixel = aberrationBuffer[targetRowOffset + redX]
                val centerPixel = aberrationBuffer[rowOffset + x]
                val bluePixel = aberrationBuffer[targetRowOffset + blueX]

                val r = (redPixel ushr 16) and 0xFF
                val g = (centerPixel ushr 8) and 0xFF
                val b = bluePixel and 0xFF
                val a = (centerPixel ushr 24) and 0xFF

                // Sensory static flicker at extreme insanity (< 25%)
                val noise = if (insanity > 0.75f && (x + y + (timeSec * 45).toInt()) % 11 == 0) {
                    (Random.nextInt(40) - 20)
                } else 0

                val finalR = (r + noise).coerceIn(0, 255)
                val finalG = (g + noise).coerceIn(0, 255)
                val finalB = (b + noise).coerceIn(0, 255)

                pixelBuffer[rowOffset + x] = (a shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }
    }

    private fun renderCeilingAndFloor(horizon: Int, dangerProximity: Float) {
        val ceilingR = (12 + dangerProximity * 20).toInt()
        val ceilingG = 10
        val ceilingB = 14
        val ceilingColor = (255 shl 24) or (ceilingR shl 16) or (ceilingG shl 8) or ceilingB

        val floorR = (20 + dangerProximity * 15).toInt()
        val floorG = 16
        val floorB = 22
        val floorColor = (255 shl 24) or (floorR shl 16) or (floorG shl 8) or floorB

        for (y in 0 until horizon) {
            val shade = (y.toFloat() / horizon * 0.4f)
            val r = (ceilingR * shade).toInt()
            val g = (ceilingG * shade).toInt()
            val b = (ceilingB * shade).toInt()
            val col = (255 shl 24) or (r shl 16) or (g shl 8) or b
            for (x in 0 until screenWidth) {
                pixelBuffer[y * screenWidth + x] = col
            }
        }
        for (y in horizon until screenHeight) {
            val shade = ((screenHeight - y).toFloat() / (screenHeight - horizon) * 0.45f)
            val r = (floorR * shade).toInt()
            val g = (floorG * shade).toInt()
            val b = (floorB * shade).toInt()
            val col = (255 shl 24) or (r shl 16) or (g shl 8) or b
            for (x in 0 until screenWidth) {
                pixelBuffer[y * screenWidth + x] = col
            }
        }
    }

    private fun renderSprites(
        playerX: Float,
        playerY: Float,
        dirX: Float,
        dirY: Float,
        planeX: Float,
        planeY: Float,
        horizon: Int,
        sprites: List<Sprite3D>,
        flashlightOn: Boolean,
        isLitRoom: Boolean,
        dangerProximity: Float
    ) {
        if (sprites.isEmpty()) return

        // Sort sprites from furthest to closest
        val sortedSprites = sprites.map { sprite ->
            val dist = ((playerX - sprite.x) * (playerX - sprite.x) + (playerY - sprite.y) * (playerY - sprite.y))
            Pair(dist, sprite)
        }.sortedByDescending { it.first }

        val invDet = 1.0f / (planeX * dirY - dirX * planeY)

        for ((_, sprite) in sortedSprites) {
            val spriteX = sprite.x - playerX
            val spriteY = sprite.y - playerY

            val transformX = invDet * (dirY * spriteX - dirX * spriteY)
            val transformY = invDet * (-planeY * spriteX + planeX * spriteY) // depth in camera space

            if (transformY <= 0.2f) continue // behind camera or too close

            val spriteScreenX = ((screenWidth / 2f) * (1f + transformX / transformY)).toInt()

            val spriteSize = abs((screenHeight / transformY * sprite.scale).toInt())
            val drawStartY = (horizon - spriteSize / 2).coerceIn(0, screenHeight - 1)
            val drawEndY = (horizon + spriteSize / 2).coerceIn(0, screenHeight - 1)

            val drawStartX = (spriteScreenX - spriteSize / 2).coerceIn(0, screenWidth - 1)
            val drawEndX = (spriteScreenX + spriteSize / 2).coerceIn(0, screenWidth - 1)

            val dist = sqrt(spriteX * spriteX + spriteY * spriteY)
            val centerDist = abs(spriteScreenX - screenWidth / 2f) / (screenWidth / 2f)
            val flashlightIntensity = if (flashlightOn) {
                val spot = (1f - centerDist * 1.5f).coerceIn(0f, 1f)
                (spot * 1.8f / (1f + dist * 0.15f))
            } else 0.15f

            val baseAmbient = if (isLitRoom) 0.45f else 0.2f
            val distanceDecay = (1f / (1f + dist * 0.2f)).coerceIn(0f, 1f)
            val light = (baseAmbient * distanceDecay + flashlightIntensity).coerceIn(0.1f, 1.4f)

            for (stripe in drawStartX..drawEndX) {
                val texX = (((stripe - (spriteScreenX - spriteSize / 2f)) * Textures.TEX_SIZE) / spriteSize).toInt()
                    .coerceIn(0, Textures.TEX_SIZE - 1)

                if (transformY < zBuffer[stripe]) {
                    for (y in drawStartY..drawEndY) {
                        val d = (y - horizon + spriteSize / 2f)
                        val texY = ((d * Textures.TEX_SIZE) / spriteSize).toInt().coerceIn(0, Textures.TEX_SIZE - 1)
                        val color = sprite.texture[texY * Textures.TEX_SIZE + texX]
                        val alpha = (color ushr 24) and 0xFF
                        if (alpha > 30) {
                            val shaded = applyLighting(color, light, if (sprite.isMonster) 0f else dangerProximity)
                            pixelBuffer[y * screenWidth + stripe] = shaded
                        }
                    }
                }
            }
        }
    }

    private fun applyLighting(argb: Int, light: Float, dangerBlood: Float): Int {
        val a = (argb ushr 24) and 0xFF
        var r = (argb ushr 16) and 0xFF
        var g = (argb ushr 8) and 0xFF
        var b = argb and 0xFF

        // Distance & Flashlight shading
        r = (r * light).toInt().coerceIn(0, 255)
        g = (g * light).toInt().coerceIn(0, 255)
        b = (b * light).toInt().coerceIn(0, 255)

        // Blood vignette tint when monster is near
        if (dangerBlood > 0.3f) {
            val bloodTint = (dangerBlood * 80f).toInt()
            r = (r + bloodTint).coerceIn(0, 255)
            g = (g - bloodTint / 2).coerceIn(0, 255)
            b = (b - bloodTint / 2).coerceIn(0, 255)
        }

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Casts a ray directly in front of player (center crosshair) to find interactive targets.
     */
    fun checkCrosshairTarget(playerX: Float, playerY: Float, playerAngle: Float): RayHit? {
        val dirX = cos(playerAngle)
        val dirY = sin(playerAngle)
        val deltaDistX = if (abs(dirX) < 1e-6f) 1e30f else abs(1f / dirX)
        val deltaDistY = if (abs(dirY) < 1e-6f) 1e30f else abs(1f / dirY)

        var mapX = playerX.toInt()
        var mapY = playerY.toInt()

        var stepX: Int
        var sideDistX: Float
        if (dirX < 0) {
            stepX = -1
            sideDistX = (playerX - mapX) * deltaDistX
        } else {
            stepX = 1
            sideDistX = (mapX + 1f - playerX) * deltaDistX
        }

        var stepY: Int
        var sideDistY: Float
        if (dirY < 0) {
            stepY = -1
            sideDistY = (playerY - mapY) * deltaDistY
        } else {
            stepY = 1
            sideDistY = (mapY + 1f - playerY) * deltaDistY
        }

        var side = 0
        for (i in 0 until 12) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX
                mapX += stepX
                side = 0
            } else {
                sideDistY += deltaDistY
                mapY += stepY
                side = 1
            }

            val tile = getTile(mapX, mapY)
            if (tile in 1..8) {
                val dist = if (side == 0) {
                    (mapX - playerX + (1 - stepX) / 2f) / dirX
                } else {
                    (mapY - playerY + (1 - stepY) / 2f) / dirY
                }
                if (dist <= 2.8f) {
                    return RayHit(mapX, mapY, tile, dist, side)
                }
                return null
            }
        }
        return null
    }
}
