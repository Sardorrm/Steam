## HouseManager
## Manages house layout, rooms, doors, lights
## Simple procedural layout for replayability

extends Node3D

class_name HouseManager

# Room data
var rooms: Dictionary = {}
var doors: Array[Node] = []
var lights: Array[Node] = []

# Layout (room connections)
var connections: Dictionary = {}

func _ready() -> void:
	_initialize_house()

func _initialize_house() -> void:
	"""Create a simple 5-room house layout"""
	
	# Room definitions
	# Each room: {"name": String, "center": Vector3, "size": Vector3}
	rooms = {
		"living_room": {
			"center": Vector3(0, 1, 0),
			"size": Vector3(8, 3, 8),
		},
		"bedroom": {
			"center": Vector3(10, 1, 0),
			"size": Vector3(6, 3, 6),
		},
		"kitchen": {
			"center": Vector3(-10, 1, 0),
			"size": Vector3(6, 3, 6),
		},
		"bathroom": {
			"center": Vector3(0, 1, 10),
			"size": Vector3(4, 3, 4),
		},
		"basement": {
			"center": Vector3(0, -4, 0),
			"size": Vector3(8, 3, 8),
		},
	}
	
	# Door connections (which rooms connect to which)
	connections = {
		"living_room": ["bedroom", "kitchen", "bathroom", "basement"],
		"bedroom": ["living_room"],
		"kitchen": ["living_room"],
		"bathroom": ["living_room"],
		"basement": ["living_room"],
	}
	
	# Dynamically create visual representations if needed
	# For now, this is data structure
	print("[HouseManager] House initialized with ", rooms.size(), " rooms")

## Get room by name
func get_room(room_name: String) -> Dictionary:
	if room_name in rooms:
		return rooms[room_name]
	return {}

## Get center position of room
func get_room_center(room_name: String) -> Vector3:
	var room = get_room(room_name)
	if room.is_empty():
		return Vector3.ZERO
	return room["center"]

## Check if position is inside a room
func get_room_at_position(position: Vector3) -> String:
	for room_name in rooms.keys():
		var room = rooms[room_name]
		var center = room["center"]
		var size = room["size"]
		
		# Simple AABB check
		if position.x > center.x - size.x/2 and position.x < center.x + size.x/2 and \
		   position.z > center.z - size.z/2 and position.z < center.z + size.z/2:
			return room_name
	
	return "unknown"

## Get all connected rooms from current room
func get_connected_rooms(room_name: String) -> Array[String]:
	var connected: Array[String] = []
	if room_name in connections:
		connected = connections[room_name]
	return connected

## Find path between two rooms (simple)
func find_path(from_room: String, to_room: String) -> Array[String]:
	var path: Array[String] = [from_room]
	var queue: Array[String] = [from_room]
	var visited: Array[String] = [from_room]
	
	while not queue.is_empty():
		var current = queue.pop_front()
		
		if current == to_room:
			return path
		
		for connected in get_connected_rooms(current):
			if connected not in visited:
				visited.append(connected)
				queue.append(connected)
				path.append(connected)
	
	return path

## Randomize which rooms are available (for replayability)
func randomize_layout() -> void:
	# Simple: randomly close 0-2 doors
	var rooms_to_close = randi_range(0, 2)
	var closed_rooms: Array[String] = []
	var all_rooms = rooms.keys()
	
	for i in range(rooms_to_close):
		var random_room = all_rooms[randi() % all_rooms.size()]
		if random_room != "living_room":  # Never close starting room
			closed_rooms.append(random_room)
	
	print("[HouseManager] Randomized: Closed rooms: ", closed_rooms)

## Toggle all lights
func toggle_all_lights(on: bool) -> void:
	for light in lights:
		if light and light.has_method("set_enabled"):
			light.set_enabled(on)

## DEBUG: Print house info
func _input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed:
		if event.keycode == KEY_F5:
			print("=== HOUSE LAYOUT ===")
			for room_name in rooms.keys():
				print("Room: ", room_name, " at ", rooms[room_name]["center"])
				print("  Connects to: ", connections.get(room_name, []))
