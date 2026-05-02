# TruffleRush — JavaFX Grid Game

## Project Overview
A grid-based top-down foraging game where a player pig competes against 3 AI rivals (Hoggart, Whiskers, Bramble) to collect truffles and become the heaviest pig. Features level progression, scoring, lose conditions, random events, power-ups (with rare stacking variants), persistent meta-progression, run-modifying boons, daily seeded runs, biomes, elite levels every 5th round, achievements, a synthesized audio system, and a post-run summary screen. Built with pure JavaFX shape primitives (no images).

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
| `core/` | GameSession, ScoreEvent, RandomEventManager, EventBus, MetaProgression, Perk, Boon, Achievement, AchievementTracker, AudioManager, LevelType |
| `world/` | GameMap (25×17 grid), MapGenerator (density + biome scaling, seedable), Biome, WeatherSystem, TimeOfDay |
| `entity/` | Pig, PlayerPig, AIPig, Wolf, Farmer; behaviors: HoggartBehavior, WhiskersBehavior, BrambleBehavior (active); RandomBehavior, CunningBehavior, RuthlessBehavior (legacy, unused) |
| `item/` | Item types (incl. power-ups + rare GREATER_SPEED / MAGNET_CROWN), ItemSpawner, GoldenTruffleManager, SuperAcornManager |
| `util/` | BFS pathfinding, FloodFill connectivity check |
| `render/` | GridRenderer, HudRenderer, SidePanelRenderer, PigRenderer, ItemRenderer, WolfRenderer, FarmerRenderer, MainMenuOverlay, ShopOverlay, BoonOverlay, RoundEndOverlay, RunSummaryOverlay, GameOverOverlay, WeatherRenderer, EffectsRenderer |

## Key Architecture Rules
- **Single AnimationTimer** drives all game logic at 60fps
- **GameSession** is run-scoped state (level, score, active boons, run seed); high score is static across runs in-memory
- **MetaProgression** persists across runs (truffle bank, perk levels, daily-best score) in `~/.trufflerush/meta.txt`
- **Game logic never touches JavaFX nodes** — only renderers create/update shapes
- **Grid-only positions** — pigs and items use integer (col, row), no sub-cell floats
- **EventBus** for decoupled communication: itemCollected, weightChanged, goldenTruffleSpawned, roundEnded
- **WeatherEffect** only modifies map state and VisibilityMask, never pig/item logic directly
- **BFS utility** is stateless and reusable
- **AI uses Strategy pattern**: `PigBehavior` interface with `Direction nextMove(AIPig, GameMap, List<Pig>)`
- **ItemSpawner** uses a spatial `Item[25][17]` grid for O(1) position lookup — update grid on every spawn and collect
- **PigRenderer** only rebuilds JavaFX shape children when radius or facing direction changes
- **EffectsRenderer** owns particle/screen-shake/hit-stop state; the world `Group` is translated by `getShakeX/Y` each frame, and `consumeHitStop()` lets the loop skip simulation while still rendering shake offsets
- **Run seed**: when `GameSession.runSeed != 0`, MapGenerator and RandomEventManager are seeded from `runSeed + level * 1_000_003`. Daily runs use `LocalDate.now().toEpochDay()` as the seed base.
- **AudioManager** synthesizes PCM tones at startup via `javax.sound.sampled` (no external files; requires `java.desktop` module); each `play(Sfx)` plays asynchronously on a daemon thread so the loop never blocks
- **AchievementTracker** routes unlocks through a single `Consumer<Achievement>` callback (in `TruffleRushApp` it pushes to `SidePanelRenderer.addEvent` + chime); persistence is delegated to `MetaProgression`

## Game Constants
- Grid: 25 columns × 17 rows, tile size 40px
- Game window: 1120×680px (1000px grid + 120px side panel)
- Level time: `max(1800, 3600 - (level-1) * 900)` ticks — starts 1 min, 15s less per level, floor 30s
- Starting weight: 50.0 kg (+ EXTRA_WEIGHT perk bonus), minimum: 10.0 kg, max cap (Pacifist boon): 120.0 kg
- Weight decay: `0.008 + (level-1) * 0.001` kg/tick (reduced by SLOWER_DECAY perk; 0 with GLUTTON boon; 1.5× with SWIFT_HOOVES boon)
- DECAY_GRACE perk: skip decay for first 300 ticks of each level
- AI move interval: `max(8, 20 - level*2)` ticks
- AI vision radius: 8 Manhattan-distance cells
- Obstacle density multiplier: `1.0 + (level-1) * 0.15`
- Pig visual radius: clamp(12 + weight × 0.15, 12, 28)
- Event check interval: 300 ticks (~5 s); event cooldown: 150 ticks between random events
- Magnet pull range: `3 + EXTRA_MAGNET_RANGE perk - (1 if SHARP_NOSE boon)`, every 60 ticks
- Risk zones: 3 wolf-den tiles per level; standing on one calls `eventMgr.accelerateCooldown(2)` per tick

## Level Progression and Scoring
- Rounds are called "levels" — surviving a level advances to the next
- **Lose conditions**: weight ≤ 10 kg (starvation), wolf catches player, farmer catches player, finishing last place
- **Score events** (ScoreEvent enum): ACORN(10), MUSHROOM(30), BLACK_TRUFFLE(80), WHITE_TRUFFLE(150), GOLDEN_TRUFFLE(300), WOLF_STUNNED(200), FARMER_ESCAPED(500), LEVEL_COMPLETE(500×level), WEIGHT_BONUS(finalWeight×5), SURVIVAL_TICK(2 per second), CLEAN_ROUND_BONUS(1000)
- High score persists for the session (GameSession.getHighScore()); daily-run best score persists in MetaProgression
- **Truffle bank deposit**: on game over, `max(1, score / 100)` truffles are added to MetaProgression and saved

## Power-ups (PlayerPig)
| Item | Effect | Duration |
|------|--------|----------|
| SPEED_MUSHROOM | 2× movement speed | 300 ticks (5s) |
| SHIELD_ACORN | Block next hit (wolf/farmer/hazard) | Until hit |
| MAGNET_TRUFFLE | Pull items within range | 360 ticks (6s) |
| DECOY_MUSHROOM | Lure AI pigs with fake golden truffle | 300 ticks (5s) |
| SUPER_ACORN | Invincible, double speed, scares wolf | 480 ticks (8s) |

Power-up state lives on PlayerPig: `tickPowerUps()` decrements timers each game tick.

SUPER_ACORN is available from level 2+ (spawns once per level in the middle 40% of round time).

### Power-up Synergies
- **Magnet + Shield**: when wolf is within 2 cells, emits a "Repulse!" pulse with magnet-coloured particles (visual; cooldown gated to every 30 ticks).
- **Speed + Super Pig**: leaves a golden trail that applies 60-tick mud slow to AI pigs within 2 cells (every 4 ticks).

### Stacking & Rare Variants
- `PlayerPig.activateSpeedBoost(int)` and `activateMagnet(int)` now **add** to the timer instead of overwriting, so collecting multiple in a row extends duration.
- `GREATER_SPEED` (deep blue mushroom): rare variant of speed mushroom, 600-tick (10 s) duration. Rarity weight 1.
- `MAGNET_CROWN` (magenta hexagon with crown spikes): rare variant of magnet truffle, 720-tick (12 s) duration. Rarity weight 1.

## Meta-progression (persistent — `~/.trufflerush/meta.txt`)
Truffle bank funds permanent perks bought in `ShopOverlay` between menu and run.

| Perk | Effect | Levels |
|------|--------|--------|
| EXTRA_WEIGHT (Well Fed) | +5 starting weight per level | 3 |
| SLOWER_DECAY (Iron Stomach) | -0.001 weight decay per level | 3 |
| START_WITH_SHIELD (Lucky Acorn) | Start each level with a shield | 1 |
| EXTRA_MAGNET_RANGE (Big Snout) | +1 magnet range per level | 2 |
| DECAY_GRACE (Slow Starter) | Skip decay for first 5s per level | 1 |

Cost formula: `baseCost * (currentLevel + 1)`. `MetaProgression.respec()` refunds 70% of spent truffles.

## Boons (run-scoped — `BoonOverlay` between levels)
After each completed level, the player picks 1 of 3 random boons; choices stack across the run.

| Boon | Effect |
|------|--------|
| TRUFFLE_HUNTER | Truffles 2× value, acorns/mushrooms 0 |
| GLUTTON | No weight decay, but 2× wolf chance |
| PACIFIST | Farmer never spawns, max weight capped at 120 |
| GREEDY_HEART | All items +50% value, hazards −50% effect |
| SWIFT_HOOVES | +25% movement speed, decay +50% |
| SHARP_NOSE | Magnet range −1 (placeholder for future sniff-cooldown halving) |

Boons are applied across `TruffleRushApp` (item-collection block, decay, max-weight cap), `RandomEventManager` (`setGluttonActive`, `setFarmerDisabled`), and `GameSession.getWeightDecayRate`.

## Random Events (RandomEventManager)
Checked every 300 ticks (~5 s); trigger chance: `min(50 + level×8, 90)`. Min 150-tick cooldown between events. Constructor takes optional `Random` for seeded daily runs. Default eventType weighting (`DEFAULT_EVENT_TABLE`): wolf 2×, farmer 2×, truffle rain / mud storm / frenzy 1× each.

| Event | Min Level | Effect | Duration |
|-------|-----------|--------|----------|
| Wolf Attack | 2 | Wolf spawns at edge, BFS-chases nearest pig; speed scales with level (`max(12, 30 - level*6)` ticks) | ~600 ticks |
| Farmer Raid | 4 | Farmer chases player; escape hole spawns | ~480 ticks |
| Truffle Rain | 1 | Extra truffle spawns every 20 ticks | 300 ticks |
| Mud Storm | 3 | Mud slow applied to all pigs | 240 ticks |
| Frenzy Mode | 1 | Double all item weight deltas | 300 ticks |

When GLUTTON is active, an extra wolf slot is appended to `DEFAULT_EVENT_TABLE` so wolves get one more weight on top of the default 2×. When PACIFIST is active, the farmer case is skipped silently.

## AI Personalities
| Pig | Behavior class | Strategy |
|-----|----------------|----------|
| Hoggart (pink) | HoggartBehavior | Always heads for the closest visible item, ignoring value/hazards |
| Whiskers (brown) | WhiskersBehavior | Cunning item-hunter; flees when wolf is within 6 cells (uses a `Supplier<Wolf>` to read the active wolf without coupling to RandomEventManager) |
| Bramble (dark grey) | BrambleBehavior | Chases the player only when Bramble is at least as heavy as the player, player weight > 25, and within 8 cells; otherwise drifts toward map centre |

`RandomBehavior`, `CunningBehavior`, `RuthlessBehavior` remain on disk as legacy code (unused by the current `TruffleRushApp` wiring).

## Game Feel (EffectsRenderer)
- **Particle bursts**: 8 particles on regular collect, 14 for high-value items, 20–30 on golden truffle / super acorn / wolf events. Colour matches item type.
- **Screen shake**: `worldGroup.setTranslateX/Y` jitters by `[-intensity, +intensity]` for `ticks` frames. Used on big collects, hits, and player death.
- **Hit-stop**: `effects.hitStop(N)` — game loop skips simulation for N frames (rendering still updates shake), used on golden truffle (4), super acorn (6), wolf stun (4).

## Audio (AudioManager — synthesized, no external files)
Generates short PCM clips at startup; failures (sandbox, no audio device) silently disable. Sfx values:

| Sfx | Triggered by |
|-----|--------------|
| `COLLECT` | Any normal item pickup |
| `BIG_COLLECT` | Truffle / golden truffle / super acorn / wolf stun |
| `HIT` | Wolf catches player, farmer catches player, starvation |
| `SHIELD_BLOCK` | Shield consumed |
| `LEVEL_UP` | Round end, farmer escape |
| `WOLF_HOWL` | Elite-level start banner |
| `HEARTBEAT` | Played every second when player weight < 18 |
| `BOON_PICK` | Boon picked from BoonOverlay |
| `ACHIEVEMENT` | Achievement unlocked |

## Biomes (`world.Biome`)
Picked from level number every 3 levels: `Biome.forLevel(level) = values()[((level - 1) / 3) % 3]`. Each biome scales the four obstacle types via per-biome multipliers fed into `MapGenerator`.

| Biome | Levels | Rocks | Bushes | Mud | Fences |
|-------|--------|-------|--------|-----|--------|
| Forest | 1–3 | 1.3× | 1.6× | 1.0× | 1.0× |
| Swamp  | 4–6 | 1.0× | 0.8× | 2.5× | 0.5× |
| Farm   | 7–9 | 0.7× | 0.5× | 0.5× | 2.5× |

## Elite Levels (`core.LevelType`)
Every 5th level cycles through ARENA → SWARM → GAUNTLET (`session.getLevelType()`). Elite levels score a 2× LEVEL_COMPLETE bonus.

| Type | Behaviour |
|------|-----------|
| ARENA | `itemSpawner.tick()` is suppressed; every 400 ticks the event cooldown is force-bumped so wolves spawn aggressively. |
| SWARM | Items spawn 6× more often (extra `itemSpawner.tick()` every 10 frames on top of the normal cadence). |
| GAUNTLET | Map obstacle density × 1.4; AI move interval × 0.66 (≈ 50% faster rivals). |

## Achievements (`core.Achievement`, persisted in MetaProgression)
15 achievements stored in `~/.trufflerush/meta.txt`. Conditions are checked from inline calls in `TruffleRushApp` (e.g. `achievements.unlock(Achievement.GOLDEN_TRUFFLE)` after a golden truffle pickup). `AchievementTracker.checkLifetimeTruffles/checkLevelReached/checkPerkMaxed` are convenience helpers. Each unlock fires the `ACHIEVEMENT` sfx + a starred event-log line.

Examples: `FIRST_TRUFFLE`, `HUNDRED_TRUFFLES`, `THOUSAND_TRUFFLES`, `REACH_LEVEL_5/10/15`, `STUN_WOLF`, `ESCAPE_FARMER`, `GOLDEN_TRUFFLE`, `CLEAN_ROUND`, `GLUTTON_PACIFIST_RUN`, `DAILY_RUN`, `MAX_PERK`.

## Run Summary (`render.RunSummaryOverlay`)
Shown between the Game Over click and the Main Menu transition. Lists the level reached, score, items collected, wolves stunned, farmers escaped, active boons, and the truffles banked from this run. Stats live on `GameSession`: `incItemsCollected`, `incWolvesStunned`, `incFarmersEscaped`.

## Navigation Flow
1. `start()` → load `MetaProgression` → `showMainMenu(stage)`
2. "Start Game" → create `GameSession` → `showShop(stage)`
3. Shop "Begin Run" → `applyPerks(meta)` → `startLevel(stage)` (random seed)
4. Shop "Today's Daily Run" → `applyPerks(meta)` + `setDailyRun(true)` + seed = `LocalDate.now().toEpochDay()` → `startLevel(stage)`
5. Level ends → `RoundEndOverlay` "Next Level" → `BoonOverlay` (if boons remain) → pick → `nextLevel()` → `startLevel(stage)`
6. Game over → `gameOverOverlay` → "Main Menu" → `depositRunRewards()` (banks `score/100` truffles, records daily score, fires lifetime-truffle achievements, saves) → `RunSummaryOverlay` → `showMainMenu(stage)`

## Module System
All new packages must be exported in `module-info.java`. No FXML opens needed. Currently exported: `com.example.demo`, `core`, `world`, `entity`, `item`, `util`, `render`. Required modules: `javafx.controls`, `java.desktop` (for `javax.sound.sampled` audio synthesis).
