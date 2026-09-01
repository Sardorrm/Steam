## PlayerController
## Handles player movement, interaction, hiding
## Must feel responsive and smooth

extends CharacterBody3D

class_name Player

# Movement
@export var move_speed: float = 5.0
@export var crouch_speed: float = 2.5
@export var acceleration: float = 0.2
@export var friction: float = 0.1

# References
var current_velocity: Vector3 = Vector3.ZERO
var is_crouching: bool = false
var is_hiding: bool = false
var is_caught: bool = false
var game_manager: Node = null
var house_director: Node = null

# Interaction
var nearest_interactive: Node = null
@export var interaction_range: float = 2.0

# Audio
var footstep_timer: float = 0.0
@export var footstep_interval_normal: float = 0.4
@export var footstep_interval_crouch: float = 0.8

signal player_moved(position: Vector3, velocity: Vector3)
signal player_hiding(hiding: bool)
signal footstep_made(is_loud: bool)

func _ready() -> void:
	game_manager = get_tree().root.find_child("GameManager", true, false)
	house_director = get_tree().root.find_child("HouseDirector", true, false)
	
	# Start at spawn position
	position = Vector3(0, 1, 0)

func _physics_process(delta: float) -> void:
	if not game_manager or not game_manager.is_playing or is_caught:
		return
	
	# Handle input
	var input_direction = _get_input()
	var target_speed = crouch_speed if is_crouching else move_speed
	
	# Accelerate/decelerate smoothly
	var desired_velocity = input_direction.normalized() * target_speed
	current_velocity.x = lerp(current_velocity.x, desired_velocity.x, acceleration)
	current_velocity.z = lerp(current_velocity.z, desired_velocity.z, acceleration)
	
	# Apply gravity
	current_velocity.y -= 9.8 * delta
	
	# Move character
	velocity = current_velocity
	move_and_slide()
	
	# Emit position for monster hearing
	if input_direction.length() > 0:
		player_moved.emit(global_position, current_velocity)
		_make_footstep()
	
	# Check for nearby interactive objects
	_update_nearest_interactive()
	
	# Handle interaction
	_handle_interaction()

func _get_input() -> Vector3:
	var direction = Vector3.ZERO
	
	if Input.is_action_pressed("move_forward"):
		direction -= global_transform.basis.z
	if Input.is_action_pressed("move_backward"):
		direction += global_transform.basis.z
	if Input.is_action_pressed("move_left"):
		direction -= global_transform.basis.x
	if Input.is_action_pressed("move_right"):
		direction += global_transform.basis.x
	
	# Toggle crouch
	if Input.is_action_just_pressed("crouch"):
		is_crouching = not is_crouching
	
	return direction

func _make_footstep() -> void:
	footstep_timer += 1.0 / Engine.get_physics_frames()
	var interval = footstep_interval_crouch if is_crouching else footstep_interval_normal
	
	if footstep_timer >= interval:
		footstep_timer = 0.0
		var is_loud = not is_crouching
		footstep_made.emit(is_loud)
		
		# Monster hears footsteps
		var monster = get_tree().root.find_child("Monster", true, false)
		if monster:
			monster.hear_sound(global_position, is_loud)

func _update_nearest_interactive() -> void:
	# Find closest interactive object
	nearest_interactive = null
	var closest_distance = interaction_range
	
	var space_state = get_world_3d().direct_space_state
	var query = PhysicsShapeQueryParameters3D.new()
	query.shape = SphereShape3D.new()
	query.shape.radius = interaction_range
	query.transform.origin = global_position
	
	# Simple raycast/sphere cast would go here
	# For now, just check manually
	for obj in get_tree().get_nodes_in_group("interactive"):
		var distance = global_position.distance_to(obj.global_position)
		if distance < closest_distance:
			closest_distance = distance
			nearest_interactive = obj

func _handle_interaction() -> void:
	if Input.is_action_just_pressed("interact") and nearest_interactive:
		nearest_interactive.interact(self)

## Hide in current location (called by interactive objects)
func start_hiding(hide_location: Vector3) -> void:
	is_hiding = true
	is_crouching = true
	position = hide_location
	current_velocity = Vector3.ZERO
	player_hiding.emit(true)
	
	# Record this for house director
	if house_director:
		house_director.record_hide(hide_location, get_parent().name if get_parent() else "unknown")
	
	print("[Player] Hidden!")

func stop_hiding() -> void:
	is_hiding = false
	player_hiding.emit(false)
	print("[Player] No longer hidden")

## Called when monster catches player
func be_caught() -> void:
	is_caught = true
	print("[Player] Caught by monster!")
	
	if game_manager:
		game_manager.player_caught()
	
	# Reset after delay
	await get_tree().create_timer(2.0).timeout
	is_caught = false
	is_hiding = false

## Escape the house (called by exit door)
func escape() -> void:
	print("[Player] ESCAPED!")
	if game_manager:
		game_manager.escape_house()

## DEBUG: Print player state
func _input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed:
		if event.keycode == KEY_F3:
			print("=== PLAYER STATE ===")
			print("Position: ", position)
			print("Velocity: ", current_velocity)
			print("Crouching: ", is_crouching)
			print("Hiding: ", is_hiding)
			print("Caught: ", is_caught)
