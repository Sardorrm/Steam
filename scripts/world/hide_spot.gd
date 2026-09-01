## HideSpot
## Places where player can hide (closet, under bed, etc.)
## Monster can learn these locations and check them

extends Node3D

class_name HideSpot

@export var hide_name: String = "Closet"
@export var room_name: String = "bedroom"
@export var hide_position: Vector3 = Vector3.ZERO  # Where player goes when hiding

var is_occupied: bool = false
var hidden_player: CharacterBody3D = null

signal player_hidden()
signal player_revealed()

func _ready() -> void:
	add_to_group("interactive")
	add_to_group("hide_spots")
	
	# Create simple visual (small box)
	var mesh_instance = MeshInstance3D.new()
	var box_mesh = BoxMesh.new()
	box_mesh.size = Vector3(1, 2, 0.5)
	mesh_instance.mesh = box_mesh
	add_child(mesh_instance)
	
	# If hide_position not set, use self position
	if hide_position == Vector3.ZERO:
		hide_position = global_position

func interact(player: CharacterBody3D) -> void:
	"""Called when player presses interact near hide spot"""
	
	if is_occupied:
		print("[HideSpot] ", hide_name, " is already occupied!")
		return
	
	hide_player(player)

func hide_player(player: CharacterBody3D) -> void:
	"""Hide player in this spot"""
	is_occupied = true
	hidden_player = player
	
	player.start_hiding(hide_position)
	player_hidden.emit()
	
	print("[HideSpot] Player hiding in ", hide_name)
	
	# If monster spots this spot while player is hidden, player caught
	var monster = get_tree().root.find_child("Monster", true, false)
	if monster:
		await get_tree().create_timer(0.5).timeout
		if monster.global_position.distance_to(hide_position) < 2.0:
			unhide_player()
			monster.spot_player(player)

func unhide_player() -> void:
	"""Player leaves hiding spot"""
	if hidden_player:
		is_occupied = false
		hidden_player.stop_hiding()
		hidden_player = null
		player_revealed.emit()
		print("[HideSpot] Player left ", hide_name)

## Monster checks this spot - is player here?
func check_spot(monster: CharacterBody3D) -> bool:
	if is_occupied and hidden_player:
		# Monster found player!
		unhide_player()
		return true
	return false

## Can randomize locations for replayability
func randomize_position() -> void:
	var offset = Vector3(randf_range(-1, 1), 0, randf_range(-1, 1))
	hide_position = hide_position + offset
