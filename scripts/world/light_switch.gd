## LightSwitch
## Toggle room lights on/off
## Turning off lights can help hide from monster

extends Node3D

class_name LightSwitch

@export var switch_name: String = "Light Switch"
@export var controls_room: String = "living_room"

var is_on: bool = true
var light_node: OmniLight3D = null

signal lights_toggled(is_on: bool)

func _ready() -> void:
	add_to_group("interactive")
	
	# Create light
	light_node = OmniLight3D.new()
	light_node.omni_range = 15.0
	add_child(light_node)
	
	# Create switch visual (small cube)
	var mesh_instance = MeshInstance3D.new()
	var box_mesh = BoxMesh.new()
	box_mesh.size = Vector3(0.2, 0.4, 0.1)
	mesh_instance.mesh = box_mesh
	add_child(mesh_instance)

func interact(player: CharacterBody3D) -> void:
	"""Called when player interacts with switch"""
	toggle_lights()

func toggle_lights() -> void:
	is_on = !is_on
	
	if is_on:
		_turn_on()
	else:
		_turn_off()

func _turn_on() -> void:
	print("[Light] ", switch_name, " turned ON in ", controls_room)
	lights_toggled.emit(true)
	
	if light_node:
		light_node.visible = true
	
	# Tween for fade-in
	var tween = create_tween()
	if light_node:
		tween.tween_property(light_node, "energy", 1.0, 0.3)

func _turn_off() -> void:
	print("[Light] ", switch_name, " turned OFF in ", controls_room)
	lights_toggled.emit(false)
	
	if light_node:
		light_node.visible = false
	
	# Tween for fade-out
	var tween = create_tween()
	if light_node:
		tween.tween_property(light_node, "energy", 0.0, 0.3)

## Lights can be turned off by monster or events
func force_off() -> void:
	is_on = false
	_turn_off()

func force_on() -> void:
	is_on = true
	_turn_on()
