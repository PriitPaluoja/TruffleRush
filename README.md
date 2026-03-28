# TruffleRush

A grid-based top-down foraging game built with pure JavaFX. You control a pig competing against three AI rivals to become the heaviest by collecting truffles. Every round ends with a climactic Golden Truffle event.

## Requirements

- Java 21+
- Gradle (wrapper included)

## Running

```bash
./gradlew run
```

## How to Play

| Input | Action |
|-------|--------|
| Arrow keys | Move pig one cell |
| Space | Sniff — reveals hidden items within 2 cells for 2 seconds (8s cooldown) |

**Goal:** Be the heaviest pig when the 3-minute round ends, or reach 150 kg first.

Your pig loses weight passively over time (−0.05 kg/tick), so no lead is ever safe.

## Items

| Item | Weight Change | Notes |
|------|--------------|-------|
| Black Truffle | +8 kg | Common |
| White Truffle | +15 kg | Rare |
| Common Mushroom | +3 kg | Very common |
| Acorn | +1 kg | Abundant |
| Celery | −5 kg | Hazard |
| Diet Pill | −10 kg | Rare hazard |
| Mud Splash | 0 kg | Slows movement for 3 seconds |
| **Golden Truffle** | **+30 kg** | Spawns once in the final 30% of the round |

Items hidden under bushes are invisible until a pig moves adjacent to them.

## AI Rivals

| Pig | Colour | Behaviour |
|-----|--------|-----------|
| Dumb | Pink | Wanders randomly |
| Cunning | Brown | BFS-navigates to the highest-value item; avoids hazards |
| Ruthless | Dark grey | Aggressively BFS-routes around other pigs; races for the Golden Truffle when it's not the heaviest pig |

## Golden Truffle

- Spawns exactly once per round, at a random moment in the final 30% of the round
- An expanding gold pulse announces its location to all pigs simultaneously
- Despawns after 8 seconds if uncollected
- Never spawns adjacent to the current heaviest pig

## Obstacles

| Obstacle | Effect |
|----------|--------|
| Rock | Impassable |
| Bush | Impassable; hides items underneath until a pig is adjacent |
| Mud Pit | Passable, but halves movement speed while crossing |
| Fence | Impassable; forms corridors |

Maps are procedurally generated with a seeded algorithm. Flood-fill validation ensures no isolated regions exist.

## Weather

Weather cycles in order: **Sunny → Overcast → Rain → Fog**, with ~3-second interpolated transitions.

| Weather | Visual | Gameplay Effect |
|---------|--------|----------------|
| Sunny | Warm yellow tint | Baseline |
| Overcast | Grey tint | Item spawn rate +20% |
| Rain | Blue-grey tint + falling lines | All pigs move at half speed |
| Fog | White overlay with visibility holes | Each pig sees only a 4-cell radius; AI recomputes less often |

## Time of Day

The grid colour shifts continuously as the round progresses — Dawn → Day → Dusk → Night — serving as a visual round timer. No numeric countdown is needed.

## Architecture

```
com.example.demo
├── core/        EventBus, GameEvent
├── world/       GameMap, Cell, Obstacle, MapGenerator, FloodFill,
│                WeatherSystem, Weather, TimeOfDay
├── entity/      Pig, PlayerPig, AIPig, Direction,
│                PigBehavior, RandomBehavior, CunningBehavior, RuthlessBehavior
├── item/        Item, ItemType, ItemSpawner, GoldenTruffleManager
├── util/        BFS, FloodFill
└── render/      GridRenderer, PigRenderer, ShapeFactory, ItemRenderer,
                 ObstacleRenderer, WeatherRenderer, GoldenTruffleRenderer,
                 SniffRenderer, HudRenderer, RoundEndOverlay
```

Key design rules:
- **Single `AnimationTimer`** drives all game logic at 60 fps
- **Renderers read state, never modify it** — game logic never touches JavaFX nodes directly
- **Grid-only positions** — pigs and items use integer `(col, row)` coordinates
- **No images or FXML** — every visual element is a JavaFX shape primitive (`Circle`, `Rectangle`, `Polygon`, `Line`, `Ellipse`) or a group of them
- **Strategy pattern** for AI — swap behaviours by implementing `PigBehavior`

## Build

```bash
./gradlew build   # compile and test
./gradlew run     # launch the game
```
