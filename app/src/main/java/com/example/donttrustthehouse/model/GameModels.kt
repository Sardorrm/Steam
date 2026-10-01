package com.example.donttrustthehouse.model

enum class MonsterState {
    IDLE,
    PATROLLING,
    INVESTIGATING,
    HUNTING
}

enum class HorrorEventType {
    MONSTER_AWAKENS,
    HEAR_OWN_VOICE,
    HEAR_OWN_FOOTSTEPS,
    FALSE_EXIT,
    TELEPORT_SCARE,
    LIGHTS_FLICKER,
    GENERIC_SCARE
}

data class HorrorEvent(
    val type: HorrorEventType,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Room(
    val id: String,
    val name: String,
    val center: Vector2D,
    val width: Float,
    val height: Float,
    val floorColor: Long = 0xFF2A2830
) {
    fun contains(pos: Vector2D): Boolean {
        return pos.x >= center.x - width / 2f &&
               pos.x <= center.x + width / 2f &&
               pos.y >= center.y - height / 2f &&
               pos.y <= center.y + height / 2f
    }
}

data class Door(
    val id: String,
    val name: String,
    val roomA: String,
    val roomB: String,
    val position: Vector2D,
    val isOpen: Boolean = false,
    val isLocked: Boolean = false,
    val isExit: Boolean = false
)

data class LightSwitch(
    val id: String,
    val name: String,
    val roomName: String,
    val position: Vector2D,
    val isOn: Boolean = true
)

data class HideSpot(
    val id: String,
    val name: String,
    val roomName: String,
    val position: Vector2D,
    val isOccupied: Boolean = false
)

data class PlayerState(
    val position: Vector2D = Vector2D(0f, 0f),
    val velocity: Vector2D = Vector2D(0f, 0f),
    val facingAngle: Float = 0f,
    val isCrouching: Boolean = false,
    val isHiding: Boolean = false,
    val currentHideSpotId: String? = null,
    val isCaught: Boolean = false,
    val noiseLevel: Float = 0f // 0f to 1f
)

data class MonsterData(
    val position: Vector2D = Vector2D(8f, 8f),
    val state: MonsterState = MonsterState.IDLE,
    val intelligence: Float = 0.5f,
    val targetPosition: Vector2D = Vector2D.ZERO,
    val facingAngle: Float = 0f,
    val huntSpeed: Float = 5.5f,
    val patrolSpeed: Float = 2.0f,
    val isSpottedByPlayer: Boolean = false
)

data class PlayerBehavior(
    val hideSpotsUsed: MutableMap<String, Int> = mutableMapOf(),
    val favoriteRooms: MutableMap<String, Int> = mutableMapOf(),
    val deathLocations: MutableList<Vector2D> = mutableListOf(),
    var escapeAttempts: Int = 0,
    var timesCaught: Int = 0,
    var playStyle: String = "cautious",
    var crouchDuration: Float = 0f,
    var runDuration: Float = 0f
)
