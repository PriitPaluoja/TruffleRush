# TruffleRush — JavaFX Grid Game

## Project Overview
A grid-based top-down foraging game where a player pig competes against 3 AI rivals to collect truffles and become the heaviest pig. Features a full level progression system, scoring, lose conditions, random events, power-ups, and a main menu. Built with pure JavaFX shape primitives (no images).

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
| `core/` | GameSession, ScoreEvent, RandomEventManager, GameState, EventBus |
| `world/` | GameMap (25×17 grid), MapGenerator (density scaling), WeatherSystem, TimeOfDay |
| `entity/` | Pig, PlayerPig, AIPig, Wolf, Farmer — AI behaviors (Strategy pattern), Direction |
| `item/` | Item types (incl. power-ups), ItemSpawner, GoldenTruffleManager, SuperAcornManager |
| `util/` | BFS pathfinding, FloodFill connectivity check |
| `render/` | GridRenderer, HudRenderer, SidePanelRenderer, PigRenderer, ItemRenderer, WolfRenderer, FarmerRenderer, MainMenuOverlay, RoundEndOverlay, GameOverOverlay, WeatherRenderer |

## Key Architecture Rules
- **Single AnimationTimer** drives all game logic at 60fps
- **GameSession** is the persistent state across levels (level, score, static highScore, alive flag)
- **Game logic never touches JavaFX nodes** — only renderers create/update shapes
- **Grid-only positions** — pigs and items use integer (col, row), no sub-cell floats
- **EventBus** for decoupled communication: itemCollected, weightChanged, goldenTruffleSpawned, roundEnded
- **WeatherEffect** only modifies map state and VisibilityMask, never pig/item logic directly
- **BFS utility** is stateless and reusable
- **AI uses Strategy pattern**: `PigBehavior` interface with `Direction nextMove(AIPig, GameMap, List<Pig>)`

## Game Constants
- Grid: 25 columns × 17 rows, tile size 40px
- Game window: 1120×680px (1000px grid + 120px side panel)
- Level time: `max(5400, 10800 - (level-1) * 900)` ticks — 15s less per level, floor 90s
- Starting weight: 50.0 kg, minimum: 10.0 kg
- Weight decay: `0.008 + (level-1) * 0.001` kg/tick
- AI move interval: `max(8, 15 - level)` ticks
- Obstacle density multiplier: `1.0 + (level-1) * 0.15`
- Pig visual radius: clamp(12 + weight × 0.15, 12, 28)

## Level Progression and Scoring
- Rounds are called "levels" — surviving a level advances to the next
- **Lose conditions**: weight ≤ 10 kg (starvation), wolf catches player, farmer catches player
- **Score events** (ScoreEvent enum): ACORN(10), MUSHROOM(30), BLACK_TRUFFLE(80), WHITE_TRUFFLE(150), GOLDEN_TRUFFLE(300), WOLF_STUNNED(200), FARMER_ESCAPED(500), LEVEL_COMPLETE(500×level), WEIGHT_BONUS(finalWeight×5), SURVIVAL_TICK(2 per second)
- High score persists for the session (GameSession.getHighScore())

## Power-ups (PlayerPig)
| Item | Effect | Duration |
|------|--------|----------|
| SPEED_MUSHROOM | 2× movement speed | 300 ticks (5s) |
| SHIELD_ACORN | Block next hit (wolf/farmer) | Until hit |
| MAGNET_TRUFFLE | Pull items within 3 cells | 360 ticks (6s) |
| DECOY_MUSHROOM | Lure AI pigs with fake golden truffle | 300 ticks (5s) |
| SUPER_ACORN | Invincible, double speed, scares wolf | 480 ticks (8s) |

Power-up state lives on PlayerPig: `tickPowerUps()` decrements timers each game tick.

## Random Events (RandomEventManager)
Checked every 600 ticks; trigger chance: `30 + level×5` capped at 70%.

| Event | Min Level | Effect | Duration |
|-------|-----------|--------|----------|
| Wolf Attack | 2 | Wolf spawns at edge, BFS-chases nearest pig | ~600 ticks |
| Farmer Raid | 4 | Farmer chases player; escape hole spawns | ~480 ticks |
| Truffle Rain | 1 | Extra truffle spawns every 20 ticks | 300 ticks |
| Mud Storm | 3 | Mud slow applied to all pigs | 240 ticks |
| Pig Stampede | 5 | 2 extra AI pigs for 480 ticks | 480 ticks |
| Frenzy Mode | 1 | Double all item weight deltas | 300 ticks |

## Navigation Flow
1. `start()` → `showMainMenu(stage)` — displays MainMenuOverlay
2. "Start Game" → creates `GameSession`, calls `startLevel(stage)`
3. Level ends: level++ → `startLevel(stage)` again (RoundEndOverlay "Next Level")
4. Game over: `session.endGame(reason)` → GameOverOverlay → "Main Menu" → `showMainMenu(stage)`

## Module System
All new packages must be exported in `module-info.java`. No FXML opens needed.
