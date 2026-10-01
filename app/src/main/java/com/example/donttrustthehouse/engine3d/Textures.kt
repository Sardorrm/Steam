package com.example.donttrustthehouse.engine3d

import com.example.donttrustthehouse.model.ItemType
import kotlin.math.abs
import kotlin.math.sin

object Textures {
    const val TEX_SIZE = 64

    // Wall texture arrays: size 64 * 64 each (ARGB Ints)
    val wallpaper = IntArray(TEX_SIZE * TEX_SIZE)
    val woodPanels = IntArray(TEX_SIZE * TEX_SIZE)
    val kitchenTiles = IntArray(TEX_SIZE * TEX_SIZE)
    val bathroomTiles = IntArray(TEX_SIZE * TEX_SIZE)
    val basementStone = IntArray(TEX_SIZE * TEX_SIZE)
    val doorWood = IntArray(TEX_SIZE * TEX_SIZE)
    val exitDoor = IntArray(TEX_SIZE * TEX_SIZE)

    // Sprite textures (ARGB with alpha channel)
    val monsterSprite = IntArray(TEX_SIZE * TEX_SIZE)
    val hideSpotSprite = IntArray(TEX_SIZE * TEX_SIZE)
    val itemKeySprite = IntArray(TEX_SIZE * TEX_SIZE)
    val itemSedativeSprite = IntArray(TEX_SIZE * TEX_SIZE)
    val itemBatterySprite = IntArray(TEX_SIZE * TEX_SIZE)
    val itemTalismanSprite = IntArray(TEX_SIZE * TEX_SIZE)
    val itemNoteSprite = IntArray(TEX_SIZE * TEX_SIZE)

    init {
        generateTextures()
    }

    private fun color(a: Int, r: Int, g: Int, b: Int): Int {
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun generateTextures() {
        // 1. Wallpaper (Living Room: Victorian damask/striped dark crimson/purple)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val stripe = (x / 8) % 2 == 0
                val pattern = ((x xor y) and 15) < 3
                val baseR = if (stripe) 85 else 60
                val baseG = if (stripe) 35 else 25
                val baseB = if (stripe) 45 else 32
                val add = if (pattern) 25 else 0
                wallpaper[y * TEX_SIZE + x] = color(255, baseR + add, baseG + add, baseB + add)
            }
        }

        // 2. Wood Panels (Bedroom: Vertical wood planks with grain)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val plankBorder = x % 16 == 0 || x % 16 == 15
                val grain = ((x * 7 + y * 13) % 17) - 8
                if (plankBorder) {
                    woodPanels[y * TEX_SIZE + x] = color(255, 25, 15, 10)
                } else {
                    val r = (75 + grain).coerceIn(30, 120)
                    val g = (45 + grain / 2).coerceIn(20, 80)
                    val b = (28 + grain / 3).coerceIn(10, 50)
                    woodPanels[y * TEX_SIZE + x] = color(255, r, g, b)
                }
            }
        }

        // 3. Kitchen Tiles (Dirty checkered tile with mortar)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val mortar = x % 16 == 0 || y % 16 == 0
                val tileCheck = ((x / 16) + (y / 16)) % 2 == 0
                if (mortar) {
                    kitchenTiles[y * TEX_SIZE + x] = color(255, 30, 30, 35)
                } else {
                    val base = if (tileCheck) 90 else 50
                    val grime = ((x * 3 + y * 5) % 11)
                    kitchenTiles[y * TEX_SIZE + x] = color(255, base + grime, base + grime, base + grime + 10)
                }
            }
        }

        // 4. Bathroom Tiles (Eerie cyan/green damp ceramic)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val grout = x % 16 == 0 || y % 16 == 0
                if (grout) {
                    bathroomTiles[y * TEX_SIZE + x] = color(255, 20, 35, 35)
                } else {
                    val mold = if ((x + y * 2) % 19 < 4) 30 else 0
                    val r = (30 + mold).coerceIn(0, 100)
                    val g = (70 + mold).coerceIn(0, 130)
                    val b = (75 - mold).coerceIn(0, 130)
                    bathroomTiles[y * TEX_SIZE + x] = color(255, r, g, b)
                }
            }
        }

        // 5. Basement Stone (Dark cracked rough masonry)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val stoneX = x / 16
                val stoneY = y / 8
                val border = (x % 16 == 0 && stoneY % 2 == 0) || (x % 16 == 8 && stoneY % 2 != 0) || (y % 8 == 0)
                val noise = ((x * 17 + y * 31) % 23)
                if (border) {
                    basementStone[y * TEX_SIZE + x] = color(255, 18, 18, 22)
                } else {
                    val tone = 40 + noise
                    basementStone[y * TEX_SIZE + x] = color(255, tone, tone, tone + 4)
                }
            }
        }

        // 6. Wooden Door (Door frame, wooden panels, brass handle)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val frame = x < 4 || x > 59 || y < 4 || y > 59
                val middleBar = y in 28..32 || x in 28..32
                val isHandle = (x in 8..14 && y in 33..37)
                if (frame || middleBar) {
                    doorWood[y * TEX_SIZE + x] = color(255, 45, 25, 15)
                } else if (isHandle) {
                    doorWood[y * TEX_SIZE + x] = color(255, 220, 180, 50) // brass handle
                } else {
                    val grain = ((y * 7) % 13)
                    doorWood[y * TEX_SIZE + x] = color(255, 90 + grain, 55 + grain, 30 + grain)
                }
            }
        }

        // 7. Exit Door (Heavy reinforced iron door with bright red/gold illuminated EXIT sign)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val frame = x < 4 || x > 59 || y < 4 || y > 59
                val isExitSign = (x in 16..48 && y in 10..22)
                val isSignBorder = isExitSign && (x in 16..17 || x in 47..48 || y in 10..11 || y in 21..22)
                val isExitLetter = isExitSign && !isSignBorder && (
                    // Simple "EXIT" text pixel mask inside the sign box
                    (x in 20..22 && y in 13..19) || (x in 20..26 && (y == 13 || y == 16 || y == 19)) || // E
                    ((x - y) in 7..9 && x in 28..34 && y in 13..19) || ((x + y) in 45..47 && x in 28..34 && y in 13..19) || // X
                    (x in 37..39 && y in 13..19) || (x in 36..40 && (y == 13 || y == 19)) || // I
                    (x in 43..45 && y in 13..19) || (x in 41..47 && y == 13) // T
                )

                if (isExitLetter) {
                    exitDoor[y * TEX_SIZE + x] = color(255, 255, 50, 50) // Glowing bright red text
                } else if (isExitSign) {
                    exitDoor[y * TEX_SIZE + x] = if (isSignBorder) color(255, 255, 200, 50) else color(255, 40, 10, 10)
                } else if (frame) {
                    exitDoor[y * TEX_SIZE + x] = color(255, 30, 30, 35)
                } else {
                    // Steel door surface with hazard stripes along bottom
                    val isHazard = y > 46 && ((x + y) % 12 < 6)
                    if (isHazard) {
                        exitDoor[y * TEX_SIZE + x] = color(255, 200, 160, 20)
                    } else {
                        val steel = 60 + ((x * 5 + y * 11) % 9)
                        exitDoor[y * TEX_SIZE + x] = color(255, steel, steel, steel + 5)
                    }
                }
            }
        }

        // 8. Monster Sprite (Terrifying shadowed entity with glowing crimson eyes, sharp horns, and shadowy tendrils)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val dx = x - 32
                val dy = y - 32
                val dist = dx * dx + dy * dy

                val inHead = (dx * dx * 1.3f + (dy + 8) * (dy + 8) * 1.1f) < 220f
                val inTorso = (dx * dx * 0.8f + (dy - 12) * (dy - 12) * 0.7f) < 280f
                val inHorns = (abs(dx) in 10..18 && dy in -26..-14 && (abs(dx) + dy * 0.6f) < 14)

                val isLeftEye = (x in 24..27 && y in 20..23)
                val isRightEye = (x in 37..40 && y in 20..23)
                val isEyeGlow = (x in 23..28 && y in 19..24) || (x in 36..41 && y in 19..24)

                val isMouth = (x in 26..38 && y in 30..34 && ((x + y) % 3 == 0))

                if (isLeftEye || isRightEye) {
                    monsterSprite[y * TEX_SIZE + x] = color(255, 255, 20, 20) // Glowing eyes
                } else if (isEyeGlow) {
                    monsterSprite[y * TEX_SIZE + x] = color(180, 220, 0, 0)
                } else if (isMouth) {
                    monsterSprite[y * TEX_SIZE + x] = color(255, 255, 240, 230) // Jagged fangs
                } else if (inHead || inTorso || inHorns) {
                    // Shadow body with subtle crimson vein pulse
                    val vein = ((x * 13 + y * 7) % 19 < 2)
                    if (vein) {
                        monsterSprite[y * TEX_SIZE + x] = color(240, 140, 10, 10)
                    } else {
                        monsterSprite[y * TEX_SIZE + x] = color(250, 16, 8, 14)
                    }
                } else if (dist < 400f) {
                    // Shadow mist aura
                    val alpha = ((400f - dist) / 400f * 120f).toInt().coerceIn(0, 255)
                    monsterSprite[y * TEX_SIZE + x] = color(alpha, 30, 5, 8)
                } else {
                    monsterSprite[y * TEX_SIZE + x] = 0 // Transparent
                }
            }
        }

        // 9. Hide Spot Sprite (Wardrobe / Cabinet / Crate silhouette)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val inBox = x in 10..54 && y in 8..58
                val border = inBox && (x in 10..12 || x in 52..54 || y in 8..10 || y in 56..58 || x in 31..33)
                val handle = inBox && (y in 32..36 && (x in 26..28 || x in 36..38))
                if (border) {
                    hideSpotSprite[y * TEX_SIZE + x] = color(255, 50, 40, 60)
                } else if (handle) {
                    hideSpotSprite[y * TEX_SIZE + x] = color(255, 180, 160, 220)
                } else if (inBox) {
                    hideSpotSprite[y * TEX_SIZE + x] = color(240, 80, 70, 95)
                } else {
                    hideSpotSprite[y * TEX_SIZE + x] = 0 // Transparent
                }
            }
        }

        // 10. Item Sprites (Key, Sedative, Battery, Talisman, Note)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val dx = x - 32f
                val dy = y - 32f
                val dist = dx * dx + dy * dy

                // Key: Golden antique skeleton key shape
                val keyHead = (x - 32) * (x - 32) + (y - 20) * (y - 20) in 25..81
                val keyShaft = (x in 30..34) && (y in 24..50)
                val keyTeeth = (x in 34..42) && (y in 40..43 || y in 47..50)
                if (keyHead || keyShaft || keyTeeth) {
                    itemKeySprite[y * TEX_SIZE + x] = color(255, 255, 215, 0)
                } else if (dist < 400f) {
                    val aura = ((400f - dist) / 400f * 90f).toInt().coerceIn(0, 255)
                    itemKeySprite[y * TEX_SIZE + x] = color(aura, 255, 200, 50)
                } else {
                    itemKeySprite[y * TEX_SIZE + x] = 0
                }

                // Sedative: Medical vial / syringe with glowing cyan tranquilizer liquid
                val vialBody = (x in 24..40) && (y in 22..52)
                val vialCap = (x in 28..36) && (y in 14..21)
                val vialCross = ((x in 30..34) && (y in 32..42)) || ((x in 26..38) && (y in 35..39))
                if (vialCap) {
                    itemSedativeSprite[y * TEX_SIZE + x] = color(255, 180, 190, 205)
                } else if (vialCross) {
                    itemSedativeSprite[y * TEX_SIZE + x] = color(255, 0, 230, 118)
                } else if (vialBody) {
                    val fluidGlow = if (x in 26..38 && y in 28..50) color(240, 20, 180, 240) else color(255, 60, 90, 110)
                    itemSedativeSprite[y * TEX_SIZE + x] = fluidGlow
                } else if (dist < 400f) {
                    val aura = ((400f - dist) / 400f * 80f).toInt().coerceIn(0, 255)
                    itemSedativeSprite[y * TEX_SIZE + x] = color(aura, 0, 200, 220)
                } else {
                    itemSedativeSprite[y * TEX_SIZE + x] = 0
                }

                // Battery: Cylindrical cell with gold terminal
                val batBody = (x in 23..41) && (y in 18..54)
                val batTip = (x in 29..35) && (y in 12..17)
                val batBand = (x in 23..41) && (y in 24..30)
                if (batTip || batBand) {
                    itemBatterySprite[y * TEX_SIZE + x] = color(255, 255, 179, 0)
                } else if (batBody) {
                    itemBatterySprite[y * TEX_SIZE + x] = color(255, 50, 50, 55)
                } else if (dist < 360f) {
                    val aura = ((360f - dist) / 360f * 70f).toInt().coerceIn(0, 255)
                    itemBatterySprite[y * TEX_SIZE + x] = color(aura, 255, 220, 0)
                } else {
                    itemBatterySprite[y * TEX_SIZE + x] = 0
                }

                // Talisman: Occult carved bone charm with pulsating purple eye
                val talisDiamond = (abs(dx) + abs(dy)) < 20f
                val talisEye = dist < 36f
                if (talisEye) {
                    itemTalismanSprite[y * TEX_SIZE + x] = color(255, 255, 40, 40)
                } else if (talisDiamond) {
                    itemTalismanSprite[y * TEX_SIZE + x] = color(255, 186, 104, 200)
                } else if (dist < 450f) {
                    val aura = ((450f - dist) / 450f * 100f).toInt().coerceIn(0, 255)
                    itemTalismanSprite[y * TEX_SIZE + x] = color(aura, 200, 40, 220)
                } else {
                    itemTalismanSprite[y * TEX_SIZE + x] = 0
                }

                // Note: Creepy weathered folded parchment note
                val notePaper = (x in 18..46) && (y in 16..50)
                val noteLines = notePaper && (y in 24..44 && y % 5 == 0 && x in 22..42)
                if (noteLines) {
                    itemNoteSprite[y * TEX_SIZE + x] = color(255, 90, 30, 30) // Blood ink lines
                } else if (notePaper) {
                    itemNoteSprite[y * TEX_SIZE + x] = color(255, 238, 225, 195) // Aged parchment
                } else if (dist < 380f) {
                    val aura = ((380f - dist) / 380f * 60f).toInt().coerceIn(0, 255)
                    itemNoteSprite[y * TEX_SIZE + x] = color(aura, 255, 240, 200)
                } else {
                    itemNoteSprite[y * TEX_SIZE + x] = 0
                }
            }
        }
    }

    fun getItemSprite(type: ItemType): IntArray {
        return when (type) {
            ItemType.KEY -> itemKeySprite
            ItemType.SEDATIVE -> itemSedativeSprite
            ItemType.BATTERY -> itemBatterySprite
            ItemType.TALISMAN -> itemTalismanSprite
            ItemType.NOTE -> itemNoteSprite
        }
    }

    fun getWallTexture(wallType: Int): IntArray {
        return when (wallType) {
            1 -> wallpaper       // Living Room
            2 -> woodPanels      // Bedroom
            3 -> kitchenTiles    // Kitchen
            4 -> bathroomTiles   // Bathroom
            5 -> basementStone   // Basement
            6 -> doorWood        // Interior Door
            7 -> exitDoor        // Front Exit Door
            else -> wallpaper
        }
    }
}
