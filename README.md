# Don't Trust the House - Android Edition

A psychological horror survival game rewritten in Kotlin with Jetpack Compose for Android.

## Overview

The core premise of the original prototype is preserved: **THE HOUSE LEARNS HOW YOU PLAY.**
You are trapped inside a 5-room haunted house (Living Room, Bedroom, Kitchen, Bathroom, Basement). An entity awakens after a brief grace period and hunts you dynamically. Every hide spot you use, every sound you make, and every route you take is recorded and analyzed by the adaptive House Director.

## Core Features & Architecture

### 1. House Director (`core/HouseDirector.kt`)
- **Adaptive Play Style Detection**: Analyzes player habits in real time:
  - `sneaky`: High stealth crouch ratio, frequently utilizes hide spots.
  - `aggressive`: Fast sprinting, rapid escape rushes.
  - `cautious`: Methodical room exploration.
- **Predictive AI**: Predicts the player's favorite hiding spot and directs the monster to search there.
- **Dynamic Difficulty**: Scales monster aggression based on player survival and catch rates.
- **Smart Horror Event Generation**: Triggers psychological scares tailored to the detected play style.

### 2. Monster AI (`core/MonsterAI.kt`)
- **State Machine**: `IDLE` (grace period), `PATROLLING` (room waypoints), `INVESTIGATING` (rushing toward noise origin), `HUNTING` (pursuing line of sight or searching predicted hideouts).
- **Dual Senses**:
  - **Hearing**: Hears loud sprinting footsteps up to 22 units away; crouched stealth footsteps are audible only within close range.
  - **Vision**: Facing cone angle and line-of-sight. Dark (unlit) rooms cut monster vision range in half.
- **Adaptive Intelligence**: Increases speed and pathing efficiency after catches.

### 3. House World Simulation (`core/HouseManager.kt`)
- **5-Room Layout**: Living Room, Bedroom, Kitchen, Bathroom, Basement.
- **Interactive Doors**: Can be opened or closed to obstruct line of sight. Includes the Front Exit Door for escaping outside.
- **Interactive Light Switches**: Toggle room lighting on/off. Darkness reduces monster sight and improves player stealth.
- **Interactive Hide Spots**: Wardrobe closets, under beds, pantry cabinets, shower curtains, and basement crates.

### 4. Horror Event System (`core/EventSystem.kt`)
- Triggers procedural psychological events:
  - `The Entity Awakens`: Initial wake-up growl.
  - `Phantom Footsteps`: Out-of-sync footsteps behind the player.
  - `Dissonant Whispers`: Distorted voice echoing from empty rooms.
  - `False Exit`: Illusion of exit door opening.
  - `Power Surge`: Violent light flickering and buzzing.
  - `Spatial Distortion`: Teleport displacement scare.

### 5. Procedural Horror Audio & Haptics Engine (`audio/HorrorAudioEngine.kt`)
- Synthesizes real-time sound effects using Android's `AudioTrack` API (PCM synthesis):
  - Dynamic footsteps (quiet vs loud).
  - Proximity-based heartbeat pulses (accelerates as the entity draws close).
  - Monster wake-up growls and spotted screech stingers.
  - Creaking doors, whisper ambience, and static surges.
  - Synchronized Android haptic vibrations for jump-scare catches and heartbeats.

### 6. User Interface & Controls (`ui/`)
- **2.5D Tactical Architectural Canvas** (`ui/components/HouseCanvasView.kt`):
  - Smooth camera tracking, room bounds, wall portals, lighting shadows, noise wave ripples, monster red vision cones, and horror vignette pulses.
- **Ergonomic Touch Controls**:
  - 360-degree virtual analog joystick (`ui/components/VirtualJoystick.kt`).
  - Action button for contextual interactions (doors, hide spots, switches, exit).
  - Stealth / Crouch toggle with real-time decibel noise meter.
- **Director AI Telemetry Inspector** (`ui/components/DirectorDebugDialog.kt`):
  - In-game telemetry monitor (equivalent to original F1–F5 debug keys) showing detected play style, favorite rooms, predicted hideouts, monster intelligence, and game state.
- **Game Over & Victory Modals**:
  - Post-run analytics and stats.
  - Replay button with randomized door layout for high replayability.

## Build Requirements
- Android SDK 36
- Java 21
- Jetpack Compose & Material 3
