## GameManager
## Global game state, objective tracking, win/lose conditions
## Purpose: Single source of truth for game progression

extends Node

# Game state
var is_playing: bool = false
var player_alive: bool = true
var lives: int = 3
var objective_completed: bool = false

# References
@onready var player: CharacterBody3D = null
@onready var monster: CharacterBody3D = null
@onready var house_director: Node = null
@onready var event_system: Node = null

# Events
signal player_died(remaining_lives: int)
signal objective_changed(new_objective: String)
signal game_over(victory: bool)
signal monster_spotted()

func _ready() -> void:
	# Make this singleton persist across scenes
	add_to_group("singleton")
	
	if not is_node_ready():
		await tree_entered
	
	# Find core systems
	await get_tree().process_frame  # Wait for scene to load
	player = get_tree().root.find_child("Player", true, false)
	monster = get_tree().root.find_child("Monster", true, false)
	house_director = get_tree().root.find_child("HouseDirector", true, false)
	event_system = get_tree().root.find_child("EventSystem", true, false)
	
	# Start game
	start_game()

func start_game() -> void:
	is_playing = true
	player_alive = true
	lives = 3
	objective_completed = false
	
	objective_changed.emit("Escape the house")
	
	# Monster starts hunting after delay
	await get_tree().create_timer(5.0).timeout
	if is_playing and monster:
		monster.start_hunting()
		event_system.play_event("monster_awakens")

func player_caught() -> void:
	if not is_playing:
		return
	
	lives -= 1
	player_died.emit(lives)
	
	# Record behavior for house director
	if house_director:
		house_director.record_death(player.global_position)
	
	# Reset positions
	player.position = Vector3(0, 1, 0)  # Spawn position
	monster.reset_hunt()
	
	if lives <= 0:
		end_game(false)  # Player loses
	else:
		# Continue hunting
		await get_tree().create_timer(2.0).timeout
		if is_playing:
			monster.start_hunting()

func escape_house() -> void:
	objective_completed = true
	end_game(true)  # Player wins

func end_game(victory: bool) -> void:
	is_playing = false
	game_over.emit(victory)
	print("Game Over! Victory: ", victory)
	
	# Fade out, show results, option to restart
	await get_tree().create_timer(3.0).timeout
	get_tree().reload_current_scene()

func get_game_time() -> float:
	return get_tree().get_frame().time

# DEBUG: Print current state
func _input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed:
		if event.keycode == KEY_F1:
			print("=== GAME STATE ===")
			print("Player alive: ", player_alive)
			print("Lives: ", lives)
			print("Playing: ", is_playing)
			print("Objective complete: ", objective_completed)
