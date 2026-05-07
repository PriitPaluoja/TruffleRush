# TruffleRush

A grid-based top-down foraging arcade-roguelite built with pure JavaFX. You play a pig competing against three AI rivals — Hoggart, Whiskers, and Bramble — to become the heaviest pig before the round ends. Pure shape primitives, no images, no FXML.

## Requirements

- Java 21+
- Gradle (wrapper included)

## Running

```bash
# Windows
gradlew run
# macOS / Linux
./gradlew run
```

## Controls

| Input | Action |
|-------|--------|
| Arrow keys | Move pig one cell |
| Space | Sniff — reveals hidden items in a radius for 2 seconds (8 s cooldown) |
| Esc | Pause / open settings |

## Goal

Reach the level's win-weight (150 kg) or be the heaviest when the timer runs out. Lose conditions: weight ≤ 10 kg, wolf catches you, farmer catches you, or finishing last place.

Each cleared level advances the run; you eventually reach the **level-10 victory** (Truffle King), then choose to bank or continue into Endless mode.

## Run loop

```
Main Menu → Cellar (shop)
   ↓
Apply perks  →  Pick Heat (after first level-10 clear)
   ↓
Level 1, 2, …
   ↓ (between levels)
Boon picker  →  Combo discovery
   ↓
Level 10  →  TRUFFLE KING victory  →  Continue (Endless) / Bank Run
   ↓
Game Over  →  Run Summary  →  Main Menu (truffles banked)
```

## Items

| Item | Weight | Effect |
|------|--------|--------|
| Acorn | +1 kg | Cheap snack |
| Common Mushroom | +3 kg | Worth chasing |
| Black Truffle | +5 kg | High score |
| White Truffle | +8 kg | Rare, high score |
| **Golden Truffle** | **+30 kg** | One-shot per round, broadcast pulse, 8 s window |
| **Super Acorn** | massive | Level 2+, makes you Super Pig (invincible, 2× speed) |
| Speed Mushroom | — | 2× speed for 5 s |
| Greater Speed (rare) | — | 2× speed for 10 s |
| Magnet Truffle | — | Pulls items in for 6 s |
| Magnet Crown (rare) | — | Pulls items in for 12 s |
| Shield Acorn | — | Block next hit |
| Decoy Mushroom | — | Lures AI rivals with a fake golden truffle |
| Mud Splash | hazard | Slows you, breaks combo |
| Celery / Diet Pill | hazard | Weight loss |

Items hidden under bushes are invisible until a pig walks adjacent. Sniffing reveals them in a small radius.

## AI rivals

| Pig | Colour | Strategy |
|-----|--------|----------|
| Hoggart | Pink | Always heads for the nearest visible item |
| Whiskers | Brown | Chases value, flees from wolves within 6 cells |
| Bramble | Dark grey | Bullies the player when it outweighs you, otherwise drifts to the centre |

When a rival snags a truffle, you'll hear about it: each pig has its own voice in the side-panel event log.

## Combo meter

Snappy back-to-back pickups stack a combo. Tier scales score (not weight):

| Combo | Multiplier |
|-------|------------|
| 1–2 | ×1.0 |
| 3–4 | ×1.25 |
| 5–7 | ×1.5 |
| 8+ | ×2.0 |

Combos break on hits or after ~1.5 seconds without a pickup. Particles tint warmer as the tier rises.

## Boons (run-modifying)

After every cleared level, pick 1 of 3 random boons. Choices stack across the run.

| Boon | Effect |
|------|--------|
| Truffle Hunter | Truffles 2×, acorns/mushrooms 0 |
| Glutton | No decay, 2× wolf chance |
| Pacifist | No farmer, weight cap 120 |
| Greedy Heart | Items +50%, hazards halved |
| Swift Hooves | +25% speed, decay +50% |
| Sharp Nose | Magnet range −1 (combo-relevant) |

### Hidden combos

Holding two specific boons unlocks one of 8 hidden synergies (e.g. *Black Truffle Banquet*, *Glass Cannon*, *Iron Belly*, *Wind-Walker*). Combos fire silently with a side-panel banner; first discovery unlocks the **Synergist** achievement, all 8 unlocks **Combo Connoisseur**.

## Permanent perks (Truffle Cellar)

Spend banked truffles between runs. Persist across sessions in `~/.trufflerush/meta.txt`.

| Perk | Effect | Levels |
|------|--------|--------|
| Well Fed | +5 starting weight per level | 3 |
| Iron Stomach | −0.001 decay per level | 3 |
| Lucky Acorn | Start each level with a shield | 1 |
| Big Snout | +1 magnet range per level | 2 |
| Slow Starter | Skip decay for first 5 s of each level | 1 |

`respec()` refunds 70 % of spent truffles.

## Heat / Ascension ladder

Unlocks after the first level-10 clear. Pick Heat 0 to 6 in the shop (`+` / `−` buttons). Each level adds one stacking modifier on top of the last:

| Heat | Modifier |
|------|----------|
| 1 | Lean Pickings (items −15 %) |
| 2 | Hungry Pack (rivals 15 % faster) |
| 3 | No Safety Net (start at 45 kg) |
| 4 | Shorter Fuse (timer × 0.9) |
| 5 | Crowded Field (+20 % obstacles) |
| 6 | Drained Bank (Lucky Acorn disabled) |

Heat scales banked truffles by `1 + 0.15 × heat`. Daily runs always force Heat 0.

## Endless mode

Beating level 10 once flips the run into endless mode. Decay scales `1.05^(level − 10)`, lore drops every 5 endless levels, and your deepest endless level is shown on the main menu (`Endless Ante reached: L<x>`). Click Bank Run on the victory screen to lock in your truffles, or keep going.

## Daily run

The shop's "Today's Daily Run" button uses today's epoch-day as the seed. Heat is forced to 0 and the best score for the day is recorded in `meta.txt`. Friends running the same day get the same map, item drops, and event sequence.

## Random events

Every ~5 s, an event may trigger:

| Event | From level | Effect |
|-------|-----------|--------|
| Wolf Attack | 2 | Wolf BFS-chases the nearest pig |
| Farmer Raid | 4 | Farmer chases you; an escape hole appears |
| Truffle Rain | 1 | Bonus truffle spawns |
| Mud Storm | 3 | All pigs slowed |
| Frenzy | 1 | All weight deltas doubled |

Three risk-zone tiles per level visibly mark wolf dens — standing on one accelerates the next wolf spawn.

## Biomes & elite levels

Every 3 levels the biome cycles **Forest → Swamp → Farm**, shifting which obstacle types dominate the map. Every 5th level is an **elite level**: ARENA (no item spawns + aggressive wolves), SWARM (6× item spawns), or GAUNTLET (denser map + faster rivals). Elites award a 2× completion bonus.

## Weather

Cycles **Sunny → Overcast → Rain → Fog**, with smooth transitions:

| Weather | Effect |
|---------|--------|
| Sunny | Baseline |
| Overcast | +20 % item spawns |
| Rain | All pigs at half speed |
| Fog | Visibility limited to a small radius around each pig |

The grid colour also shifts continuously (Dawn → Day → Dusk → Night) as a visual round timer.

## Audio

All SFX are PCM tones synthesized at startup via `javax.sound.sampled` — no external audio files. The pause menu's master-volume slider applies live.

## Pause menu

Esc opens the pause overlay during a level. Toggle screen shake and hit-stop, drag the volume slider, or quit to the main menu (abandons the run, no rewards). All settings persist.

## Architecture

```
com.example.demo
├── core/        GameSession, MetaProgression, AudioManager, RandomEventManager,
│                Achievement, AchievementTracker, Perk, Boon, BoonCombo,
│                HeatModifier, Flavor, LevelType, ScoreEvent, EventBus
├── world/       GameMap, Cell, Obstacle, Biome, MapGenerator,
│                WeatherSystem, Weather, TimeOfDay
├── entity/      Pig, PlayerPig, AIPig, Wolf, Farmer, Direction,
│                PigBehavior, HoggartBehavior, WhiskersBehavior, BrambleBehavior
├── item/        Item, ItemType, ItemSpawner, GoldenTruffleManager, SuperAcornManager
├── util/        BFS, FloodFill
└── render/      GridRenderer, PigRenderer, ItemRenderer, ObstacleRenderer,
                 WeatherRenderer, GoldenTruffleRenderer, SniffRenderer,
                 WolfRenderer, FarmerRenderer, EffectsRenderer,
                 HudRenderer, SidePanelRenderer,
                 MainMenuOverlay, ShopOverlay, BoonOverlay, RoundEndOverlay,
                 RunSummaryOverlay, GameOverOverlay, PauseOverlay
```

Key design rules:
- **Single `AnimationTimer`** drives everything at 60 fps
- **Renderers read state, never modify it** — the simulation never touches JavaFX nodes directly
- **Grid-only positions** — pigs and items use integer `(col, row)` coordinates
- **No images, no FXML** — every visual is a JavaFX shape primitive (`Circle`, `Rectangle`, `Polygon`, `Line`, `Ellipse`); the only `Control` is the shop's pig-name `TextField`
- **Strategy pattern** for AI — swap behaviours by implementing `PigBehavior`
- **Forwards-compatible saves** — `~/.trufflerush/meta.txt` is plain key=value lines; missing keys default cleanly so older saves load without breaking

## Build

```bash
./gradlew build   # full build
./gradlew run     # launch the game
```

Save file location: `~/.trufflerush/meta.txt`.
