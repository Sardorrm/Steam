## HUD (Heads-Up Display)
## Shows game state to player: objective, lives, status

extends CanvasLayer

class_name HUD

@onready var label_objective = Label.new()
@onready var label_lives = Label.new()
@onready var label_status = Label.new()

var game_manager: Node = null

func _ready() -> void:
	game_manager = get_tree().root.find_child("GameManager", true, false)
	
	# Setup objective label
	label_objective.text = "Objective: Escape the house"
	label_objective.position = Vector2(10, 10)
	label_objective.add_theme_font_size_override("font_size", 24)
	add_child(label_objective)
	
	# Setup lives label
	label_lives.text = "Lives: 3"
	label_lives.position = Vector2(10, 50)
	label_lives.add_theme_font_size_override("font_size", 20)
	add_child(label_lives)
	
	# Setup status label
	label_status.text = "Status: Playing"
	label_status.position = Vector2(10, 90)
	label_status.add_theme_font_size_override("font_size", 16)
	add_child(label_status)
	
	# Connect signals
	if game_manager:
		game_manager.objective_changed.connect(_on_objective_changed)
		game_manager.player_died.connect(_on_player_died)
		game_manager.game_over.connect(_on_game_over)

func _process(_delta: float) -> void:
	# Update status dynamically
	if game_manager:
		if not game_manager.is_playing:
			label_status.text = "Status: Game Over"
		elif game_manager.player_alive:
			label_status.text = "Status: Hunting..."
		else:
			label_status.text = "Status: Dead"

func _on_objective_changed(objective: String) -> void:
	label_objective.text = "Objective: " + objective

func _on_player_died(lives: int) -> void:
	label_lives.text = "Lives: " + str(lives)
	label_status.text = "Status: CAUGHT!"
	
	# Flash effect
	label_status.add_theme_color_override("font_color", Color.RED)
	await get_tree().create_timer(0.5).timeout
	label_status.add_theme_color_override("font_color", Color.WHITE)

func _on_game_over(victory: bool) -> void:
	if victory:
		label_status.text = "ESCAPED! - Press any key to restart"
		label_status.add_theme_color_override("font_color", Color.GREEN)
	else:
		label_status.text = "GAME OVER - Press any key to restart"
		label_status.add_theme_color_override("font_color", Color.RED)
