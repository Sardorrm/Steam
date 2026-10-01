package com.example.donttrustthehouse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.donttrustthehouse.core.GameManager
import com.example.donttrustthehouse.core.GameUIState
import java.util.Locale

@Composable
fun DirectorDebugDialog(
    gameManager: GameManager,
    uiState: GameUIState,
    onDismiss: () -> Unit
) {
    val director = gameManager.houseDirector
    val behavior = director.behavior
    val monster = gameManager.monsterAI.data

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF18151E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("director_debug_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "House Director",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "HOUSE DIRECTOR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "AI Learning & Telemetry Monitor",
                                fontSize = 11.sp,
                                color = Color(0xFFA5A0B0)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_debug_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // F2: House Learning Card
                SectionCard(title = "THE HOUSE LEARNS HOW YOU PLAY") {
                    DebugRow(
                        "Detected Play Style",
                        behavior.playStyle.uppercase(Locale.ROOT),
                        when (behavior.playStyle) {
                            "sneaky" -> Color(0xFF43A047)
                            "aggressive" -> Color(0xFFE53935)
                            else -> Color(0xFF4DD0E1)
                        }
                    )
                    DebugRow("Times Caught", "${behavior.timesCaught} / 3")
                    DebugRow(
                        "Monster Aggression",
                        "${(director.getMonsterAggression() * 100).toInt()}%"
                    )
                    DebugRow(
                        "Predicted Next Hideout",
                        director.predictHideLocation(gameManager.houseManager)?.let { "Pos (${it.x.toInt()}, ${it.y.toInt()})" } ?: "None yet"
                    )
                    DebugRow("Hide Spots Used", "${behavior.hideSpotsUsed.size} distinct spots")
                    behavior.hideSpotsUsed.forEach { (spotId, count) ->
                        Text(
                            text = "  • $spotId: used $count time(s)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFCFD8DC),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    DebugRow("Favorite Rooms", behavior.favoriteRooms.keys.joinToString(", ").ifEmpty { "None" })
                }

                Spacer(modifier = Modifier.height(10.dp))

                // F4: Monster State
                SectionCard(title = "MONSTER ENTITY STATE") {
                    DebugRow("State", monster.state.name, if (monster.state.name == "HUNTING") Color.Red else Color.Yellow)
                    DebugRow("Intelligence Level", String.format(Locale.ROOT, "%.2f / 1.00", monster.intelligence))
                    DebugRow("Hunt Speed", String.format(Locale.ROOT, "%.1f units/s", monster.huntSpeed))
                    DebugRow("Position", "(${monster.position.x.toInt()}, ${monster.position.y.toInt()})")
                    DebugRow("Target Coordinate", "(${monster.targetPosition.x.toInt()}, ${monster.targetPosition.y.toInt()})")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Psychological & Sanity Telemetry
                SectionCard(title = "PSYCHOLOGICAL & SANITY TELEMETRY") {
                    val sanityPercent = (uiState.sanity * 100).toInt()
                    val sanityStatus = when {
                        uiState.sanity >= 0.75f -> "STABLE"
                        uiState.sanity >= 0.50f -> "UNSETTLED"
                        uiState.sanity >= 0.25f -> "PARANOID"
                        else -> "PSYCHOSIS / CRITICAL"
                    }
                    val statusColor = when {
                        uiState.sanity >= 0.75f -> Color(0xFF00E5FF)
                        uiState.sanity >= 0.50f -> Color(0xFFFFD54F)
                        uiState.sanity >= 0.25f -> Color(0xFFFF7043)
                        else -> Color(0xFFFF1744)
                    }
                    DebugRow("Sanity Level", "$sanityPercent% ($sanityStatus)", statusColor)
                    val drainDescription = when {
                        uiState.sanityDrainRate > 0.02f -> String.format(Locale.ROOT, "+%.2f/s (RAPID TERROR DRAIN)", uiState.sanityDrainRate)
                        uiState.sanityDrainRate > 0f -> String.format(Locale.ROOT, "+%.2f/s (Subtle Drain)", uiState.sanityDrainRate)
                        uiState.sanityDrainRate < -0.01f -> String.format(Locale.ROOT, "%.2f/s (Calming / Sanctuary)", uiState.sanityDrainRate)
                        else -> "0.00/s (Equilibrium)"
                    }
                    DebugRow("Drain Trend", drainDescription, if (uiState.sanityDrainRate > 0) Color(0xFFFF5252) else Color(0xFF69F0AE))
                    DebugRow(
                        "Chromatic Aberration",
                        if (uiState.sanity < 0.85f) String.format(Locale.ROOT, "ACTIVE (Level %.1f)", (1f - uiState.sanity) * 4f) else "INACTIVE (Clear)"
                    )
                    DebugRow(
                        "Psychic Audio Flutter",
                        if (uiState.sanity < 0.65f) "STREAMING DISSOCIATIVE FREQUENCIES" else "DORMANT"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // F1: Game State
                SectionCard(title = "GAME STATE & PROGRESSION") {
                    DebugRow("Remaining Lives", "${uiState.lives} / 3")
                    DebugRow("Run Time", "${uiState.gameTimeSeconds.toInt()}s")
                    DebugRow("Objective", uiState.objective)
                    DebugRow("Current Room", uiState.currentRoomName)
                    DebugRow("Player Coordinates", String.format(Locale.ROOT, "X: %.1f, Y: %.1f", uiState.player.position.x, uiState.player.position.y))
                    DebugRow("Monster Proximity", String.format(Locale.ROOT, "%.1f%% Threat", uiState.dangerProximity * 100))
                    DebugRow("Crouch Status", if (uiState.player.isCrouching) "CROUCHING (Silent)" else "STANDING (Loud)")
                    DebugRow("Hiding Status", if (uiState.player.isHiding) "HIDDEN inside ${uiState.player.currentHideSpotId}" else "NOT HIDDEN")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // RoomState & Inventory Telemetry
                SectionCard(title = "ROOM ARCHITECTURE & INVENTORY") {
                    val roomState = uiState.currentRoomState
                    DebugRow("Active Room", roomState?.name ?: uiState.currentRoomName)
                    DebugRow("Lighting Status", if (roomState?.isLit == true) "LIT" else "PITCH BLACK", if (roomState?.isLit == true) Color(0xFFFFD54F) else Color(0xFF90A4AE))
                    DebugRow("Room Threat Level", String.format(Locale.ROOT, "%.1f / 1.0", roomState?.dangerLevel ?: 0f))
                    DebugRow("Discovered Rooms", "${uiState.roomStates.values.count { it.isDiscovered }} / ${uiState.roomStates.size}")
                    DebugRow("Inventory Count", "${uiState.inventory.size} items held")
                    if (uiState.inventory.isNotEmpty()) {
                        uiState.inventory.forEach { item ->
                            Text(
                                text = "  • ${item.name} (${item.type.name})",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF80D8FF),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "  (No items collected yet)",
                            fontSize = 11.sp,
                            color = Color(0xFF78909C),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF372C42))
                ) {
                    Text("Return to Game", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF221D2B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF8A80),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun DebugRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFA5A0B0)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
