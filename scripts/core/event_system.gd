## EventSystem
## Generates horror events, scary moments, psychological scares
## Smart events based on player behavior via HouseDirector

extends Node

class_name EventSystem

# Audio references (will be loaded at runtime)
var audio_effects: Dictionary = {}
var last_event_time: float = 0.0
var event_cooldown: float = 15.0

var game_manager: Node = null
var house_director: Node = null
var player: CharacterBody3D = null

signal horror_event_triggered(event_type: String)

func _ready() -> void:
	game_manager = get_tree().root.find_child("GameManager", true, false)
	house_director = get_tree().root.find_child("HouseDirector", true, false)
	player = get_tree().root.find_child("Player", true, false)
	
	# Load or create audio effects
	_setup_audio()

## Play a specific horror event
func play_event(event_type: String) -> void:
	if Time.get_ticks_msec() - last_event_time < event_cooldown * 1000:
		return  # Cooldown active
	
	last_event_time = Time.get_ticks_msec()
	horror_event_triggered.emit(event_type)
	
	match event_type:
		"monster_awakens":
			_play_sound("monster_wake")
			print("[Event] Monster awakens...")
		
		"hear_own_voice":
			_play_sound("voice_echo")
			print("[Event] You hear your own voice in the distance...")
		
		"hear_own_footsteps":
			_play_sound("footsteps")
			print("[Event] Footsteps that don't match your movement...")
		
		"false_exit":
			_play_sound("door_open")
			print("[Event] The door to outside appears open...")
		
		"teleport_scare":
			if player and randf() > 0.5:  # Rare event
				_teleport_random_direction()
				_play_sound("static")
				print("[Event] ...you're not where you were...")
		
		"lights_flicker":
			_play_sound("electric_buzz")
			print("[Event] Lights begin to flicker...")
		
		"generic_scare":
			var scares = ["monster_wake", "voice_echo", "footsteps"]
			_play_sound(scares[randi() % scares.size()])

## Intelligently trigger next horror event
func trigger_smart_event() -> void:
	if not house_director or not house_director.analysis_ready:
		play_event("generic_scare")
		return
	
	var event = house_director.get_next_event()
	play_event(event)

## Play audio - placeholder (will use AudioStreamPlayer3D in full game)
func _play_sound(sound_name: String) -> void:
	# Placeholder: In real game, instantiate AudioStreamPlayer3D
	# For now, just log it
	print("[Audio] Playing sound: ", sound_name)

## Rare teleport scare - disorient player
func _teleport_random_direction() -> void:
	if not player:
		return
	
	# Move player randomly but slightly forward/backward/sideways
	var offset = Vector3(randf_range(-2, 2), 0, randf_range(-2, 2))
	player.position += offset
	print("[Teleport] Player moved: ", offset)

## Setup audio files (will be actual files in production)
func _setup_audio() -> void:
	# Placeholder - in real game, load AudioStream resources
	audio_effects = {
		"monster_wake": "Low, deep growl",
		"voice_echo": "Distorted player voice",
		"footsteps": "Wet footsteps on tiles",
		"door_open": "Creaking door",
		"electric_buzz": "High-pitched buzz",
		"static": "Radio static and whispers"
	}

## Periodic event trigger
func _physics_process(_delta: float) -> void:
	# Chance to trigger event every frame (low probability)
	if game_manager and game_manager.is_playing:
		if randf() < 0.0001:  # Very rare
			trigger_smart_event()
