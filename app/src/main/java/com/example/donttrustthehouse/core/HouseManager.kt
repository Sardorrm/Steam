package com.example.donttrustthehouse.core

import com.example.donttrustthehouse.model.Door
import com.example.donttrustthehouse.model.GameItem
import com.example.donttrustthehouse.model.HideSpot
import com.example.donttrustthehouse.model.ItemType
import com.example.donttrustthehouse.model.LightSwitch
import com.example.donttrustthehouse.model.Room
import com.example.donttrustthehouse.model.RoomState
import com.example.donttrustthehouse.model.Vector2D
import kotlin.random.Random

class HouseManager {

    val rooms = mutableMapOf<String, Room>()
    val doors = mutableListOf<Door>()
    val lightSwitches = mutableListOf<LightSwitch>()
    val hideSpots = mutableListOf<HideSpot>()
    val items = mutableListOf<GameItem>()
    val discoveredRoomIds = mutableSetOf<String>()

    init {
        initializeHouse()
    }

    fun initializeHouse() {
        rooms.clear()
        doors.clear()
        lightSwitches.clear()
        hideSpots.clear()
        items.clear()
        discoveredRoomIds.clear()
        discoveredRoomIds.add("living_room")

        // 5 interconnected rooms aligned with the 32x32 3D Grid
        rooms["living_room"] = Room(
            id = "living_room",
            name = "Living Room",
            center = Vector2D(15.5f, 15.5f),
            width = 10f,
            height = 10f,
            floorColor = 0xFF2A2328
        )
        rooms["bedroom"] = Room(
            id = "bedroom",
            name = "Bedroom",
            center = Vector2D(26.0f, 15.5f),
            width = 8f,
            height = 10f,
            floorColor = 0xFF202330
        )
        rooms["kitchen"] = Room(
            id = "kitchen",
            name = "Kitchen",
            center = Vector2D(5.5f, 15.5f),
            width = 8f,
            height = 10f,
            floorColor = 0xFF1C2826
        )
        rooms["bathroom"] = Room(
            id = "bathroom",
            name = "Bathroom",
            center = Vector2D(15.5f, 5.5f),
            width = 10f,
            height = 8f,
            floorColor = 0xFF252528
        )
        rooms["basement"] = Room(
            id = "basement",
            name = "Basement",
            center = Vector2D(15.5f, 26.0f),
            width = 10f,
            height = 8f,
            floorColor = 0xFF19171C
        )

        // Doors
        doors.add(
            Door(
                id = "door_bedroom",
                name = "Bedroom Door",
                roomA = "living_room",
                roomB = "bedroom",
                position = Vector2D(21f, 16f),
                isOpen = true
            )
        )
        doors.add(
            Door(
                id = "door_kitchen",
                name = "Kitchen Door",
                roomA = "living_room",
                roomB = "kitchen",
                position = Vector2D(10f, 16f),
                isOpen = true
            )
        )
        doors.add(
            Door(
                id = "door_bathroom",
                name = "Bathroom Door",
                roomA = "living_room",
                roomB = "bathroom",
                position = Vector2D(16f, 10f),
                isOpen = true
            )
        )
        doors.add(
            Door(
                id = "door_basement",
                name = "Basement Door",
                roomA = "living_room",
                roomB = "basement",
                position = Vector2D(12f, 21f),
                isOpen = false,
                isLocked = true // Locked: Requires Brass Basement Key from Bedroom
            )
        )
        doors.add(
            Door(
                id = "door_exit",
                name = "Front Exit",
                roomA = "living_room",
                roomB = "outside",
                position = Vector2D(16f, 21f),
                isOpen = false,
                isExit = true,
                isLocked = true // Locked: Requires Heavy Iron Exit Key from Basement
            )
        )

        // Light Switches
        lightSwitches.add(LightSwitch("light_living", "Living Switch", "living_room", Vector2D(14f, 14f), isOn = true))
        lightSwitches.add(LightSwitch("light_bedroom", "Bedroom Switch", "bedroom", Vector2D(24f, 14f), isOn = true))
        lightSwitches.add(LightSwitch("light_kitchen", "Kitchen Switch", "kitchen", Vector2D(7f, 14f), isOn = true))
        lightSwitches.add(LightSwitch("light_bathroom", "Bathroom Switch", "bathroom", Vector2D(14f, 7f), isOn = true))
        lightSwitches.add(LightSwitch("light_basement", "Basement Switch", "basement", Vector2D(14f, 24f), isOn = false))

        // Hide Spots
        hideSpots.add(HideSpot("hide_closet", "Wardrobe Closet", "bedroom", Vector2D(28f, 13f)))
        hideSpots.add(HideSpot("hide_bed", "Under the Bed", "bedroom", Vector2D(25f, 18f)))
        hideSpots.add(HideSpot("hide_pantry", "Pantry Cabinet", "kitchen", Vector2D(4f, 13f)))
        hideSpots.add(HideSpot("hide_curtain", "Behind Shower Curtain", "bathroom", Vector2D(17f, 4f)))
        hideSpots.add(HideSpot("hide_furnace", "Dark Furnace Corner", "basement", Vector2D(18f, 28f)))
        hideSpots.add(HideSpot("hide_crate", "Storage Crate", "basement", Vector2D(13f, 28f)))

        // House Items for Exploration & Survival
        // 1. Living Room: Blueprint / Note
        items.add(
            GameItem(
                id = "item_house_blueprint",
                name = "House Blueprint",
                description = "Architectural schematic: Key to the basement is hidden in the bedroom. Exit key is sealed in the basement.",
                type = ItemType.NOTE,
                position = Vector2D(16.5f, 13.5f),
                iconName = "blueprint"
            )
        )
        // 2. Bedroom: Basement Key & Calming Sedative
        items.add(
            GameItem(
                id = "item_key_basement",
                name = "Brass Basement Key",
                description = "An antique brass key. Unlocks the heavy door leading into the dark basement.",
                type = ItemType.KEY,
                position = Vector2D(27.5f, 17.0f),
                targetDoorId = "door_basement",
                iconName = "key"
            )
        )
        items.add(
            GameItem(
                id = "item_sedative_bedroom",
                name = "Calming Sedative",
                description = "Anti-panic drops found on the vanity table. Restores 45% sanity.",
                type = ItemType.SEDATIVE,
                position = Vector2D(24.0f, 13.0f),
                value = 0.45f,
                iconName = "sedative"
            )
        )
        // 3. Kitchen: Flashlight Battery & Investigator Note
        items.add(
            GameItem(
                id = "item_battery_kitchen",
                name = "Flashlight Battery",
                description = "A fresh high-capacity battery cell. Replenishes flashlight power and keeps darkness away.",
                type = ItemType.BATTERY,
                position = Vector2D(4.5f, 17.5f),
                value = 1.0f,
                iconName = "battery"
            )
        )
        items.add(
            GameItem(
                id = "item_note_kitchen",
                name = "Investigator's Journal",
                description = "Excerpt: 'The entity hunts by sound and panic. Crouching dampens your footsteps. Use hiding spots to recover your sanity.'",
                type = ItemType.NOTE,
                position = Vector2D(7.0f, 13.5f),
                iconName = "note"
            )
        )
        // 4. Bathroom: Adrenaline Syringe
        items.add(
            GameItem(
                id = "item_sedative_bathroom",
                name = "Adrenaline Syringe",
                description = "Emergency stimulant syringe from the medicine cabinet. Restores 50% sanity instantly.",
                type = ItemType.SEDATIVE,
                position = Vector2D(13.5f, 4.5f),
                value = 0.50f,
                iconName = "sedative"
            )
        )
        // 5. Basement: Heavy Exit Key & Occult Talisman
        items.add(
            GameItem(
                id = "item_key_exit",
                name = "Heavy Iron Exit Key",
                description = "The master front exit key. Unlock the front door to escape the nightmare!",
                type = ItemType.KEY,
                position = Vector2D(18.5f, 27.5f),
                targetDoorId = "door_exit",
                iconName = "key"
            )
        )
        items.add(
            GameItem(
                id = "item_ward_talisman",
                name = "Occult Talisman",
                description = "An ancient protective charm etched with protective sigils. Use it to banish the monster back to its lair.",
                type = ItemType.TALISMAN,
                position = Vector2D(13.0f, 24.5f),
                value = 1.0f,
                iconName = "talisman"
            )
        )
    }

    fun randomizeLayout() {
        initializeHouse()
        val candidateDoors = doors.filter { !it.isExit && it.id != "door_basement" }
        if (candidateDoors.isNotEmpty() && Random.nextBoolean()) {
            val randomDoor = candidateDoors.random()
            val index = doors.indexOf(randomDoor)
            doors[index] = randomDoor.copy(isOpen = false, isLocked = Random.nextFloat() < 0.25f)
        }
    }

    fun getRoomAt(pos: Vector2D): Room? {
        return rooms.values.firstOrNull { it.contains(pos) }
    }

    fun toggleDoor(doorId: String): Door? {
        val index = doors.indexOfFirst { it.id == doorId }
        if (index != -1) {
            val door = doors[index]
            if (door.isLocked) return null
            val updated = door.copy(isOpen = !door.isOpen)
            doors[index] = updated
            return updated
        }
        return null
    }

    fun unlockDoor(doorId: String): Boolean {
        val index = doors.indexOfFirst { it.id == doorId }
        if (index != -1) {
            doors[index] = doors[index].copy(isLocked = false, isOpen = true)
            return true
        }
        return false
    }

    fun toggleLight(switchId: String): Boolean? {
        val index = lightSwitches.indexOfFirst { it.id == switchId }
        if (index != -1) {
            val sw = lightSwitches[index]
            val updated = sw.copy(isOn = !sw.isOn)
            lightSwitches[index] = updated
            return updated.isOn
        }
        return null
    }

    fun isRoomLit(roomId: String): Boolean {
        val sw = lightSwitches.firstOrNull { it.roomName == roomId }
        return sw?.isOn ?: true
    }

    fun setHideSpotOccupied(spotId: String, occupied: Boolean): Boolean {
        val index = hideSpots.indexOfFirst { it.id == spotId }
        if (index != -1) {
            val spot = hideSpots[index]
            hideSpots[index] = spot.copy(isOccupied = occupied)
            return true
        }
        return false
    }

    fun getHideSpot(spotId: String): HideSpot? {
        return hideSpots.firstOrNull { it.id == spotId }
    }

    fun getItemsInRoom(roomId: String): List<GameItem> {
        val room = rooms[roomId] ?: return emptyList()
        return items.filter { !it.isCollected && room.contains(it.position) }
    }

    fun collectItem(itemId: String): GameItem? {
        val index = items.indexOfFirst { it.id == itemId && !it.isCollected }
        if (index != -1) {
            val collected = items[index].copy(isCollected = true)
            items[index] = collected
            return collected
        }
        return null
    }

    fun getRoomState(roomId: String, playerPos: Vector2D, monsterPos: Vector2D): RoomState? {
        val room = rooms[roomId] ?: return null
        val isPlayerIn = room.contains(playerPos)
        if (isPlayerIn) {
            discoveredRoomIds.add(roomId)
        }
        val isMonsterIn = room.contains(monsterPos)
        val roomItems = items.filter { !it.isCollected && room.contains(it.position) }
        val roomHideSpots = hideSpots.filter { it.roomName == roomId }
        val roomDoors = doors.filter { it.roomA == roomId || it.roomB == roomId }
        val distToMonster = playerPos.distanceTo(monsterPos)
        val danger = if (isMonsterIn) 1.0f else (1f - distToMonster / 20f).coerceIn(0f, 0.9f)

        val roomDescription = when (roomId) {
            "living_room" -> "Central foyer with creaking floorboards and grandfather clock echoes."
            "bedroom" -> "Dark master bedroom with heavy wardrobe closets and bedside cabinets."
            "kitchen" -> "Cold tiled kitchen. Rusty water drips slowly from the corroded sink."
            "bathroom" -> "Damp porcelain and cracked mirrors. Shadows crawl across the shower curtain."
            "basement" -> "Subterranean cold stone chamber where the entity was first summoned."
            else -> "Shadowy unexplored corner of the house."
        }

        return RoomState(
            id = room.id,
            name = room.name,
            center = room.center,
            width = room.width,
            height = room.height,
            isLit = isRoomLit(roomId),
            isPlayerPresent = isPlayerIn,
            isMonsterPresent = isMonsterIn,
            items = roomItems,
            hideSpots = roomHideSpots,
            doors = roomDoors,
            dangerLevel = danger,
            description = roomDescription,
            isDiscovered = discoveredRoomIds.contains(roomId)
        )
    }

    fun getAllRoomStates(playerPos: Vector2D, monsterPos: Vector2D): Map<String, RoomState> {
        return rooms.keys.mapNotNull { roomId ->
            getRoomState(roomId, playerPos, monsterPos)?.let { roomId to it }
        }.toMap()
    }
}
