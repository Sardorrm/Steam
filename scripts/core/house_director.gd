## HouseDirector
## THE HOUSE LEARNS HOW YOU PLAY
## Records player behavior, analyzes patterns, adjusts monster behavior

extends Node

class PlayerBehavior:
	var hide_spots: Array[Vector3] = []
	var favorite_rooms: Array[String] = []
	var death_locations: Array[Vector3] = []
	var escape_attempts: int = 0
	var times_caught: int = 0
	var play_style: String = ""  # "aggressive", "sneaky", "runner"

var behavior: PlayerBehavior = PlayerBehavior.new()
var analysis_ready: bool = false
var game_manager: Node = null
var monster: CharacterBody3D = null

func _ready() -> void:
	game_manager = get_tree().root.find_child("GameManager", true, false)
	monster = get_tree().root.find_child("Monster", true, false)

## Called when player dies - record location
func record_death(location: Vector3) -> void:
	behavior.death_locations.append(location)
	behavior.times_caught += 1
	analyze_behavior()
	
	# After 2 deaths, monster gets slightly smarter
	if behavior.times_caught == 2:
		monster.increase_intelligence()

## Called when player hides successfully
func record_hide(location: Vector3, room_name: String) -> void:
	behavior.hide_spots.append(location)
	if room_name not in behavior.favorite_rooms:
		behavior.favorite_rooms.append(room_name)

## Analyze what we learned about the player
func analyze_behavior() -> void:
	if behavior.times_caught < 1:
		return
	
	# Determine play style
	if behavior.times_caught >= 2 and behavior.hide_spots.size() > 2:
		behavior.play_style = "sneaky"
	elif behavior.escape_attempts > behavior.times_caught:
		behavior.play_style = "aggressive"
	else:
		behavior.play_style = "cautious"
	
	analysis_ready = true
	print("[HouseDirector] Detected play style: ", behavior.play_style)

## Get smart event for this player's behavior
func get_next_event() -> String:
	if not analysis_ready:
		return "generic_scare"
	
	# Generate event based on what we know
	match behavior.play_style:
		"sneaky":
			# Scare them by mimicking their hiding behavior
			return "hear_own_footsteps"
		"aggressive":
			# Make escape seem possible, then block it
			return "false_exit"
		"cautious":
			# Psychological horror - isolation
			return "hear_own_voice"
	
	return "generic_scare"

## Predict where player will hide next
func predict_hide_location() -> Vector3:
	if behavior.hide_spots.is_empty():
		return Vector3.ZERO
	
	# Return most used hide spot (monster will check there first)
	var most_used = behavior.hide_spots[0]
	for spot in behavior.hide_spots:
		if behavior.hide_spots.count(spot) > behavior.hide_spots.count(most_used):
			most_used = spot
	
	return most_used

## Adaptive difficulty
func get_monster_aggression() -> float:
	# Base aggression increases with each catch
	var aggression = 0.5 + (behavior.times_caught * 0.15)
	aggression = clamp(aggression, 0.5, 1.0)
	return aggression

## DEBUG: See what we learned
func _input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed:
		if event.keycode == KEY_F2:
			print("=== HOUSE DIRECTOR ANALYSIS ===")
			print("Play style: ", behavior.play_style)
			print("Times caught: ", behavior.times_caught)
			print("Hide spots found: ", behavior.hide_spots.size())
			print("Favorite rooms: ", behavior.favorite_rooms)
			print("Monster aggression: ", get_monster_aggression())
