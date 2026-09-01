## DEVELOPER QUICK REFERENCE
## Fast lookup for common tasks

---

## CONTROLS

| Key | Action |
|-----|--------|
| W/A/S/D | Move |
| Shift | Crouch (quiet) |
| E | Interact (doors, switches, hide) |
| F1 | GameManager debug |
| F2 | HouseDirector debug |
| F3 | Player debug |
| F4 | Monster debug |
| F5 | House layout debug |

---

## GAME FLOW

1. **Player spawns** at (0, 1, 0)
2. **5 seconds pass** → Monster wakes up
3. **Monster hunts** → Player tries to escape
4. **Caught 3 times** → Game Over (loss)
5. **Reach exit** → Victory
6. **Any outcome** → Restart scene

---

## ADDING NEW CONTENT

### Add a New Room
```gdscript
# In HouseManager._initialize_house()
rooms["new_room"] = {
    "center": Vector3(x, y, z),
    "size": Vector3(width, height, depth),
}
connections["new_room"] = ["room1", "room2"]
```

### Add a Door
```gdscript
# In main.tscn, create Node3D
# Name: Door_Example
# Position: (x, y, z)
# Script: door.gd
# Inspector:
#   door_name = "Example Door"
#   leads_to_room = "target_room"
#   is_exit = false (or true if exits house)
```

### Add a Hide Spot
```gdscript
# In main.tscn, create Node3D
# Name: HideSpot_Example
# Position: (x, y, z)
# Script: hide_spot.gd
# Inspector:
#   hide_name = "Under Bed"
#   room_name = "bedroom"
```

### Add a Light Switch
```gdscript
# In main.tscn, create Node3D
# Name: LightSwitch_Living
# Position: (x, y, z)
# Script: light_switch.gd
# Inspector:
#   switch_name = "Living Room Light"
#   controls_room = "living_room"
```

### Trigger a Horror Event
```gdscript
# In any script, get EventSystem and call:
var event_system = get_tree().root.find_child("EventSystem", true, false)
event_system.play_event("hear_own_voice")

# Available events:
# - "monster_awakens"
# - "hear_own_voice"
# - "hear_own_footsteps"
# - "false_exit"
# - "teleport_scare"
# - "lights_flicker"
# - "generic_scare"
```

---

## COMMON BUGS & FIXES

### Player won't move
- [ ] Check PlayerController script is attached
- [ ] Verify CollisionShape3D exists (CapsuleShape3D)
- [ ] Check physics frames per second (Project → Project Settings → Physics)

### Monster doesn't hunt
- [ ] Verify MonsterAI script attached
- [ ] Check game_manager.start_game() is called
- [ ] Verify monster is CharacterBody3D with collision
- [ ] Watch F4 debug output

### Doors don't work
- [ ] Check Door script attached
- [ ] Player must be within 2 units to interact
- [ ] Check nearest_interactive is updating in player script
- [ ] Verify E key is bound (Project → Input Map)

### Monster walks through walls
- [ ] Add CollisionShape3D to all rooms (walls)
- [ ] Use NavMesh for pathfinding in Milestone 2

---

## PERFORMANCE CHECKLIST

Before each build:

- [ ] Profile with Godot Monitor (Debug → Monitor)
- [ ] Check FPS (should be 60fps locked)
- [ ] Check draw calls (aim for <100)
- [ ] Check memory usage (aim for <500MB)
- [ ] Test on low-end hardware simulation

---

## SIGNAL REFERENCE

### GameManager signals
```gdscript
game_manager.player_died.connect(func(lives): ...)
game_manager.objective_changed.connect(func(obj): ...)
game_manager.game_over.connect(func(victory): ...)
game_manager.monster_spotted.connect(func(): ...)
```

### Player signals
```gdscript
player.player_moved.connect(func(pos, vel): ...)
player.player_hiding.connect(func(hiding): ...)
player.footstep_made.connect(func(is_loud): ...)
```

### Door signals
```gdscript
door.door_opened.connect(func(): ...)
door.door_closed.connect(func(): ...)
```

### LightSwitch signals
```gdscript
light.lights_toggled.connect(func(is_on): ...)
```

### HideSpot signals
```gdscript
hide_spot.player_hidden.connect(func(): ...)
hide_spot.player_revealed.connect(func(): ...)
```

### EventSystem signals
```gdscript
event_system.horror_event_triggered.connect(func(event_type): ...)
```

---

## EXPORT VARIABLES REFERENCE

### Player (`player_controller.gd`)
```gdscript
@export var move_speed: float = 5.0       # Normal movement speed
@export var crouch_speed: float = 2.5     # Crouch movement speed
@export var acceleration: float = 0.2     # Smoothing
@export var friction: float = 0.1         # Deceleration
@export var interaction_range: float = 2.0
@export var footstep_interval_normal: float = 0.4
@export var footstep_interval_crouch: float = 0.8
```

### Monster (`monster_ai.gd`)
```gdscript
@export var patrol_speed: float = 2.0
@export var hunt_speed: float = 6.0
@export var acceleration: float = 0.15
@export var hearing_range: float = 20.0
@export var vision_range: float = 15.0
@export var vision_angle: float = 90.0
```

### Door (`door.gd`)
```gdscript
@export var door_name: String = "Door"
@export var leads_to_room: String = "living_room"
@export var is_exit: bool = false
@export var is_locked: bool = false
@export var interaction_range: float = 2.0
```

---

## TESTING SCENARIOS

### Scenario 1: Direct Escape
1. Run game
2. Sprint to exit door
3. Expected: Reach exit before monster (60% success)

### Scenario 2: Hide & Sneak
1. Run game
2. Find hide spot immediately
3. Monster should not find you immediately
4. Expected: Successfully hide for >20 seconds

### Scenario 3: Learning Mechanic
1. Play 3 times
2. Die in same location each time
3. 4th playthrough: Monster should check that location first
4. Expected: Monster hunting at known death spots

### Scenario 4: Horror Events
1. Play 10 times
2. Look for at least 3 unique horror events
3. Expected: Variety of scares, not repetitive

---

## RELEASE CHECKLIST (Milestone 1)

- [ ] All 8 core systems working
- [ ] No crashes after 30 minutes play
- [ ] No stuck players (always path to exit)
- [ ] No infinite loops
- [ ] F1-F5 debug keys available
- [ ] Code compiles without warnings
- [ ] README.md complete
- [ ] SETUP_GUIDE.md complete
- [ ] ARCHITECTURE.md complete
- [ ] Git commit: "Milestone 1 Complete"

---

## GIT WORKFLOW

```bash
# Create branch for new feature
git checkout -b feature/feature-name

# Work, then commit
git add scripts/
git commit -m "Add feature description"

# When ready to merge
git checkout main
git merge feature/feature-name

# Push to GitHub
git push origin main
```

---

## NEXT STEPS AFTER MILESTONE 1

### Immediate (1-2 days)
- [ ] Playtest with friends
- [ ] Collect feedback on core loop
- [ ] Fix any game-breaking bugs
- [ ] Optimize if FPS < 60

### Short-term (1 week)
- [ ] Design audio asset list (Milestone 2)
- [ ] Plan room layouts (Milestone 2)
- [ ] Sketch UI mockups (Milestone 2)
- [ ] Start asset hunting/creation

### Medium-term (2 weeks)
- [ ] Begin Milestone 2 (House Content)
- [ ] Add audio system
- [ ] Add 3D models for rooms
- [ ] Improve visuals

### Long-term
- [ ] Plan multiplayer architecture (Milestone 6)
- [ ] Design story/narrative (Milestone 9)
- [ ] Plan Steam release strategy

---

## USEFUL GODOT SHORTCUTS

| Shortcut | Action |
|----------|--------|
| F5 | Play scene |
| F6 | Play from cursor |
| F7 | Break |
| F8 | Continue |
| Ctrl+B | Toggle breakpoint |
| Ctrl+D | Duplicate node |
| Ctrl+Shift+D | Duplicate resource |
| Ctrl+S | Save |
| Ctrl+Z | Undo |
| Ctrl+Y | Redo |

---

## CONTACTS & RESOURCES

### Godot Documentation
- https://docs.godotengine.org/
- GDScript reference
- Node & Physics docs

### Game Design
- Original Design Document (TBD)
- Milestone roadmap in ARCHITECTURE.md

### Development Standards
- Code style: Consistent indentation (tabs)
- Naming: snake_case for functions, PascalCase for classes
- Comments: For non-obvious logic only
- Signals: For cross-system communication

---

**STATUS: MILESTONE 1 PROTOTYPE COMPLETE** ✅
**NEXT: Playtest and gather feedback**

