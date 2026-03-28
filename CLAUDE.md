# TruffleRush — JavaFX Grid Game

## Project Overview
A grid-based top-down foraging game where a player pig competes against 3 AI rivals to collect truffles and become the heaviest pig. Built with pure JavaFX shape primitives (no images).

## Tech Stack
- **Java 25** with **JavaFX 21**
- **Gradle Kotlin DSL** build system
- Java module system (`com.example.demo`)
- No external game libraries, no FXML

## Build & Run
```bash
./gradlew run
```

## Project Structure
Base package: `com.example.demo`

| Package | Purpose |
|---------|---------|
| `core/` | GameEngine (AnimationTimer), GameState, EventBus |
| `world/` | GameMap (20×15 grid), MapGenerator, WeatherSystem, TimeOfDay |
| `entity/` | Pig classes, AI behaviors (Strategy pattern), Direction |
| `item/` | Item types, ItemSpawner, GoldenTruffleManager |
| `util/` | BFS pathfinding, FloodFill connectivity check |
| `render/` | All JavaFX rendering: ShapeFactory, GridRenderer, HUD, overlays |

## Key Architecture Rules
- **Single AnimationTimer** drives all game logic at 60fps
- **GameState** is the single source of truth — renderers read it, never modify it
- **Game logic never touches JavaFX nodes** — only renderers create/update shapes
- **Grid-only positions** — pigs and items use integer (col, row), no sub-cell floats
- **EventBus** for decoupled communication: itemCollected, weightChanged, goldenTruffleSpawned, roundEnded
- **WeatherEffect** only modifies map state and VisibilityMask, never pig/item logic directly
- **BFS utility** is stateless and reusable
- **AI uses Strategy pattern**: `PigBehavior` interface with `Direction nextMove(AIPig, GameMap, List<Pig>)`

## Game Constants
- Grid: 20 columns × 15 rows, tile size 40px, canvas 800×600px
- Round duration: 3 minutes (10,800 ticks at 60fps)
- Starting weight: 50.0 kg, minimum: 10.0 kg, win threshold: 150 kg
- Weight decay: −0.05 kg/tick
- Pig visual radius: clamp(12 + weight × 0.15, 12, 28)

## Implementation Order
Follow the 13-step sequence in the plan file. Each step must compile and run before proceeding to the next. Do not skip or merge steps.

## Module System
All new packages must be exported in `module-info.java`. No FXML opens needed.
