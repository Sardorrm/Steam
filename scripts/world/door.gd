## Door
## Manages door state (open/closed), interaction
## Can be locked, can lead to next room or exit

extends Node3D

class_name Door

@export var door_name: String = "Door"
@export var leads_to_room: String = "living_room"
@export var is_exit: bool = false  # True if this leads outside
@export var is_locked: bool = false
@export var interaction_range: float = 2.0

var is_open: bool = false
var player_nearby: Node = null

signal door_opened()
signal door_closed()

func _ready() -> void:
	add_to_group("interactive")
	
	# Create simple visual (cube)
	var mesh_instance = MeshInstance3D.new()
	var box_mesh = BoxMesh.new()
	box_mesh.size = Vector3(1, 2.5, 0.1)
	mesh_instance.mesh = box_mesh
	add_child(mesh_instance)
	
	# Add collision
	var collision = CollisionShape3D.new()
	collision.shape = BoxShape3D.new()
	collision.shape.size = Vector3(1, 2.5, 0.1)
	add_child(collision)

func interact(player: CharacterBody3D) -> void:
	"""Called when player presses interact near door"""
	
	if is_locked:
		print("[Door] ", door_name, " is locked!")
		return
	
	if is_exit:
		print("[Door] ESCAPING!")
		player.escape()
		return
	
	# Toggle door open/close
	is_open = !is_open
	
	if is_open:
		_open_door(player)
	else:
		_close_door()

func _open_door(player: CharacterBody3D) -> void:
	print("[Door] ", door_name, " opens to ", leads_to_room)
	door_opened.emit()
	
	# Visual: rotate door
	var tween = create_tween()
	tween.tween_property(self, "rotation:y", PI / 2, 0.5)
	
	# Play sound (placeholder)
	print("[Audio] Door opening sound")

func _close_door() -> void:
	print("[Door] ", door_name, " closes")
	door_closed.emit()
	
	# Visual: rotate back
	var tween = create_tween()
	tween.tween_property(self, "rotation:y", 0.0, 0.5)
	
	# Play sound
	print("[Audio] Door closing sound")

func lock_door() -> void:
	is_locked = true
	print("[Door] ", door_name, " is now LOCKED")

func unlock_door() -> void:
	is_locked = false
	print("[Door] ", door_name, " is now UNLOCKED")

func get_target_room() -> String:
	return leads_to_room
