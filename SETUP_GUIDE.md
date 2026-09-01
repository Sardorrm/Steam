## SETUP GUIDE: Don't Trust The House - Milestone 1
## How to set up the Godot project and run the prototype

---

## STEP 1: Download Godot 4.1+

Download from: https://godotengine.org/download/

We recommend **Godot 4.x** (latest stable). This project uses Godot's built-in scripting (GDScript).

---

## STEP 2: Open Project in Godot

1. Launch Godot
2. Click **Import** or **Open Project**
3. Navigate to `/workspaces/Steam/`
4. Select the folder
5. Click **Open**

Godot will recognize `project.godot` and load the project.

---

## STEP 3: Project Structure

All scripts are created and organized:

```
res://
├── scripts/
│   ├── core/
│   │   ├── game_manager.gd        (Global game state)
│   │   ├── house_director.gd      (AI learns behavior)
│   │   └── event_system.gd        (Scary moments)
│   ├── player/
│   │   └── player_controller.gd   (Player movement, hiding)
│   ├── monster/
│   │   └── monster_ai.gd          (Monster hunting)
│   ├── world/
│   │   ├── house_manager.gd       (House layout)
│   │   ├── door.gd                (Door mechanics)
│   │   ├── light_switch.gd        (Light toggles)
│   │   └── hide_spot.gd           (Hiding mechanics)
│   └── ui/
│       └── (HUD will be added next)
└── scenes/
    └── (Will create next)
```

---

## STEP 4: Create Main Scene

In Godot Editor:

1. Go to **File** → **New Scene**
2. Root node: **Node3D** (Name: `Main`)
3. Add child: **Camera3D** (Name: `Camera`)
   - Position: (0, 2, 8)
   - Look at player
4. Add child: **DirectionalLight3D** (Name: `Sun`)
   - Rotation: (-45°, -45°, 0°)
   - Energy: 2.0
5. Add child: **Node3D** (Name: `GameSystems`)
   - Add child: **Node** → Attach script `game_manager.gd`
   - Add child: **Node** → Attach script `house_director.gd`
   - Add child: **Node** → Attach script `event_system.gd`
   - Add child: **Node3D** → Attach script `house_manager.gd`
6. Add child: **CharacterBody3D** (Name: `Player`)
   - Position: (0, 1, 0)
   - Add collision: **CollisionShape3D** with **CapsuleShape3D**
   - Attach script `player_controller.gd`
7. Add child: **CharacterBody3D** (Name: `Monster`)
   - Position: (5, 1, 5)
   - Add collision: **CollisionShape3D** with **CapsuleShape3D**
   - Attach script `monster_ai.gd`

**Save scene as:** `res://scenes/main.tscn`

---

## STEP 5: Add Interactive Objects

### Add Doors

1. Create **Node3D** under Main (Name: `Door_Bedroom`)
   - Position: (9, 1, 0)
   - Attach script: `door.gd`
   - Set Export Variables:
     - `door_name`: "Bedroom Door"
     - `leads_to_room`: "bedroom"
     - `is_exit`: false

2. Create **Node3D** under Main (Name: `Door_Exit`)
   - Position: (15, 1, 0)
   - Attach script: `door.gd`
   - Set Export Variables:
     - `door_name`: "Exit"
     - `leads_to_room`: "outside"
     - `is_exit`: true

### Add Hide Spots

1. Create **Node3D** (Name: `Closet_Bedroom`)
   - Position: (11, 1, -3)
   - Attach script: `hide_spot.gd`
   - Set Export Variables:
     - `hide_name`: "Bedroom Closet"
     - `room_name`: "bedroom"

2. Create **Node3D** (Name: `UnderBed_Bedroom`)
   - Position: (11, 0.5, 2)
   - Attach script: `hide_spot.gd`
   - Set Export Variables:
     - `hide_name`: "Under Bed"
     - `room_name`: "bedroom"

### Add Light Switches

1. Create **Node3D** (Name: `Switch_Living`)
   - Position: (-3, 1.5, 0)
   - Attach script: `light_switch.gd`
   - Set `controls_room`: "living_room"

---

## STEP 6: Test Run

1. Click **Play** (F5 or Play button)
2. You should see:
   - Player standing in the middle
   - Monster standing to the side
   - Basic 3D environment

3. Test Controls:
   - **WASD** = Move
   - **E** = Interact with doors/switches/hide spots
   - **Shift** = Crouch (reduces noise)
   - **F1-F5** = Debug keys (shows state info)

---

## STEP 7: Debug Keys

- **F1** = GameManager state
- **F2** = HouseDirector analysis
- **F3** = Player state
- **F4** = Monster state
- **F5** = House layout

---

## STEP 8: Next Steps

After testing the basic setup:

1. **Add more hide spots** in different rooms
2. **Create door animations** (more visual feedback)
3. **Add audio** (footsteps, monster growl, scary sounds)
4. **Improve monster pathfinding** (use NavMesh)
5. **Add HUD** (show lives, objective, health)
6. **Create simple visual scenes** for each room

---

## TROUBLESHOOTING

**"Error: Can't find script"**
- Make sure scripts are in `res://scripts/` with correct paths
- Check script attachment paths in Godot inspector

**Player/Monster not moving**
- Verify CollisionShape3D is set correctly
- Check CharacterBody3D setup (must have collision shape)
- Make sure script is attached and code has no syntax errors

**Monster not hunting**
- Check that `game_manager.start_game()` is being called
- Verify monster is a CharacterBody3D with collision
- Monitor with F4 debug key

**Doors not interacting**
- Verify door.gd is attached
- Player must press E within 2 units of door
- Check console for error messages

---

## PERFORMANCE TIPS FOR MILESTONE 1

- Limit draw calls (keep models simple)
- Use baked lighting where possible
- Profile with Godot's profiler (Debug → Monitor)
- Aim for 60fps minimum

---

## CUSTOM GODOT PROJECT SETTINGS

We've set up essential input mapping in `project.godot`:
- move_forward = W
- move_backward = S
- move_left = A
- move_right = D
- interact = E
- crouch = Shift

You can edit in **Project → Project Settings → Input Map**

