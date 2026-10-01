package com.example.donttrustthehouse.core

import com.example.donttrustthehouse.model.MonsterData
import com.example.donttrustthehouse.model.MonsterState
import com.example.donttrustthehouse.model.PlayerState
import com.example.donttrustthehouse.model.Vector2D
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MonsterAI(
    private val houseManager: HouseManager,
    private val houseDirector: HouseDirector
) {
    var data = MonsterData(position = Vector2D(26f, 15.5f))
        private set

    private val patrolPoints = listOf(
        Vector2D(15.5f, 15.5f), // Living room
        Vector2D(26.0f, 15.5f), // Bedroom
        Vector2D(15.5f, 5.5f),  // Bathroom
        Vector2D(5.5f, 15.5f),  // Kitchen
        Vector2D(15.5f, 26.0f)  // Basement
    )
    private var currentPatrolIndex = 0
    private var investigateTimer = 0f

    val hearingRange = 22f
    val visionRange = 15f
    val catchDistance = 1.6f

    fun startHunting() {
        data = data.copy(state = MonsterState.PATROLLING, targetPosition = patrolPoints[currentPatrolIndex])
    }

    fun resetHunt(respawnPos: Vector2D = Vector2D(26f, 15.5f)) {
        currentPatrolIndex = 0
        data = data.copy(
            position = respawnPos,
            state = MonsterState.PATROLLING,
            targetPosition = patrolPoints[0]
        )
    }

    fun increaseIntelligence() {
        val newIntel = (data.intelligence + 0.2f).coerceAtMost(1.0f)
        val newHuntSpeed = 5.5f * (0.85f + newIntel * 0.35f)
        data = data.copy(intelligence = newIntel, huntSpeed = newHuntSpeed)
    }

    fun hearSound(position: Vector2D, isLoud: Boolean) {
        if (data.state == MonsterState.IDLE) return

        val effectiveHearingRange = if (isLoud) hearingRange else (hearingRange * 0.35f)
        val distance = data.position.distanceTo(position)

        if (distance <= effectiveHearingRange) {
            data = data.copy(
                targetPosition = position,
                state = if (data.state == MonsterState.HUNTING) MonsterState.HUNTING else MonsterState.INVESTIGATING
            )
            investigateTimer = 5.0f
        }
    }

    fun canSeePlayer(player: PlayerState): Boolean {
        if (player.isHiding) return false

        val dist = data.position.distanceTo(player.position)
        val currentRoom = houseManager.getRoomAt(player.position)
        val isLit = currentRoom?.let { houseManager.isRoomLit(it.id) } ?: true

        var effectiveVisionRange = visionRange * (if (isLit) 1f else 0.45f)
        if (player.isCrouching) {
            effectiveVisionRange *= 0.65f
        }

        if (dist > effectiveVisionRange) return false

        // Check facing cone (approx 120 degrees)
        val toPlayer = player.position - data.position
        val angleToPlayer = atan2(toPlayer.y, toPlayer.x)
        var diff = angleToPlayer - data.facingAngle
        while (diff < -Math.PI) diff += (2 * Math.PI).toFloat()
        while (diff > Math.PI) diff -= (2 * Math.PI).toFloat()

        return kotlin.math.abs(diff) < (Math.PI / 2.0)
    }

    fun update(
        delta: Float,
        player: PlayerState,
        onCatchPlayer: () -> Unit,
        onSpotPlayer: () -> Unit
    ) {
        if (data.state == MonsterState.IDLE) return

        // Vision check
        if (canSeePlayer(player)) {
            if (data.state != MonsterState.HUNTING) {
                onSpotPlayer()
            }
            data = data.copy(
                state = MonsterState.HUNTING,
                targetPosition = player.position,
                isSpottedByPlayer = true
            )
        }

        var speed = data.patrolSpeed
        var target = data.targetPosition

        when (data.state) {
            MonsterState.PATROLLING -> {
                speed = data.patrolSpeed
                target = patrolPoints[currentPatrolIndex]
                if (data.position.distanceTo(target) < 1.5f) {
                    currentPatrolIndex = (currentPatrolIndex + 1) % patrolPoints.size
                    target = patrolPoints[currentPatrolIndex]
                }
            }
            MonsterState.INVESTIGATING -> {
                speed = data.patrolSpeed * 1.5f
                investigateTimer -= delta
                if (data.position.distanceTo(target) < 1.5f || investigateTimer <= 0f) {
                    data = data.copy(state = MonsterState.PATROLLING)
                }
            }
            MonsterState.HUNTING -> {
                speed = data.huntSpeed

                if (player.isHiding) {
                    // Consult HouseDirector learning!
                    val predictedHide = houseDirector.predictHideLocation(houseManager)
                    if (predictedHide != null) {
                        target = predictedHide
                    } else {
                        // Lost player
                        data = data.copy(state = MonsterState.INVESTIGATING)
                        investigateTimer = 3.0f
                    }
                } else {
                    target = player.position
                }

                // Catch check
                if (data.position.distanceTo(player.position) < catchDistance) {
                    onCatchPlayer()
                    return
                }
            }
            MonsterState.IDLE -> return
        }

        // Move towards target
        val dir = (target - data.position).normalized()
        val newPos = data.position + dir * (speed * delta)
        val newAngle = if (dir.length() > 0.01f) atan2(dir.y, dir.x) else data.facingAngle

        data = data.copy(
            position = newPos,
            targetPosition = target,
            facingAngle = newAngle
        )
    }
}
