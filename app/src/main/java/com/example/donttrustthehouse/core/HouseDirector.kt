package com.example.donttrustthehouse.core

import com.example.donttrustthehouse.model.HorrorEventType
import com.example.donttrustthehouse.model.PlayerBehavior
import com.example.donttrustthehouse.model.Vector2D

class HouseDirector {
    val behavior = PlayerBehavior()
    var analysisReady: Boolean = false

    fun recordDeath(location: Vector2D) {
        behavior.deathLocations.add(location)
        behavior.timesCaught++
        analyzeBehavior()
    }

    fun recordHide(spotId: String, roomName: String) {
        val currentCount = behavior.hideSpotsUsed.getOrDefault(spotId, 0)
        behavior.hideSpotsUsed[spotId] = currentCount + 1

        val roomCount = behavior.favoriteRooms.getOrDefault(roomName, 0)
        behavior.favoriteRooms[roomName] = roomCount + 1

        analyzeBehavior()
    }

    fun recordMovement(isCrouching: Boolean, delta: Float) {
        if (isCrouching) {
            behavior.crouchDuration += delta
        } else {
            behavior.runDuration += delta
        }
    }

    fun recordEscapeAttempt() {
        behavior.escapeAttempts++
        analyzeBehavior()
    }

    fun analyzeBehavior() {
        val totalHides = behavior.hideSpotsUsed.values.sum()
        val totalMoveTime = behavior.crouchDuration + behavior.runDuration
        val crouchRatio = if (totalMoveTime > 0.1f) behavior.crouchDuration / totalMoveTime else 0.5f

        behavior.playStyle = when {
            totalHides >= 2 || crouchRatio > 0.65f -> "sneaky"
            behavior.escapeAttempts > behavior.timesCaught || crouchRatio < 0.25f -> "aggressive"
            else -> "cautious"
        }
        analysisReady = true
    }

    fun getNextEventType(): HorrorEventType {
        if (!analysisReady) return HorrorEventType.GENERIC_SCARE

        return when (behavior.playStyle) {
            "sneaky" -> HorrorEventType.HEAR_OWN_FOOTSTEPS
            "aggressive" -> HorrorEventType.FALSE_EXIT
            "cautious" -> HorrorEventType.HEAR_OWN_VOICE
            else -> HorrorEventType.GENERIC_SCARE
        }
    }

    fun predictHideLocation(houseManager: HouseManager): Vector2D? {
        if (behavior.hideSpotsUsed.isEmpty()) return null

        // Find most used hide spot
        val mostUsedId = behavior.hideSpotsUsed.maxByOrNull { it.value }?.key ?: return null
        return houseManager.getHideSpot(mostUsedId)?.position
    }

    fun getMonsterAggression(): Float {
        // Base aggression scales with catches
        val aggression = 0.5f + (behavior.timesCaught * 0.15f)
        return aggression.coerceIn(0.5f, 1.0f)
    }

    fun resetRun() {
        // Keeps learned long-term habits (The House remembers!), but resets temporary flags
        analyzeBehavior()
    }
}
