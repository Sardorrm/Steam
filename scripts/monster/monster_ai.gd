## MonsterAI
## Intelligent monster that hunts the player
## Learns from player behavior via HouseDirector

extends CharacterBody3D

class_name Monster

# Movement
@export var patrol_speed: float = 2.0
@export var hunt_speed: float = 6.0
@export var acceleration: float = 0.15

# Senses
@export var hearing_range: float = 20.0
@export var vision_range: float = 15.0
@export var vision_angle: float = 90.0

# Behavior
var current_state: String = "idle"  # idle, patrolling, hunting, investigating
var current_velocity: Vector3 = Vector3.ZERO
var target_position: Vector3 = Vector3.ZERO
var hunt_target: CharacterBody3D = null
var intelligence: float = 0.5

# Pathfinding
var patrol_points: Array[Vector3] = []
var current_patrol_index: int = 0

# References
var game_manager: Node = null
var house_director: Node = null
var player: CharacterBody3D = null
var event_system: Node = null

signal monster_spotted()

func _ready() -> void:
	game_manager = get_tree().root.find_child("GameManager", true, false)
	house_director = get_tree().root.find_child("HouseDirector", true, false)
	player = get_tree().root.find_child("Player", true, false)
	event_system = get_tree().root.find_child("EventSystem", true, false)
	
	# Setup patrol points (room corners)
	_setup_patrol()
	
	# Monster starts idle
	current_state = "idle"

func _physics_process(delta: float) -> void:
	if not game_manager or not game_manager.is_playing:
		return
	
	# Update AI based on state
	match current_state:
		"idle":
			_idle_behavior(delta)
		"patrolling":
			_patrol_behavior(delta)
		"investigating":
			_investigate_behavior(delta)
		"hunting":
			_hunt_behavior(delta)
	
	# Apply movement
	velocity = current_velocity
	move_and_slide()

func _idle_behavior(_delta: float) -> void:
	current_velocity = current_velocity.lerp(Vector3.ZERO, 0.2)

func _patrol_behavior(delta: float) -> void:
	if patrol_points.is_empty():
		return
	
	target_position = patrol_points[current_patrol_index]
	var direction = (target_position - global_position).normalized()
	
	current_velocity = current_velocity.lerp(direction * patrol_speed, acceleration)
	current_velocity.y -= 9.8 * delta  # Gravity
	
	# Reached patrol point?
	if global_position.distance_to(target_position) < 2.0:
		current_patrol_index = (current_patrol_index + 1) % patrol_points.size()

func _investigate_behavior(delta: float) -> void:
	# Move toward last heard sound
	var direction = (target_position - global_position).normalized()
	current_velocity = current_velocity.lerp(direction * patrol_speed * 1.5, acceleration)
	current_velocity.y -= 9.8 * delta
	
	# If target reached or timeout, back to patrol
	if global_position.distance_to(target_position) < 3.0:
		current_state = "patrolling"

func _hunt_behavior(delta: float) -> void:
	if not hunt_target:
		current_state = "patrolling"
		return
	
	# Direct pursuit with learning
	target_position = hunt_target.global_position
	
	# If player is hiding, check known hide spots (from house director)
	if hunt_target.is_hiding and house_director:
		var predicted_hide = house_director.predict_hide_location()
		if predicted_hide != Vector3.ZERO:
			target_position = predicted_hide
	
	var direction = (target_position - global_position).normalized()
	var hunt_speed_adjusted = hunt_speed * (0.8 + intelligence * 0.4)  # Intelligence increases speed
	
	current_velocity = current_velocity.lerp(direction * hunt_speed_adjusted, acceleration)
	current_velocity.y -= 9.8 * delta
	
	# Catch player?
	if global_position.distance_to(hunt_target.global_position) < 1.5:
		hunt_target.be_caught()
		hunt_target = null
		current_state = "patrolling"

func hear_sound(position: Vector3, is_loud: bool) -> void:
	if current_state == "idle":
		return
	
	# Loud sounds attract monster more
	var hearing_range_adjusted = hearing_range * (1.0 if is_loud else 0.5)
	var distance = global_position.distance_to(position)
	
	if distance < hearing_range_adjusted:
		target_position = position
		current_state = "investigating"
		
		# If already hunting, update target
		if current_state == "hunting":
			hunt_target.position = position

func can_see_player() -> bool:
	if not player or player.is_hiding:
		return false
	
	var direction = (player.global_position - global_position)
	var distance = direction.length()
	
	if distance > vision_range:
		return false
	
	# Check vision angle (cone)
	var angle = global_transform.basis.z.angle_to(direction.normalized())
	return angle < deg_to_rad(vision_angle / 2.0)

## Start hunting (called by GameManager after delay)
func start_hunting() -> void:
	current_state = "patrolling"
	print("[Monster] Starting hunt...")

## Stop hunting and reset
func reset_hunt() -> void:
	current_state = "patrolling"
	hunt_target = null
	current_velocity = Vector3.ZERO

## Increase intelligence (called by HouseDirector after 2 catches)
func increase_intelligence() -> void:
	intelligence = min(intelligence + 0.2, 1.0)
	print("[Monster] Intelligence increased to: ", intelligence)

## Spot player and begin hunt
func spot_player(target: CharacterBody3D) -> void:
	if not target:
		return
	
	hunt_target = target
	current_state = "hunting"
	monster_spotted.emit()
	print("[Monster] Player spotted! Hunting...")

## Setup patrol points
func _setup_patrol() -> void:
	# Will be set by house layout
	# For now, simple room-based patrol
	patrol_points = [
		Vector3(5, 1, 5),
		Vector3(-5, 1, 5),
		Vector3(-5, 1, -5),
		Vector3(5, 1, -5),
	]

## DEBUG: Monitor monster state
func _input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed:
		if event.keycode == KEY_F4:
			print("=== MONSTER STATE ===")
			print("State: ", current_state)
			print("Position: ", position)
			print("Hunting: ", hunt_target)
			print("Intelligence: ", intelligence)
