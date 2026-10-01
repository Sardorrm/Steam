package com.example.donttrustthehouse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class SanityTier(
    val label: String,
    val primaryColor: Color,
    val glowColor: Color
) {
    STABLE("STABLE", Color(0xFF00E5FF), Color(0x4400E5FF)),
    UNSETTLED("UNSETTLED", Color(0xFFFFD54F), Color(0x44FFD54F)),
    PARANOID("PARANOID", Color(0xFFFF7043), Color(0x44FF7043)),
    PSYCHOSIS("INSANE", Color(0xFFFF1744), Color(0x66FF1744))
}

/**
 * Sanity Meter HUD Component
 * Displays the psychological stability of the player.
 * Depletes in proximity to dynamic horror triggers (the entity, darkness, poltergeist events),
 * inducing visual aberrations and audio disorientation.
 */
@Composable
fun SanityMeter(
    sanity: Float,
    drainRate: Float,
    modifier: Modifier = Modifier,
    dangerProximity: Float = 0f,
    activeHorrorEvent: String? = null
) {
    val clampedSanity = sanity.coerceIn(0f, 1f)
    val percent = (clampedSanity * 100f).roundToInt()

    val tier = when {
        clampedSanity >= 0.75f -> SanityTier.STABLE
        clampedSanity >= 0.50f -> SanityTier.UNSETTLED
        clampedSanity >= 0.25f -> SanityTier.PARANOID
        else -> SanityTier.PSYCHOSIS
    }

    // Dynamic pulse rate that accelerates as sanity decays
    val pulseDuration = when (tier) {
        SanityTier.STABLE -> 2200
        SanityTier.UNSETTLED -> 1400
        SanityTier.PARANOID -> 800
        SanityTier.PSYCHOSIS -> 450
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SanityPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val animatedSanity by animateFloatAsState(
        targetValue = clampedSanity,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "AnimatedSanity"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = tier.primaryColor.copy(alpha = if (tier == SanityTier.PSYCHOSIS) pulseAlpha else 0.55f),
        label = "BorderColor"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xBB13101A),
        modifier = modifier
            .border(1.dp, animatedBorderColor, RoundedCornerShape(8.dp))
            .testTag("sanity_meter")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            // Header Row: Psychology Icon + Sanity Label + Status Badge + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .scale(if (tier == SanityTier.PSYCHOSIS) pulseScale else 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tier == SanityTier.PSYCHOSIS) Icons.Default.Warning else Icons.Default.Psychology,
                            contentDescription = "Sanity State",
                            tint = tier.primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SANITY",
                        color = Color(0xFFCFD8DC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dynamic trend badge (Draining vs Recovering vs Stable)
                    if (drainRate > 0.015f) {
                        Text(
                            text = "▼ DRAIN",
                            color = Color(0xFFFF5252),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33FF1744))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    } else if (drainRate < -0.015f) {
                        Text(
                            text = "▲ CALM",
                            color = Color(0xFF69F0AE),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3300E676))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    // Tier Status Chip
                    Text(
                        text = tier.label,
                        color = tier.primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Percentage Value
                    Text(
                        text = "$percent%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Progress Bar Track with Gradient & Threshold Ticks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x552C283B))
            ) {
                // Animated Fill with Psychological Color Spectrum
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedSanity)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF1744), // Crimson at 0%
                                    Color(0xFFFF7043), // Orange at 30%
                                    Color(0xFFFFD54F), // Amber at 60%
                                    Color(0xFF00E5FF)  // Cyan at 100%
                                )
                            )
                        )
                )

                // Subconscious threshold guide ticks at 25%, 50%, 75%
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0x44FFFFFF)))
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0x44FFFFFF)))
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0x44FFFFFF)))
                }
            }

            // Warning note for critical states
            if (clampedSanity < 0.35f || activeHorrorEvent != null) {
                Spacer(modifier = Modifier.height(2.dp))
                val causeMessage = when {
                    activeHorrorEvent != null -> "ANOMALY: $activeHorrorEvent"
                    dangerProximity > 0.5f -> "TERROR: ENTITY IS CLOSING IN!"
                    dangerProximity > 0.2f -> "UNSETTLING PRESENCE NEARBY"
                    clampedSanity < 0.25f -> "PSYCHOSIS: REALITY IS TEARING"
                    else -> "MIND UNSTABLE - SEEK LIGHT OR HIDEOUT"
                }
                Text(
                    text = causeMessage,
                    color = if (clampedSanity < 0.25f) Color(0xFFFF5252) else Color(0xFFFFB74D),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
