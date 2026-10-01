package com.example.donttrustthehouse.model

enum class ItemType {
    KEY,
    BATTERY,
    SEDATIVE,
    TALISMAN,
    NOTE
}

data class GameItem(
    val id: String,
    val name: String,
    val description: String,
    val type: ItemType,
    val position: Vector2D = Vector2D.ZERO,
    val targetDoorId: String? = null,
    val value: Float = 1.0f,
    val isCollected: Boolean = false,
    val iconName: String = "key"
)

typealias InventoryItem = GameItem

/**
 * Represents the current atmospheric, architectural, and dynamic state of a room in the house.
 */
data class RoomState(
    val id: String,
    val name: String,
    val center: Vector2D = Vector2D.ZERO,
    val width: Float = 10f,
    val height: Float = 10f,
    val isLit: Boolean = true,
    val isPlayerPresent: Boolean = false,
    val isMonsterPresent: Boolean = false,
    val items: List<GameItem> = emptyList(),
    val hideSpots: List<HideSpot> = emptyList(),
    val doors: List<Door> = emptyList(),
    val dangerLevel: Float = 0f, // 0.0f (safe) to 1.0f (entity in room)
    val description: String = "",
    val isDiscovered: Boolean = false
) {
    fun contains(pos: Vector2D): Boolean {
        return pos.x >= center.x - width / 2f &&
               pos.x <= center.x + width / 2f &&
               pos.y >= center.y - height / 2f &&
               pos.y <= center.y + height / 2f
    }

    val hasUncollectedItems: Boolean
        get() = items.any { !it.isCollected }

    val availableHideSpotsCount: Int
        get() = hideSpots.count { !it.isOccupied }

    val openDoorsCount: Int
        get() = doors.count { it.isOpen }
}
