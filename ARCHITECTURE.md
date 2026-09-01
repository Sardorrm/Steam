## DON'T TRUST THE HOUSE - First Prototype
## Architecture & Systems Overview

### PROJECT STRUCTURE

```
res://
├── scenes/
│   ├── main.tscn (Main game scene)
│   ├── rooms/ (House layout)
│   │   ├── living_room.tscn
│   │   ├── bedroom.tscn
│   │   ├── kitchen.tscn
│   │   └── bathroom.tscn
│   ├── player/
│   │   └── player.tscn
│   ├── monster/
│   │   └── monster.tscn
│   ├── ui/
│   │   └── hud.tscn
│   └── objects/
│       ├── door.tscn
│       └── light_switch.tscn
│
├── scripts/
│   ├── core/
│   │   ├── game_manager.gd (Global state, objective tracking)
│   │   ├── house_director.gd (AI learns behavior, generates events)
│   │   └── event_system.gd (Horror events & surprises)
│   ├── player/
│   │   ├── player_controller.gd (Movement, interaction)
│   │   └── player_state.gd (Health, status)
│   ├── monster/
│   │   ├── monster_ai.gd (Behavior, hunting logic)
│   │   └── monster_senses.gd (Hearing, vision)
│   ├── world/
│   │   ├── house_manager.gd (Room connections, doors, lights)
│   │   ├── door.gd (Door logic)
│   │   └── light_switch.gd (Toggle lights)
│   └── ui/
│       └── hud.gd (Health display, objective)
│
└── assets/ (placeholder for models, audio, textures)
```

### CORE GAMEPLAY LOOP (MILESTONE 1)

1. **Player spawns** in living room
2. **Objective**: Escape the house (reach exit)
3. **Monster hunts**: Simple AI, learns if player hides
4. **One horror event**: Player hears their own voice
5. **Win condition**: Exit the house
6. **Loss condition**: Monster catches player 3 times (lives)

### KEY SYSTEMS

**GameManager**
- Tracks player objective
- Tracks lives/deaths
- Handles win/lose state
- Spawns monster after 5 seconds

**HouseDirector** (Early version)
- Records: Where player hides, which doors used, where caught
- Generates one random event (scary audio)
- Plans: Will evolve to predictive AI

**PlayerController**
- WASD movement
- E to interact (open doors, hide)
- Crouching reduces noise
- Can hide in closets/under beds

**MonsterAI**
- Patrols rooms
- Hears player noise
- Pathfinds to player
- Learns hide spots after catching player once

**EventSystem**
- Plays creepy audio cues
- "You hear a voice... is it yours?"
- "Footsteps that don't match your movement"

### TECHNICAL PRIORITIES FOR MILESTONE 1

1. **Performance** - Runs 60fps on modest hardware
2. **Feel** - Movement is responsive, horror moments are effective
3. **Replayability** - Rooms randomized, monster behavior varies
4. **Clean code** - Modular, easy to extend
5. **Streaming** - Moments that create tension/scares

### WHAT WE'RE NOT DOING YET

- Multiplayer (will add in milestone 6)
- Complex story (will add in milestone 9)
- 50+ rooms (just 5 rooms for now)
- Advanced graphics (focus on gameplay)
- Content beyond one escape scenario

### SUCCESS CRITERIA

When milestone 1 is done:

- [ ] Can compile and run in Godot 4.x
- [ ] Player can move, open doors, hide
- [ ] Monster hunts intelligently
- [ ] At least 3 playthroughs feel different
- [ ] One scary moment per playthrough
- [ ] Can escape the house in ~5-10 minutes
- [ ] Code is clean, commented, modular
- [ ] Ready to stream on Twitch
- [ ] Friends say "That was genuinely unsettling"

