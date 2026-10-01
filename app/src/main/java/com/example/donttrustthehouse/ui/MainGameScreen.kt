package com.example.donttrustthehouse.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donttrustthehouse.core.GameManager
import com.example.donttrustthehouse.engine3d.House3DView
import com.example.donttrustthehouse.model.GameItem
import com.example.donttrustthehouse.model.RoomState
import com.example.donttrustthehouse.ui.components.DirectorDebugDialog
import com.example.donttrustthehouse.ui.components.HouseCanvasView
import com.example.donttrustthehouse.ui.components.InventoryBar
import com.example.donttrustthehouse.ui.components.SanityMeter
import com.example.donttrustthehouse.ui.components.StatusNotificationBanner
import com.example.donttrustthehouse.ui.components.VirtualJoystick
import java.util.Locale

@Composable
fun MainGameScreen(
    gameManager: GameManager,
    modifier: Modifier = Modifier
) {
    val viewModel = remember(gameManager) { GameViewModel(gameManager) }
    MainGameScreen(viewModel = viewModel, modifier = modifier)
}

@Composable
fun MainGameScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameManager = viewModel.gameManager
    val uiState by viewModel.uiState.collectAsState()
    val playerPosition by viewModel.playerPosition.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val monsterProximity by viewModel.monsterProximity.collectAsState()
    val currentRoomState by viewModel.currentRoomState.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()

    var showDebugDialog by remember { mutableStateOf(false) }
    var is3DMode by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08060A))
    ) {
        // 1. Primary Render View (3D First-Person Raycaster or 2D Architectural Plan)
        if (is3DMode) {
            House3DView(
                modifier = Modifier.fillMaxSize(),
                raycastEngine = gameManager.raycastEngine,
                houseManager = gameManager.houseManager,
                playerState = uiState.player,
                monsterData = uiState.monster?.data,
                dangerProximity = uiState.dangerProximity,
                flashlightOn = uiState.flashlightOn,
                headBob = uiState.headBob,
                hasCrosshairTarget = uiState.nearestInteractivePrompt != null,
                sanity = uiState.sanity,
                gameTime = uiState.gameTimeSeconds
            )
        } else {
            HouseCanvasView(
                modifier = Modifier.fillMaxSize(),
                houseManager = gameManager.houseManager,
                playerState = uiState.player,
                monsterData = uiState.monster?.data,
                dangerProximity = uiState.dangerProximity,
                sanity = uiState.sanity
            )
        }

        // 2. Look Drag Area (Touch right screen half to rotate camera in 3D smoothly)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Sensitivity factor
                        val sensitivity = 0.0075f
                        viewModel.rotateCamera(dragAmount.x * sensitivity)
                    }
                }
        )

        // 3. Top HUD Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Header Row: Objective, Lives, Mode Toggle, Threat & Director Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lives & Objective
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) { index ->
                            val isAlive = index < uiState.lives
                            Icon(
                                imageVector = if (isAlive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isAlive) "Life $index" else "Lost Life $index",
                                tint = if (isAlive) Color(0xFFE53935) else Color(0x55E53935),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Room Tag with RoomState telemetry
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xAA201B2B)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isLit = currentRoomState?.isLit ?: true
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isLit) Color(0xFFFFD54F) else Color(0xFF78909C),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = (currentRoomState?.name ?: uiState.currentRoomName).uppercase(Locale.ROOT),
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val roomItemCount = currentRoomState?.items?.count { !it.isCollected } ?: 0
                                if (roomItemCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "• $roomItemCount ITEM${if (roomItemCount > 1) "S" else ""}",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.objective,
                        color = Color(0xFFECEFF1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Threat Sensor, Mode Toggle & Director AI button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Threat Level Indicator
                    ThreatMeter(dangerProximity = uiState.dangerProximity)

                    Spacer(modifier = Modifier.width(6.dp))

                    // 3D / 2D Camera Mode Toggle
                    IconButton(
                        onClick = { is3DMode = !is3DMode },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x882A2438), CircleShape)
                            .border(1.dp, Color(0x554DD0E1), CircleShape)
                            .testTag("view_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (is3DMode) Icons.Default.ViewInAr else Icons.Default.Map,
                            contentDescription = "Toggle 3D View",
                            tint = Color(0xFF4DD0E1),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Director Telemetry / Debug Button
                    IconButton(
                        onClick = { showDebugDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x882A2438), CircleShape)
                            .border(1.dp, Color(0x55E53935), CircleShape)
                            .testTag("director_debug_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "House Director Telemetry",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Dynamic Ambient Audio Mute / Unmute Button
                    IconButton(
                        onClick = { viewModel.toggleAudioMute() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x882A2438), CircleShape)
                            .border(
                                1.dp,
                                if (uiState.isAudioMuted) Color(0x5578909C) else Color(0x55CE93D8),
                                CircleShape
                            )
                            .testTag("audio_mute_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (uiState.isAudioMuted) "Unmute Ambient Sound" else "Mute Ambient Sound",
                            tint = if (uiState.isAudioMuted) Color(0xFF78909C) else Color(0xFFCE93D8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Sanity Meter Component (tracks mental stability & horror aberration)
            SanityMeter(
                sanity = uiState.sanity,
                drainRate = uiState.sanityDrainRate,
                dangerProximity = uiState.dangerProximity,
                activeHorrorEvent = uiState.activeHorrorTitle,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Stealth & Noise Level Meter
            NoiseMeter(
                noiseLevel = uiState.player.noiseLevel,
                isCrouching = uiState.player.isCrouching,
                isHiding = uiState.player.isHiding
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Inventory Bar (displays collected survival items)
            InventoryBar(
                inventory = inventory,
                selectedItem = selectedItem,
                onSelectItem = { viewModel.selectItem(it) },
                onUseItem = { viewModel.useItem(it) },
                onDropItem = { viewModel.dropItem(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Status Notification Banner (item pickups, unlocks, effects)
        StatusNotificationBanner(
            notification = uiState.statusNotification,
            onDismiss = { viewModel.dismissNotification() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 195.dp, start = 16.dp, end = 16.dp)
        )

        // Active Horror Event Banner (Smart scare notification)
        AnimatedVisibility(
            visible = uiState.activeHorrorTitle != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD3A0A0A)),
                modifier = Modifier
                    .border(1.dp, Color(0x88FF1744), RoundedCornerShape(10.dp))
                    .testTag("horror_event_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Horror Warning",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = uiState.activeHorrorTitle ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.activeHorrorDescription ?: "",
                            color = Color(0xFFFFCDD2),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Contextual Interaction Prompt (Center / Crosshair feedback)
        AnimatedVisibility(
            visible = uiState.nearestInteractivePrompt != null && !uiState.isCaughtOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 80.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xD81E1A29),
                modifier = Modifier
                    .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(16.dp))
                    .clickable { viewModel.triggerInteract() }
                    .testTag("interactive_prompt_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Interact prompt",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.nearestInteractivePrompt ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom Controls (Joystick on left, Actions on right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Virtual Joystick for 3D movement (Left stick)
            VirtualJoystick(
                size = 125.dp,
                onMove = { input -> viewModel.setMovementInput(input) }
            )

            // Right Action Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Flashlight Toggle Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (uiState.flashlightOn) "LIGHT" else "DARK",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.flashlightOn) Color(0xFFFFF59D) else Color(0xFF90A4AE)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    IconButton(
                        onClick = { viewModel.toggleFlashlight() },
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                if (uiState.flashlightOn) Color(0x88F57F17) else Color(0x882A2438),
                                CircleShape
                            )
                            .border(
                                2.dp,
                                if (uiState.flashlightOn) Color(0xFFFFEE58) else Color(0x55FFFFFF),
                                CircleShape
                            )
                            .testTag("flashlight_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.flashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Toggle Flashlight",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Crouch Button (Stealth Toggle)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (uiState.player.isCrouching) "STEALTH" else "STAND",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.player.isCrouching) Color(0xFF81C784) else Color(0xFFB0BEC5)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    IconButton(
                        onClick = { viewModel.toggleCrouch() },
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                if (uiState.player.isCrouching) Color(0xBB2E7D32) else Color(0x882A2438),
                                CircleShape
                            )
                            .border(
                                2.dp,
                                if (uiState.player.isCrouching) Color(0xFF4CAF50) else Color(0x55FFFFFF),
                                CircleShape
                            )
                            .testTag("crouch_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.player.isCrouching) Icons.Default.Hearing else Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = "Toggle Crouch Stealth",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Primary Interact Button
                val hasTarget = uiState.nearestInteractivePrompt != null
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (uiState.player.isHiding) "LEAVE" else "ACTION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasTarget) Color(0xFFFFD54F) else Color(0xFFB0BEC5)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    IconButton(
                        onClick = { viewModel.triggerInteract() },
                        modifier = Modifier
                            .size(62.dp)
                            .background(
                                if (hasTarget) Color(0xCCE65100) else Color(0x882A2438),
                                CircleShape
                            )
                            .border(
                                2.dp,
                                if (hasTarget) Color(0xFFFFB74D) else Color(0x55FFFFFF),
                                CircleShape
                            )
                            .testTag("interact_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.nearestInteractiveType == "door" || uiState.nearestInteractiveType == "exit") {
                                Icons.Default.DoorFront
                            } else {
                                Icons.Default.TouchApp
                            },
                            contentDescription = "Interact",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Caught Screen Flash Overlay
        AnimatedVisibility(
            visible = uiState.isCaughtOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE420000))
                    .testTag("caught_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Caught",
                        tint = Color(0xFFFF1744),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CAUGHT BY THE ENTITY!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Remaining Lives: ${uiState.lives}",
                        color = Color(0xFFFFCDD2),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The house is learning your patterns...",
                        color = Color(0xFFEF9A9A),
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        // Game Over / Defeat Dialog
        if (uiState.isGameOver && !uiState.isVictory) {
            GameOverDialog(
                gameManager = gameManager,
                onRestart = { viewModel.startGame() }
            )
        }

        // Victory / Escaped Dialog
        if (uiState.isGameOver && uiState.isVictory) {
            VictoryDialog(
                gameManager = gameManager,
                timeSeconds = uiState.gameTimeSeconds,
                remainingLives = uiState.lives,
                onPlayAgain = { viewModel.startGame() }
            )
        }

        // Director Debug Dialog
        if (showDebugDialog) {
            DirectorDebugDialog(
                gameManager = gameManager,
                uiState = uiState,
                onDismiss = { showDebugDialog = false }
            )
        }
    }
}

@Composable
private fun ThreatMeter(dangerProximity: Float) {
    val (threatLabel, threatColor) = when {
        dangerProximity > 0.8f -> "HUNTED!" to Color(0xFFFF1744)
        dangerProximity > 0.5f -> "DANGER" to Color(0xFFFF9100)
        dangerProximity > 0.25f -> "UNEASY" to Color(0xFFFFEA00)
        else -> "SAFE" to Color(0xFF00E676)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0x99201B2B),
        modifier = Modifier.border(1.dp, threatColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(threatColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = threatLabel,
                color = threatColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun NoiseMeter(
    noiseLevel: Float,
    isCrouching: Boolean,
    isHiding: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x7716131D))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isHiding || noiseLevel < 0.1f) Icons.AutoMirrored.Filled.VolumeDown else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Noise Indicator",
            tint = if (isHiding) Color(0xFF4CAF50) else if (isCrouching) Color(0xFF81C784) else Color(0xFFFF7043),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isHiding) "SILENT (HIDDEN)" else if (isCrouching) "STEALTH (QUIET)" else "NORMAL (AUDIBLE)",
            fontSize = 9.sp,
            color = Color(0xFFB0BEC5),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { noiseLevel.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (isCrouching) Color(0xFF4CAF50) else Color(0xFFE53935),
            trackColor = Color(0x33FFFFFF)
        )
    }
}

@Composable
private fun GameOverDialog(
    gameManager: GameManager,
    onRestart: () -> Unit
) {
    val director = gameManager.houseDirector
    val behavior = director.behavior

    Dialog(onDismissRequest = { /* Require button */ }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1F1214),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "YOU DIED",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF1744),
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The entity claimed you 3 times. The house learned all your hiding spots and movement patterns.",
                    fontSize = 13.sp,
                    color = Color(0xFFFFCDD2),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C181C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "HOUSE DIRECTOR POST-MORTEM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8A80)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Analyzed Play Style: ${behavior.playStyle.uppercase(Locale.ROOT)}",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "• Hideouts Discovered: ${behavior.hideSpotsUsed.size}",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "• Final Monster Aggression: ${(director.getMonsterAggression() * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restart_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VictoryDialog(
    gameManager: GameManager,
    timeSeconds: Float,
    remainingLives: Int,
    onPlayAgain: () -> Unit
) {
    val director = gameManager.houseDirector
    val behavior = director.behavior

    Dialog(onDismissRequest = { /* Require button */ }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF101B15),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("victory_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ESCAPED!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E676),
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You reached the front exit door and broke free from the psychological nightmare.",
                    fontSize = 13.sp,
                    color = Color(0xFFC8E6C9),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF182A20)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ESCAPE RUN METRICS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5D6A7)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Survival Time: ${timeSeconds.toInt()} seconds",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "• Preserved Lives: $remainingLives / 3",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "• Play Style: ${behavior.playStyle.uppercase(Locale.ROOT)}",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Text(
                            text = "• Stealth Strategy: ${behavior.hideSpotsUsed.size} hideouts used",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_again_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Play Again", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start New Run", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
