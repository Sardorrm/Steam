package com.example.donttrustthehouse.core

import com.example.donttrustthehouse.audio.HorrorAudioEngine
import com.example.donttrustthehouse.engine3d.RaycastEngine
import com.example.donttrustthehouse.model.Door
import com.example.donttrustthehouse.model.GameItem
import com.example.donttrustthehouse.model.HideSpot
import com.example.donttrustthehouse.model.HorrorEventType
import com.example.donttrustthehouse.model.ItemType
import com.example.donttrustthehouse.model.LightSwitch
import com.example.donttrustthehouse.model.MonsterState
import com.example.donttrustthehouse.model.PlayerState
import com.example.donttrustthehouse.model.RoomState
import com.example.donttrustthehouse.model.Vector2D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class GameUIState(
    val isPlaying: Boolean = false,
    val playerAlive: Boolean = true,
    val lives: Int = 3,
    val objective: String = "Find clues and the exit key to escape the house",
    val objectiveCompleted: Boolean = false,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val isCaughtOverlay: Boolean = false,
    val gameTimeSeconds: Float = 0f,
    val player: PlayerState = PlayerState(position = Vector2D(15.5f, 15.5f)),
    val monster: MonsterAI? = null,
    val nearestInteractivePrompt: String? = null,
    val nearestInteractiveType: String? = null,
    val dangerProximity: Float = 0f, // 0f to 1f
    val currentRoomName: String = "Living Room",
    val activeHorrorTitle: String? = null,
    val activeHorrorDescription: String? = null,
    val flashlightOn: Boolean = true,
    val headBob: Float = 0f,
    val isAudioMuted: Boolean = false,
    val sanity: Float = 1.0f, // 1.0f (100% sane) to 0.0f (insane)
    val sanityDrainRate: Float = 0f,
    // Inventory and Room States
    val inventory: List<GameItem> = emptyList(),
    val currentRoomState: RoomState? = null,
    val roomStates: Map<String, RoomState> = emptyMap(),
    val selectedItem: GameItem? = null,
    val statusNotification: String? = null
)

class GameManager(
    val audioEngine: HorrorAudioEngine
) {
    val houseManager = HouseManager()
    val houseDirector = HouseDirector()
    val eventSystem = EventSystem(audioEngine, houseDirector)
    val monsterAI = MonsterAI(houseManager, houseDirector)
    val raycastEngine = RaycastEngine(screenWidth = 320, screenHeight = 200)

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var gameLoopJob: Job? = null

    private val _uiState = MutableStateFlow(GameUIState())
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

    private val inventoryList = mutableListOf<GameItem>()
    private var playerMovementInput = Vector2D.ZERO
    private var footstepTimer = 0f
    private var heartbeatTimer = 0f
    private var monsterAwakeTimer = 5.0f // 5s grace period
    private var walkBobCycle = 0f
    private var playerSanity = 1.0f
    private var insanityAudioCooldown = 0f
    private var notificationJob: Job? = null

    fun startGame() {
        gameLoopJob?.cancel()
        houseManager.randomizeLayout()
        houseDirector.resetRun()
        inventoryList.clear()

        playerSanity = 1.0f
        insanityAudioCooldown = 0f

        val startPos = Vector2D(15.5f, 15.5f)
        val monsterStartPos = Vector2D(26.0f, 15.5f)
        val initialRoomStates = houseManager.getAllRoomStates(startPos, monsterStartPos)
        val initialRoomState = houseManager.getRoomState("living_room", startPos, monsterStartPos)

        _uiState.value = GameUIState(
            isPlaying = true,
            playerAlive = true,
            lives = 3,
            objective = "Search the house for clues & the exit key to escape",
            objectiveCompleted = false,
            isGameOver = false,
            isVictory = false,
            player = PlayerState(position = startPos, facingAngle = 0f),
            monster = monsterAI,
            flashlightOn = true,
            sanity = 1.0f,
            sanityDrainRate = 0f,
            inventory = emptyList(),
            currentRoomState = initialRoomState,
            roomStates = initialRoomStates,
            currentRoomName = "Living Room"
        )

        monsterAwakeTimer = 5.0f
        monsterAI.resetHunt(monsterStartPos)

        audioEngine.startAmbient()
        audioEngine.updateAmbient("living_room", 0f, isHiding = false, isFlashlightOn = true, sanity = 1.0f)

        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = scope.launch {
            var lastTime = System.nanoTime()

            while (isActive) {
                val now = System.nanoTime()
                val delta = ((now - lastTime) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
                lastTime = now

                if (_uiState.value.isPlaying && !_uiState.value.isGameOver && !_uiState.value.isCaughtOverlay) {
                    updateGame(delta)
                }

                delay(16) // ~60fps
            }
        }
    }

    private fun updateGame(delta: Float) {
        val current = _uiState.value
        val gameTime = current.gameTimeSeconds + delta

        // Monster wake up delay (5s grace period)
        if (monsterAwakeTimer > 0f) {
            monsterAwakeTimer -= delta
            if (monsterAwakeTimer <= 0f) {
                monsterAI.startHunting()
                eventSystem.triggerEvent(HorrorEventType.MONSTER_AWAKENS)
            }
        }

        // Update player movement (Relative to 3D Camera Facing Angle!)
        var player = current.player
        var headBob = current.headBob

        if (!player.isHiding) {
            val speed = if (player.isCrouching) 2.2f else 4.5f

            // Forward/Back is along facing angle; Left/Right strafes perpendicular
            val forward = -playerMovementInput.y // joystick up is negative Y
            val strafe = playerMovementInput.x

            if (abs(forward) > 0.05f || abs(strafe) > 0.05f) {
                val cosA = cos(player.facingAngle)
                val sinA = sin(player.facingAngle)

                val moveX = (cosA * forward - sinA * strafe) * speed * delta
                val moveY = (sinA * forward + cosA * strafe) * speed * delta

                // Sliding collision check with 3D grid
                var nextX = player.position.x
                var nextY = player.position.y

                val margin = 0.25f
                val canMoveX = raycastEngine.isWalkable(nextX + moveX + (if (moveX > 0) margin else -margin), nextY)
                if (canMoveX) {
                    nextX += moveX
                }

                val canMoveY = raycastEngine.isWalkable(nextX, nextY + moveY + (if (moveY > 0) margin else -margin))
                if (canMoveY) {
                    nextY += moveY
                }

                player = player.copy(
                    position = Vector2D(nextX, nextY),
                    velocity = Vector2D(moveX / delta, moveY / delta)
                )

                // Head bobbing
                walkBobCycle += delta * (if (player.isCrouching) 8f else 14f)
                headBob = sin(walkBobCycle) * (if (player.isCrouching) 1.5f else 3.5f)

                // Track movement in HouseDirector
                houseDirector.recordMovement(player.isCrouching, delta)

                // Footstep sound & monster hearing
                footstepTimer += delta
                val interval = if (player.isCrouching) 0.65f else 0.35f
                if (footstepTimer >= interval) {
                    footstepTimer = 0f
                    val isLoud = !player.isCrouching
                    audioEngine.playFootstep(isLoud)
                    monsterAI.hearSound(player.position, isLoud)
                    player = player.copy(noiseLevel = if (isLoud) 1.0f else 0.35f)
                }
            } else {
                player = player.copy(velocity = Vector2D.ZERO, noiseLevel = 0f)
                walkBobCycle = 0f
                headBob = 0f
            }
        } else {
            player = player.copy(velocity = Vector2D.ZERO, noiseLevel = 0f)
            headBob = 0f
        }

        // Decay noise level
        if (player.noiseLevel > 0f) {
            player = player.copy(noiseLevel = (player.noiseLevel - delta * 2f).coerceAtLeast(0f))
        }

        // Update Monster AI
        monsterAI.update(
            delta = delta,
            player = player,
            onCatchPlayer = { handlePlayerCaught() },
            onSpotPlayer = { audioEngine.playMonsterSpot() }
        )

        // Danger proximity calculation (distance to monster)
        val distToMonster = player.position.distanceTo(monsterAI.data.position)
        val dangerProximity = (1f - (distToMonster / 18f)).coerceIn(0f, 1f)

        // Heartbeat audio when monster is near
        heartbeatTimer += delta
        val heartbeatInterval = (1.5f - dangerProximity * 1.1f).coerceIn(0.35f, 1.5f)
        if (dangerProximity > 0.25f && heartbeatTimer >= heartbeatInterval) {
            heartbeatTimer = 0f
            audioEngine.playHeartbeat(dangerProximity)
        }

        // Current room & lighting status
        val currentRoomObj = houseManager.getRoomAt(player.position)
        val currentRoom = currentRoomObj?.name ?: "Corridor"
        val currentRoomId = currentRoomObj?.id ?: "living_room"
        val isRoomLit = currentRoomObj?.let { houseManager.isRoomLit(it.id) } ?: true

        // Room states computation
        val allRoomStates = houseManager.getAllRoomStates(player.position, monsterAI.data.position)
        val currentRoomState = houseManager.getRoomState(currentRoomId, player.position, monsterAI.data.position)

        // --- Dynamic Sanity Depletion Engine ---
        var monsterDrain = 0f
        if (dangerProximity > 0.12f) {
            val huntMultiplier = if (monsterAI.data.state == MonsterState.HUNTING) 1.8f else 1.0f
            monsterDrain = (dangerProximity * dangerProximity * 0.14f) * huntMultiplier
        }

        var gazeDrain = 0f
        if (dangerProximity > 0.20f) {
            val dirToMonster = monsterAI.data.position - player.position
            val angleToMonster = atan2(dirToMonster.y, dirToMonster.x)
            var angleDiff = abs(player.facingAngle - angleToMonster)
            while (angleDiff > Math.PI) angleDiff -= (2 * Math.PI).toFloat()
            angleDiff = abs(angleDiff)
            if (angleDiff < 0.65f) {
                gazeDrain = dangerProximity * 0.09f
            }
        }

        val darknessDrain = when {
            !isRoomLit && !current.flashlightOn -> 0.035f
            !isRoomLit && current.flashlightOn -> 0.008f
            else -> 0f
        }

        val eventDrain = if (eventSystem.currentActiveEvent != null) 0.05f else 0f
        val totalDrain = monsterDrain + gazeDrain + darknessDrain + eventDrain
        var recoveryRate = 0f
        if (dangerProximity < 0.15f && eventSystem.currentActiveEvent == null) {
            if (player.isHiding) {
                recoveryRate = 0.08f
            } else if (isRoomLit && current.flashlightOn) {
                recoveryRate = 0.035f
            } else if (current.flashlightOn) {
                recoveryRate = 0.015f
            }
        }

        val netDrainRate = totalDrain - recoveryRate
        playerSanity = (playerSanity - netDrainRate * delta).coerceIn(0.0f, 1.0f)

        // Low sanity auditory hallucinations
        insanityAudioCooldown -= delta
        if (playerSanity < 0.25f && insanityAudioCooldown <= 0f) {
            insanityAudioCooldown = 4.0f + Random.nextFloat() * 4.0f
            if (Random.nextBoolean()) {
                audioEngine.playWhisper()
            } else {
                audioEngine.playInsanitySting()
            }
        }

        // Check crosshair target
        val interactiveInfo = findCrosshairInteractive(player)

        // Dynamic horror ambient soundscape update
        audioEngine.updateAmbient(
            roomName = currentRoom,
            dangerProximity = dangerProximity,
            isHiding = player.isHiding,
            isFlashlightOn = current.flashlightOn,
            sanity = playerSanity
        )

        // Periodic smart horror event
        if (Random.nextFloat() < 0.0008f) {
            eventSystem.triggerSmartEvent(
                onTeleportScare = { offset ->
                    val tpPos = player.position + offset
                    if (raycastEngine.isWalkable(tpPos.x, tpPos.y)) {
                        player = player.copy(position = tpPos)
                    }
                }
            )
        }
        eventSystem.checkActiveEventTimeout()

        _uiState.value = current.copy(
            gameTimeSeconds = gameTime,
            player = player,
            dangerProximity = dangerProximity,
            currentRoomName = currentRoom,
            nearestInteractivePrompt = interactiveInfo?.first,
            nearestInteractiveType = interactiveInfo?.second,
            activeHorrorTitle = eventSystem.currentActiveEvent?.title,
            activeHorrorDescription = eventSystem.currentActiveEvent?.description,
            headBob = headBob,
            sanity = playerSanity,
            sanityDrainRate = netDrainRate,
            inventory = inventoryList.toList(),
            currentRoomState = currentRoomState,
            roomStates = allRoomStates
        )
    }

    fun setMovementInput(input: Vector2D) {
        playerMovementInput = input
    }

    fun setPlayerPosition(newPos: Vector2D) {
        val curPlayer = _uiState.value.player
        _uiState.value = _uiState.value.copy(
            player = curPlayer.copy(position = newPos)
        )
    }

    fun rotateCamera(deltaAngle: Float) {
        val player = _uiState.value.player
        var newAngle = player.facingAngle + deltaAngle
        while (newAngle < 0) newAngle += (2 * Math.PI).toFloat()
        while (newAngle >= 2 * Math.PI) newAngle -= (2 * Math.PI).toFloat()
        _uiState.value = _uiState.value.copy(
            player = player.copy(facingAngle = newAngle)
        )
    }

    fun toggleFlashlight() {
        val current = _uiState.value.flashlightOn
        _uiState.value = _uiState.value.copy(flashlightOn = !current)
    }

    fun toggleCrouch() {
        val player = _uiState.value.player
        _uiState.value = _uiState.value.copy(
            player = player.copy(isCrouching = !player.isCrouching)
        )
    }

    fun triggerInteract() {
        val player = _uiState.value.player

        // If player is currently hiding, exit hiding
        if (player.isHiding) {
            player.currentHideSpotId?.let { spotId ->
                houseManager.setHideSpotOccupied(spotId, false)
            }
            _uiState.value = _uiState.value.copy(
                player = player.copy(isHiding = false, currentHideSpotId = null)
            )
            return
        }

        // 1. Check nearby uncollected items (Item Pickup)
        val nearestItem = houseManager.items
            .filter { !it.isCollected }
            .minByOrNull { it.position.distanceTo(player.position) }
        if (nearestItem != null && nearestItem.position.distanceTo(player.position) < 2.4f) {
            val collected = houseManager.collectItem(nearestItem.id)
            if (collected != null) {
                inventoryList.add(collected)
                audioEngine.playItemPickup()
                val snippet = if (collected.type == ItemType.NOTE) ": \"${collected.description}\"" else ""
                showNotification("Picked up ${collected.name}$snippet")
                if (collected.id == "item_key_exit") {
                    _uiState.value = _uiState.value.copy(objective = "Head to the Front Exit in the Living Room and escape!")
                } else if (collected.id == "item_key_basement") {
                    _uiState.value = _uiState.value.copy(objective = "Unlock the Basement Door in the Living Room")
                }
                return
            }
        }

        // 2. Check if aiming directly at a door or exit
        val hit = raycastEngine.checkCrosshairTarget(player.position.x, player.position.y, player.facingAngle)
        if (hit != null) {
            if (hit.wallType == 7) { // Exit door
                handleExitDoorInteraction()
                return
            } else if (hit.wallType == 6 || hit.wallType == 8) { // Interior door
                val door = houseManager.doors.firstOrNull {
                    it.position.x.toInt() == hit.mapX && it.position.y.toInt() == hit.mapY
                }
                if (door != null) {
                    handleDoorInteraction(door)
                    return
                }
            }
        }

        // 3. Check nearby hide spots (within 2.5 units)
        val nearestSpot = houseManager.hideSpots.minByOrNull { it.position.distanceTo(player.position) }
        if (nearestSpot != null && nearestSpot.position.distanceTo(player.position) < 2.5f) {
            houseManager.setHideSpotOccupied(nearestSpot.id, true)
            houseDirector.recordHide(nearestSpot.id, nearestSpot.roomName)
            _uiState.value = _uiState.value.copy(
                player = player.copy(
                    isHiding = true,
                    isCrouching = true,
                    currentHideSpotId = nearestSpot.id,
                    position = nearestSpot.position
                )
            )
            return
        }

        // 4. Check nearby doors (within 2.5 units)
        val nearestDoor = houseManager.doors.minByOrNull { it.position.distanceTo(player.position) }
        if (nearestDoor != null && nearestDoor.position.distanceTo(player.position) < 2.5f) {
            handleDoorInteraction(nearestDoor)
            return
        }

        // 5. Check nearby light switches
        val nearestSwitch = houseManager.lightSwitches.minByOrNull { it.position.distanceTo(player.position) }
        if (nearestSwitch != null && nearestSwitch.position.distanceTo(player.position) < 2.5f) {
            houseManager.toggleLight(nearestSwitch.id)
            audioEngine.playStaticScare()
            return
        }
    }

    private fun handleExitDoorInteraction() {
        val hasExitKey = inventoryList.any { it.id == "item_key_exit" || it.targetDoorId == "door_exit" }
        if (hasExitKey) {
            audioEngine.playDoorUnlock()
            houseDirector.recordEscapeAttempt()
            escapeHouse()
        } else {
            audioEngine.playStaticScare()
            showNotification("Front Exit is locked! The Heavy Iron Key is locked in the Basement!")
        }
    }

    private fun handleDoorInteraction(door: Door) {
        if (door.isExit) {
            handleExitDoorInteraction()
            return
        }

        if (door.isLocked) {
            val matchingKey = inventoryList.firstOrNull { it.targetDoorId == door.id }
            if (matchingKey != null) {
                houseManager.unlockDoor(door.id)
                audioEngine.playDoorUnlock()
                raycastEngine.setDoorOpen(door.position.x.toInt(), door.position.y.toInt(), true)
                showNotification("Unlocked ${door.name} with ${matchingKey.name}!")
            } else {
                audioEngine.playStaticScare()
                showNotification("${door.name} is locked! Search the house for the key.")
            }
            return
        }

        val toggled = houseManager.toggleDoor(door.id)
        if (toggled != null) {
            raycastEngine.setDoorOpen(door.position.x.toInt(), door.position.y.toInt(), toggled.isOpen)
            audioEngine.playDoorOpen()
        }
    }

    fun useInventoryItem(item: GameItem) {
        when (item.type) {
            ItemType.SEDATIVE -> {
                inventoryList.remove(item)
                playerSanity = (playerSanity + item.value).coerceIn(0f, 1.0f)
                audioEngine.playItemUse()
                showNotification("Calmed nerves with ${item.name} (+${(item.value * 100).toInt()}% Sanity)")
            }
            ItemType.BATTERY -> {
                inventoryList.remove(item)
                _uiState.value = _uiState.value.copy(flashlightOn = true)
                audioEngine.playItemUse()
                showNotification("Fresh battery loaded! Flashlight at maximum brightness.")
            }
            ItemType.TALISMAN -> {
                inventoryList.remove(item)
                monsterAI.resetHunt(Vector2D(15.5f, 26.0f))
                audioEngine.playTalismanWard()
                showNotification("TALISMAN REVEALED: Holy wards banished the entity back to the basement!")
            }
            ItemType.KEY -> {
                val targetDoor = houseManager.doors.firstOrNull { it.id == item.targetDoorId }
                if (targetDoor != null && targetDoor.position.distanceTo(_uiState.value.player.position) < 3.2f) {
                    if (targetDoor.isExit) {
                        inventoryList.remove(item)
                        audioEngine.playDoorUnlock()
                        escapeHouse()
                    } else {
                        houseManager.unlockDoor(targetDoor.id)
                        audioEngine.playDoorUnlock()
                        raycastEngine.setDoorOpen(targetDoor.position.x.toInt(), targetDoor.position.y.toInt(), true)
                        showNotification("Unlocked ${targetDoor.name}!")
                    }
                } else {
                    showNotification("Equipped ${item.name}. Stand next to ${targetDoor?.name ?: "the door"} to unlock it.")
                }
            }
            ItemType.NOTE -> {
                _uiState.value = _uiState.value.copy(selectedItem = item)
            }
        }
        _uiState.value = _uiState.value.copy(inventory = inventoryList.toList())
    }

    fun dropInventoryItem(item: GameItem) {
        if (inventoryList.remove(item)) {
            val playerPos = _uiState.value.player.position
            val dropped = item.copy(isCollected = false, position = playerPos)
            houseManager.items.add(dropped)
            showNotification("Dropped ${item.name}")
            _uiState.value = _uiState.value.copy(inventory = inventoryList.toList())
        }
    }

    fun selectItem(item: GameItem?) {
        _uiState.value = _uiState.value.copy(selectedItem = item)
    }

    fun showNotification(message: String) {
        notificationJob?.cancel()
        _uiState.value = _uiState.value.copy(statusNotification = message)
        notificationJob = scope.launch {
            delay(3500)
            if (_uiState.value.statusNotification == message) {
                _uiState.value = _uiState.value.copy(statusNotification = null)
            }
        }
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(statusNotification = null)
    }

    private fun findCrosshairInteractive(player: PlayerState): Pair<String, String>? {
        if (player.isHiding) {
            return Pair("Leave Hiding Spot", "hidespot")
        }

        // 1. Check nearby items
        val nearestItem = houseManager.items
            .filter { !it.isCollected }
            .minByOrNull { it.position.distanceTo(player.position) }
        if (nearestItem != null && nearestItem.position.distanceTo(player.position) < 2.3f) {
            return Pair("Pick up ${nearestItem.name}", "item")
        }

        // 2. Direct crosshair raycast hit
        val hit = raycastEngine.checkCrosshairTarget(player.position.x, player.position.y, player.facingAngle)
        if (hit != null) {
            if (hit.wallType == 7) {
                val hasExitKey = inventoryList.any { it.id == "item_key_exit" || it.targetDoorId == "door_exit" }
                return if (hasExitKey) {
                    Pair("UNLOCK FRONT EXIT WITH KEY", "exit")
                } else {
                    Pair("Front Exit [LOCKED - Heavy Iron Key Needed]", "exit_locked")
                }
            }
            if (hit.wallType == 6) {
                val door = houseManager.doors.firstOrNull { it.position.x.toInt() == hit.mapX && it.position.y.toInt() == hit.mapY }
                return if (door?.isLocked == true) {
                    val hasKey = inventoryList.any { it.targetDoorId == door.id }
                    if (hasKey) Pair("Unlock ${door.name} with Key", "door") else Pair("${door.name} [LOCKED]", "door_locked")
                } else {
                    Pair("Open Door", "door")
                }
            }
            if (hit.wallType == 8) {
                return Pair("Close Door", "door")
            }
        }

        // 3. Proximity checks
        val nearestSpot = houseManager.hideSpots.minByOrNull { it.position.distanceTo(player.position) }
        if (nearestSpot != null && nearestSpot.position.distanceTo(player.position) < 2.4f) {
            return Pair("Hide in ${nearestSpot.name}", "hidespot")
        }

        val nearestDoor = houseManager.doors.minByOrNull { it.position.distanceTo(player.position) }
        if (nearestDoor != null && nearestDoor.position.distanceTo(player.position) < 2.4f) {
            return if (nearestDoor.isExit) {
                val hasExitKey = inventoryList.any { it.id == "item_key_exit" || it.targetDoorId == "door_exit" }
                if (hasExitKey) Pair("UNLOCK FRONT EXIT WITH KEY", "exit") else Pair("Front Exit [LOCKED - Key Needed]", "exit_locked")
            } else if (nearestDoor.isLocked) {
                val hasKey = inventoryList.any { it.targetDoorId == nearestDoor.id }
                if (hasKey) Pair("Unlock ${nearestDoor.name} with Key", "door") else Pair("${nearestDoor.name} (Locked)", "door_locked")
            } else {
                val action = if (nearestDoor.isOpen) "Close" else "Open"
                Pair("$action ${nearestDoor.name}", "door")
            }
        }

        val nearestSwitch = houseManager.lightSwitches.minByOrNull { it.position.distanceTo(player.position) }
        if (nearestSwitch != null && nearestSwitch.position.distanceTo(player.position) < 2.4f) {
            val action = if (nearestSwitch.isOn) "Turn Off" else "Turn On"
            return Pair("$action ${nearestSwitch.name}", "switch")
        }

        return null
    }

    private fun handlePlayerCaught() {
        val current = _uiState.value
        val newLives = current.lives - 1

        audioEngine.playPlayerCaught()
        houseDirector.recordDeath(current.player.position)

        // Severe psychological shock upon encounter
        playerSanity = (playerSanity - 0.35f).coerceAtLeast(0.10f)

        if (newLives == 1) {
            monsterAI.increaseIntelligence()
        }

        if (newLives <= 0) {
            endGame(victory = false)
        } else {
            _uiState.value = current.copy(
                lives = newLives,
                isCaughtOverlay = true,
                sanity = playerSanity
            )

            scope.launch {
                delay(2000)
                _uiState.value = _uiState.value.copy(
                    isCaughtOverlay = false,
                    player = PlayerState(position = Vector2D(15.5f, 15.5f), facingAngle = 0f),
                    sanity = playerSanity
                )
                monsterAI.resetHunt(Vector2D(26.0f, 15.5f))
            }
        }
    }

    private fun escapeHouse() {
        endGame(victory = true)
    }

    private fun endGame(victory: Boolean) {
        val current = _uiState.value
        _uiState.value = current.copy(
            isPlaying = false,
            isGameOver = true,
            isVictory = victory,
            objectiveCompleted = victory
        )
        if (victory) {
            audioEngine.updateAmbient("outside", 0f, false, true)
        } else {
            audioEngine.updateAmbient("basement", 0.9f, false, false)
        }
    }

    fun toggleAudioMute() {
        val nextMute = !_uiState.value.isAudioMuted
        audioEngine.setMuted(nextMute)
        _uiState.value = _uiState.value.copy(isAudioMuted = nextMute)
    }

    fun pauseAudio() {
        audioEngine.pauseAmbient()
    }

    fun resumeAudio() {
        audioEngine.resumeAmbient()
    }

    fun stop() {
        gameLoopJob?.cancel()
        audioEngine.stopAmbient()
    }
}
