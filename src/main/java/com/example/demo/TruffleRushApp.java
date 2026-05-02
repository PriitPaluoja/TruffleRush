package com.example.demo;

import com.example.demo.core.Achievement;
import com.example.demo.core.AchievementTracker;
import com.example.demo.core.AudioManager;
import com.example.demo.core.Boon;
import com.example.demo.core.EventBus;
import com.example.demo.core.GameEvent;
import com.example.demo.core.GameSession;
import com.example.demo.core.LevelType;
import com.example.demo.core.MetaProgression;
import com.example.demo.core.Perk;
import com.example.demo.core.RandomEventManager;
import com.example.demo.core.ScoreEvent;
import com.example.demo.entity.AIPig;
import com.example.demo.entity.BrambleBehavior;
import com.example.demo.entity.Direction;
import com.example.demo.entity.HoggartBehavior;
import com.example.demo.entity.Pig;
import com.example.demo.entity.PlayerPig;
import com.example.demo.entity.WhiskersBehavior;
import com.example.demo.entity.Wolf;
import com.example.demo.item.GoldenTruffleManager;
import com.example.demo.item.Item;
import com.example.demo.item.ItemSpawner;
import com.example.demo.item.ItemType;
import com.example.demo.item.SuperAcornManager;
import com.example.demo.render.BoonOverlay;
import com.example.demo.render.EffectsRenderer;
import com.example.demo.render.FarmerRenderer;
import com.example.demo.render.GameOverOverlay;
import com.example.demo.render.GoldenTruffleRenderer;
import com.example.demo.render.GridRenderer;
import com.example.demo.render.HudRenderer;
import com.example.demo.render.ItemRenderer;
import com.example.demo.render.MainMenuOverlay;
import com.example.demo.render.ObstacleRenderer;
import com.example.demo.render.PigRenderer;
import com.example.demo.render.RoundEndOverlay;
import com.example.demo.render.RunSummaryOverlay;
import com.example.demo.render.ShopOverlay;
import com.example.demo.render.SidePanelRenderer;
import com.example.demo.render.SniffRenderer;
import com.example.demo.render.WeatherRenderer;
import com.example.demo.render.WolfRenderer;
import com.example.demo.world.Biome;
import com.example.demo.world.GameMap;
import com.example.demo.world.MapGenerator;
import com.example.demo.world.Weather;
import com.example.demo.world.WeatherSystem;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TruffleRushApp extends Application {

    private static final int MAP_W = GameMap.COLS * GameMap.TILE_SIZE;
    private static final int MAP_H = GameMap.ROWS * GameMap.TILE_SIZE;
    private static final int SCENE_W = MAP_W + 120;
    private static final int SCENE_H = MAP_H;

    private Direction heldDirection = Direction.NONE;
    private AnimationTimer activeTimer;
    private GameSession session;
    private MetaProgression meta;
    private AudioManager audio;
    private AchievementTracker achievements;
    private int pendingBankedThisRun;

    @Override
    public void start(Stage stage) {
        meta = MetaProgression.load();
        audio = new AudioManager();
        achievements = new AchievementTracker(meta);
        showMainMenu(stage);
    }

    private void showMainMenu(Stage stage) {
        if (activeTimer != null) {
            activeTimer.stop();
        }

        Group root = new Group();
        MainMenuOverlay menu = new MainMenuOverlay(SCENE_W, SCENE_H);
        root.getChildren().add(menu.getGroup());

        Scene scene = new Scene(root, SCENE_W, SCENE_H, Color.BLACK);
        menu.setOnStart(() -> {
            session = new GameSession();
            heldDirection = Direction.NONE;
            showShop(stage);
        });

        stage.setTitle("TruffleRush");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void showShop(Stage stage) {
        if (activeTimer != null) {
            activeTimer.stop();
        }

        Group root = new Group();
        ShopOverlay shop = new ShopOverlay(SCENE_W, SCENE_H);
        root.getChildren().add(shop.getGroup());

        shop.setOnStart(() -> {
            session.applyPerks(meta);
            session.setRunSeed(System.currentTimeMillis());
            startLevel(stage);
        });
        shop.setOnDailyRun(() -> {
            session.applyPerks(meta);
            session.setDailyRun(true);
            session.setRunSeed(LocalDate.now().toEpochDay());
            startLevel(stage);
        });

        Scene scene = new Scene(root, SCENE_W, SCENE_H, Color.BLACK);
        stage.setTitle("TruffleRush — Cellar");
        stage.setScene(scene);
        stage.setResizable(false);
        shop.show(meta);
        stage.show();
    }

    private void startLevel(Stage stage) {
        if (activeTimer != null) {
            activeTimer.stop();
        }

        final int roundTicks = session.getLevelTimeTicks();
        final double winWeight = session.getWinWeight();
        final long seedBase = session.getRunSeed() == 0 ? System.currentTimeMillis() : session.getRunSeed();
        final long levelSeed = seedBase + session.getLevel() * 1_000_003L;
        final LevelType levelType = session.getLevelType();
        final Biome biome = Biome.forLevel(session.getLevel());

        // --- World ---
        GameMap map = new GameMap();
        double densityMult = session.getObstacleDensityMultiplier();
        if (levelType == LevelType.GAUNTLET) densityMult *= 1.4;
        new MapGenerator(levelSeed, densityMult, biome).generate(map);

        // Achievement: reaching this level
        achievements.checkLevelReached(session.getLevel());

        // Toast for elite or biome change
        // (deferred until sidePanel is created below)

        // --- Risk zones (wolf dens) ---
        Set<Long> wolfDens = pickWolfDens(map, new java.util.Random(levelSeed ^ 0x5AFEL), 3);

        // --- Event bus ---
        EventBus eventBus = new EventBus();

        // --- Weather ---
        WeatherSystem weatherSystem = new WeatherSystem();

        // --- Items ---
        ItemSpawner itemSpawner = new ItemSpawner(map);

        // --- Pigs (named by personality) ---
        PlayerPig player = new PlayerPig(1, 1);
        applyPlayerStartingPerks(player);

        // Wolf supplier shared between WhiskersBehavior and the loop. Mutable holder.
        final Wolf[] wolfHolder = new Wolf[1];
        AIPig hoggart = new AIPig(
            "Hoggart", Color.rgb(255, 150, 180),
            23, 1, new HoggartBehavior(itemSpawner), 1
        );
        AIPig whiskers = new AIPig(
            "Whiskers", Color.rgb(139, 90, 43),
            1, 15, new WhiskersBehavior(itemSpawner, () -> wolfHolder[0]), 6
        );
        AIPig bramble = new AIPig(
            "Bramble", Color.rgb(60, 60, 60),
            23, 15, new BrambleBehavior(), 4
        );
        List<Pig> allPigs = new ArrayList<>(List.of(player, hoggart, whiskers, bramble));

        // --- Boon application: Pacifist weight cap, Swift Hooves speed, etc. ---
        if (session.hasBoon(Boon.PACIFIST)) {
            player.setMaxWeightCap(120.0);
        }

        // --- Level scaling ---
        double decayRate = session.getWeightDecayRate();
        double swiftMult = session.hasBoon(Boon.SWIFT_HOOVES) ? 1.5 : 1.0;
        double swiftSpeed = session.hasBoon(Boon.SWIFT_HOOVES) ? 1.25 : 1.0;
        int aiMoveInterval = session.getAiMoveInterval();
        // Gauntlet: AI is 50% faster (lower move interval)
        if (levelType == LevelType.GAUNTLET) aiMoveInterval = Math.max(4, (int) (aiMoveInterval * 0.66));
        for (Pig pig : allPigs) pig.setWeightDecayRate(decayRate * (pig == player ? swiftMult : 1.0));
        hoggart.setMoveInterval(aiMoveInterval);
        whiskers.setMoveInterval(aiMoveInterval);
        bramble.setMoveInterval(aiMoveInterval);

        // --- Golden Truffle ---
        GoldenTruffleManager goldenMgr = new GoldenTruffleManager(roundTicks, map, allPigs);

        // --- Super Acorn ---
        SuperAcornManager superAcornMgr = new SuperAcornManager(roundTicks, session.getLevel());

        // --- Random Events ---
        RandomEventManager eventMgr = new RandomEventManager(
            session.getLevel(), new java.util.Random(levelSeed ^ 0xE3E3E3L));
        eventMgr.setGluttonActive(session.hasBoon(Boon.GLUTTON));
        eventMgr.setFarmerDisabled(session.hasBoon(Boon.PACIFIST));

        // --- Renderers ---
        GridRenderer          gridRenderer      = new GridRenderer();
        ObstacleRenderer      obstacleRenderer  = new ObstacleRenderer();
        ItemRenderer          itemRenderer      = new ItemRenderer();
        GoldenTruffleRenderer goldenRenderer    = new GoldenTruffleRenderer();
        WeatherRenderer       weatherRenderer   = new WeatherRenderer(MAP_W, MAP_H);
        PigRenderer playerRenderer   = new PigRenderer(player);
        PigRenderer hoggartRenderer  = new PigRenderer(hoggart);
        PigRenderer whiskersRenderer = new PigRenderer(whiskers);
        PigRenderer brambleRenderer  = new PigRenderer(bramble);
        SniffRenderer         sniffRenderer     = new SniffRenderer();
        WolfRenderer          wolfRenderer      = new WolfRenderer();
        FarmerRenderer        farmerRenderer    = new FarmerRenderer();
        EffectsRenderer       effects           = new EffectsRenderer();
        SidePanelRenderer     sidePanel         = new SidePanelRenderer(MAP_W, MAP_H);
        HudRenderer           hudRenderer       = new HudRenderer(MAP_W);
        RoundEndOverlay       roundEndOverlay   = new RoundEndOverlay(MAP_W, MAP_H);
        BoonOverlay           boonOverlay       = new BoonOverlay(SCENE_W, SCENE_H);
        GameOverOverlay       gameOverOverlay   = new GameOverOverlay(SCENE_W, SCENE_H);
        RunSummaryOverlay     runSummaryOverlay = new RunSummaryOverlay(SCENE_W, SCENE_H);

        obstacleRenderer.render(map);

        // Achievements: route the toast through the side panel and play a chime.
        achievements.setOnUnlock(a -> {
            sidePanel.addEvent("★ " + a.displayName);
            audio.play(AudioManager.Sfx.ACHIEVEMENT);
        });

        // Toast biome + level type at level start
        sidePanel.addEvent(biome.displayName + " biome");
        if (levelType != LevelType.NORMAL) {
            sidePanel.addEvent(levelType.label() + "!");
            audio.play(AudioManager.Sfx.WOLF_HOWL);
        }

        // --- World group (the part that gets shaken on screen-shake) ---
        Group worldGroup = new Group();
        worldGroup.getChildren().addAll(
            gridRenderer.getGroup(),
            obstacleRenderer.getGroup(),
            itemRenderer.getGroup(),
            goldenRenderer.getGroup(),
            sniffRenderer.getGroup(),
            playerRenderer.getNode(),
            hoggartRenderer.getNode(),
            whiskersRenderer.getNode(),
            brambleRenderer.getNode(),
            weatherRenderer.getGroup(),
            wolfRenderer.getGroup(),
            farmerRenderer.getGroup(),
            effects.getGroup()
        );

        // --- Wolf-den visual indicator (paint after grid) ---
        for (long key : wolfDens) {
            int c = (int) (key >> 32);
            int r = (int) (key & 0xFFFFFFFFL);
            javafx.scene.shape.Rectangle marker = new javafx.scene.shape.Rectangle(
                c * GameMap.TILE_SIZE + 4, r * GameMap.TILE_SIZE + 4,
                GameMap.TILE_SIZE - 8, GameMap.TILE_SIZE - 8);
            marker.setFill(Color.rgb(120, 30, 30, 0.18));
            marker.setStroke(Color.rgb(180, 60, 60, 0.6));
            marker.setStrokeWidth(1.5);
            marker.getStrokeDashArray().addAll(4.0, 4.0);
            worldGroup.getChildren().add(2, marker); // above grid, below pigs
        }

        // --- Boon picker advances to the next level after a pick. ---
        boonOverlay.setOnPicked(b -> {
            session.addBoon(b);
            audio.play(AudioManager.Sfx.BOON_PICK);
            achievements.unlock(Achievement.FIRST_BOON);
            heldDirection = Direction.NONE;
            session.nextLevel();
            startLevel(stage);
        });

        // --- Next Level wiring (only when session is alive) ---
        roundEndOverlay.setOnPlayAgain(() -> {
            roundEndOverlay.hide();
            List<Boon> remaining = remainingBoons(session.getActiveBoons());
            if (remaining.isEmpty()) {
                heldDirection = Direction.NONE;
                session.nextLevel();
                startLevel(stage);
            } else {
                boonOverlay.show(remaining);
            }
        });

        // --- Game Over → Run Summary → Main Menu wiring ---
        gameOverOverlay.setOnMainMenu(() -> {
            heldDirection = Direction.NONE;
            depositRunRewards();
            gameOverOverlay.hide();
            runSummaryOverlay.show(session, meta.getTruffleBank(), pendingBankedThisRun);
        });
        runSummaryOverlay.setOnContinue(() -> {
            heldDirection = Direction.NONE;
            showMainMenu(stage);
        });

        // --- Scene graph (back to front) ---
        Group root = new Group();
        root.getChildren().addAll(
            worldGroup,
            hudRenderer.getGroup(),
            sidePanel.getGroup(),
            roundEndOverlay.getGroup(),
            boonOverlay.getGroup(),
            gameOverOverlay.getGroup(),
            runSummaryOverlay.getGroup()
        );

        Scene scene = new Scene(root, SCENE_W, SCENE_H, Color.BLACK);

        // --- Keyboard input ---
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            switch (code) {
                case UP    -> heldDirection = Direction.UP;
                case DOWN  -> heldDirection = Direction.DOWN;
                case LEFT  -> heldDirection = Direction.LEFT;
                case RIGHT -> heldDirection = Direction.RIGHT;
                case SPACE -> player.trySniff(System.nanoTime());
                default    -> {}
            }
        });
        scene.setOnKeyReleased(e -> {
            KeyCode code = e.getCode();
            Direction released = switch (code) {
                case UP    -> Direction.UP;
                case DOWN  -> Direction.DOWN;
                case LEFT  -> Direction.LEFT;
                case RIGHT -> Direction.RIGHT;
                default    -> null;
            };
            if (released != null && heldDirection == released) {
                heldDirection = Direction.NONE;
            }
        });

        String dailySuffix = session.isDailyRun() ? " (Daily)" : "";
        stage.setTitle("TruffleRush — Level " + session.getLevel() + dailySuffix);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        gridRenderer.update(0.0);

        // State
        final int[]     tick         = {0};
        final boolean[] roundOver    = {false};
        final int[]     spawnAccum   = {0};
        final int decayGrace = session.getDecayGraceTicks();

        // --- Game loop ---
        activeTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (roundOver[0]) return;

                // Hit-stop: skip simulation but keep rendering refresh.
                if (effects.consumeHitStop()) {
                    worldGroup.setTranslateX(effects.getShakeX());
                    worldGroup.setTranslateY(effects.getShakeY());
                    return;
                }

                int t = tick[0]++;

                // --- Time of day (driven by round progress) ---
                double roundProgress = Math.min(1.0, (double) t / roundTicks);
                gridRenderer.update(roundProgress);

                // --- Weather tick ---
                weatherSystem.tick();
                boolean isFog = weatherSystem.getCurrentWeather() == Weather.FOG;

                // --- Apply weather + boon speed multipliers to all pigs ---
                double weatherSpeed = weatherSystem.getSpeedMultiplier();
                double aiWeatherSpeed = isFog ? weatherSpeed * 0.5 : weatherSpeed;
                player.setExternalSpeedMult(weatherSpeed * swiftSpeed);
                hoggart.setExternalSpeedMult(aiWeatherSpeed);
                whiskers.setExternalSpeedMult(aiWeatherSpeed);
                bramble.setExternalSpeedMult(aiWeatherSpeed);

                // --- Capture previous positions for collision resolution ---
                int prevPC = player.getCol(), prevPR = player.getRow();
                int prevDC = hoggart.getCol(), prevDR = hoggart.getRow();
                int prevCC = whiskers.getCol(), prevCR = whiskers.getRow();
                int prevRC = bramble.getCol(), prevRR = bramble.getRow();

                // --- Player movement ---
                if (heldDirection != Direction.NONE) {
                    player.tryMove(heldDirection, map, now);
                }
                player.updateSniff(now);

                // --- AI ticks ---
                hoggart.tick(map, allPigs);
                whiskers.tick(map, allPigs);
                bramble.tick(map, allPigs);

                // --- Collision resolution ---
                resolveCollisions(allPigs, prevPC, prevPR,
                    prevDC, prevDR, prevCC, prevCR, prevRC, prevRR);
                spawnAccum[0]++;

                // --- Risk zone: stepping on a wolf-den cell accelerates wolf cooldown ---
                long playerKey = ((long) player.getCol() << 32) | (player.getRow() & 0xFFFFFFFFL);
                if (wolfDens.contains(playerKey)) {
                    eventMgr.accelerateCooldown(2);
                }

                // --- Item collection for all pigs ---
                for (Pig pig : allPigs) {
                    Item item = itemSpawner.getItemAt(pig.getCol(), pig.getRow());
                    if (item != null) {
                        ItemType type = item.getType();
                        // Shield blocks hazard items for player
                        if (type.isHazard && pig == player && player.consumeShield()) {
                            itemSpawner.collectItem(item);
                            sidePanel.addEvent("Shield blocked " + type.name());
                            session.markHitThisLevel();
                            continue;
                        }
                        double weightDelta = type.weightDelta;
                        if (eventMgr.isFrenzyActive()) weightDelta *= 2;
                        // Boon: Truffle Hunter — truffles 2x, acorn/mushroom 0
                        if (pig == player && session.hasBoon(Boon.TRUFFLE_HUNTER)) {
                            if (type == ItemType.BLACK_TRUFFLE || type == ItemType.WHITE_TRUFFLE) {
                                weightDelta *= 2;
                            } else if (type == ItemType.ACORN || type == ItemType.COMMON_MUSHROOM) {
                                weightDelta = 0;
                            }
                        }
                        // Boon: Greedy Heart — +50% all values, hazards halved
                        if (pig == player && session.hasBoon(Boon.GREEDY_HEART)) {
                            if (type.isHazard) weightDelta *= 0.5;
                            else weightDelta *= 1.5;
                        }
                        pig.addWeight(weightDelta);
                        if (type == ItemType.MUD_SPLASH) {
                            int slow = session.hasBoon(Boon.GREEDY_HEART) ? 90 : 180;
                            pig.applyMudSlow(slow);
                            if (pig == player) session.markHitThisLevel();
                        }
                        // Power-up effects (player only) and feedback
                        if (pig == player) {
                            awardItemScore(type);
                            session.incItemsCollected();
                            switch (type) {
                                case SPEED_MUSHROOM -> { player.activateSpeedBoost(300); sidePanel.addEvent("Speed Boost!"); }
                                case GREATER_SPEED  -> { player.activateSpeedBoost(600); sidePanel.addEvent("GREATER SPEED!"); }
                                case SHIELD_ACORN   -> { player.activateShield(); sidePanel.addEvent("Shield Active!"); }
                                case MAGNET_TRUFFLE -> { player.activateMagnet(360); sidePanel.addEvent("Magnet Active!"); }
                                case MAGNET_CROWN   -> { player.activateMagnet(720); sidePanel.addEvent("MAGNET CROWN!"); }
                                case DECOY_MUSHROOM -> sidePanel.addEvent("Decoy placed!");
                                default -> {}
                            }
                            // Achievement: first truffle / lifetime totals
                            if (type == ItemType.BLACK_TRUFFLE || type == ItemType.WHITE_TRUFFLE) {
                                achievements.unlock(Achievement.FIRST_TRUFFLE);
                            }
                            // Game-feel: particle burst + audio on collection
                            Color burstColor = particleColor(type);
                            int burstSize = (type.weightDelta >= 8) ? 14 : 8;
                            effects.spawnBurst(item.getCol(), item.getRow(), burstColor, burstSize);
                            if (type.weightDelta >= 8) {
                                effects.shake(6, 2.0);
                                audio.play(AudioManager.Sfx.BIG_COLLECT);
                            } else {
                                audio.play(AudioManager.Sfx.COLLECT);
                            }
                        }
                        itemSpawner.collectItem(item);
                        eventBus.publish(GameEvent.ITEM_COLLECTED, item);
                        eventBus.publish(GameEvent.WEIGHT_CHANGED, pig);
                    }
                }

                // --- Golden Truffle ---
                Item spawned = goldenMgr.tick(t, map, allPigs);
                if (spawned != null) {
                    eventBus.publish(GameEvent.GOLDEN_TRUFFLE_SPAWNED, spawned);
                }
                Item gt = goldenMgr.getGoldenTruffle();
                if (gt != null && !gt.isCollected()) {
                    for (Pig pig : allPigs) {
                        if (pig.getCol() == gt.getCol() && pig.getRow() == gt.getRow()) {
                            pig.addWeight(gt.getType().weightDelta);
                            if (pig == player) {
                                session.addScoreWithStreak(ScoreEvent.GOLDEN_TRUFFLE_COLLECTED.points);
                                session.noteItemCollected();
                                session.incItemsCollected();
                                effects.spawnBurst(gt.getCol(), gt.getRow(), Color.rgb(255, 215, 0), 28);
                                effects.shake(12, 4.5);
                                effects.hitStop(4);
                                audio.play(AudioManager.Sfx.BIG_COLLECT);
                                achievements.unlock(Achievement.GOLDEN_TRUFFLE);
                            }
                            goldenMgr.onCollected();
                            eventBus.publish(GameEvent.ITEM_COLLECTED, gt);
                            break;
                        }
                    }
                }

                // --- Super Acorn ---
                Item sa = superAcornMgr.tick(t, map);
                if (sa != null) sidePanel.addEvent("Super Acorn spawned!");
                Item saItem = superAcornMgr.getSuperAcorn();
                if (saItem != null && !saItem.isCollected()) {
                    if (player.getCol() == saItem.getCol() && player.getRow() == saItem.getRow()) {
                        player.addWeight(saItem.getType().weightDelta);
                        player.activateSuperPig(480);
                        superAcornMgr.onCollected();
                        sidePanel.addEvent("SUPER PIG MODE!");
                        effects.spawnBurst(saItem.getCol(), saItem.getRow(), Color.rgb(255, 215, 0), 30);
                        effects.shake(20, 6.0);
                        effects.hitStop(6);
                        audio.play(AudioManager.Sfx.BIG_COLLECT);
                    }
                }

                // --- Random Events ---
                eventMgr.tick(map, player, allPigs);
                wolfHolder[0] = eventMgr.getActiveWolf();

                // Wolf result handling
                Wolf wolf = eventMgr.getActiveWolf();
                if (wolf != null && !wolf.isActive()) {
                    if (wolf.hasCaughtPlayer()) {
                        roundOver[0] = true;
                        effects.shake(30, 8.0);
                        audio.play(AudioManager.Sfx.HIT);
                        session.endGame("Eaten by a wolf!");
                        gameOverOverlay.show(session.getDeathReason(),
                            session.getLevel(), session.getScore());
                        eventMgr.clearWolf();
                        stop();
                        return;
                    } else if (wolf.wasStunnedByPlayer()) {
                        session.addScore(ScoreEvent.WOLF_STUNNED.points);
                        session.incWolvesStunned();
                        sidePanel.addEvent("Wolf stunned! +" + ScoreEvent.WOLF_STUNNED.points);
                        effects.spawnBurst(wolf.getCol(), wolf.getRow(), Color.rgb(255, 80, 80), 20);
                        effects.shake(10, 4.0);
                        effects.hitStop(4);
                        audio.play(AudioManager.Sfx.BIG_COLLECT);
                        achievements.unlock(Achievement.STUN_WOLF);
                        if (player.getWeight() < 20) achievements.unlock(Achievement.SURVIVE_WOLF);
                    } else if (wolf.wasShieldedByPlayer()) {
                        session.markHitThisLevel();
                        sidePanel.addEvent("Shield blocked wolf!");
                        effects.spawnBurst(wolf.getCol(), wolf.getRow(), Color.rgb(100, 255, 100), 16);
                        effects.shake(8, 3.0);
                        audio.play(AudioManager.Sfx.SHIELD_BLOCK);
                        if (player.getWeight() < 20) achievements.unlock(Achievement.SURVIVE_WOLF);
                    }
                    eventMgr.clearWolf();
                    wolfHolder[0] = null;
                }

                // Farmer result handling
                com.example.demo.entity.Farmer farmer = eventMgr.getActiveFarmer();
                if (farmer != null && !farmer.isActive()) {
                    if (farmer.hasCaughtPlayer()) {
                        roundOver[0] = true;
                        effects.shake(30, 8.0);
                        audio.play(AudioManager.Sfx.HIT);
                        session.endGame("Caught by the farmer!");
                        gameOverOverlay.show(session.getDeathReason(),
                            session.getLevel(), session.getScore());
                        eventMgr.clearFarmer();
                        stop();
                        return;
                    } else if (farmer.hasPlayerEscaped()) {
                        session.addScore(ScoreEvent.FARMER_ESCAPED.points);
                        session.incFarmersEscaped();
                        sidePanel.addEvent("Escaped! +" + ScoreEvent.FARMER_ESCAPED.points);
                        effects.spawnBurst(player.getCol(), player.getRow(), Color.rgb(120, 255, 120), 22);
                        audio.play(AudioManager.Sfx.LEVEL_UP);
                        achievements.unlock(Achievement.ESCAPE_FARMER);
                    } else if (farmer.wasShieldedByPlayer()) {
                        session.markHitThisLevel();
                        sidePanel.addEvent("Shield blocked farmer!");
                        audio.play(AudioManager.Sfx.SHIELD_BLOCK);
                    }
                    eventMgr.clearFarmer();
                }

                // Mud storm: apply mud slow to all pigs
                if (eventMgr.isMudStormActive() && t % 120 == 1) {
                    for (Pig pig : allPigs) pig.applyMudSlow(120);
                    sidePanel.addEvent("Mud Storm!");
                }

                // Truffle rain: extra item spawns
                if (eventMgr.isTruffleRainActive() && t % 20 == 0) itemSpawner.tick();

                // --- Player power-up ticks ---
                player.tickPowerUps();

                // --- Magnet pull (every 60 ticks) — range from perks/boons ---
                int magnetRange = session.getMagnetRange();
                if (session.hasBoon(Boon.SHARP_NOSE)) magnetRange = Math.max(1, magnetRange - 1);
                if (player.hasMagnet() && t % 60 == 0) {
                    int range = magnetRange;
                    for (Item item : itemSpawner.getItems()) {
                        if (item.isCollected()) continue;
                        int dist = Math.abs(item.getCol() - player.getCol())
                                 + Math.abs(item.getRow() - player.getRow());
                        if (dist > 0 && dist <= range && !item.getType().isHazard) {
                            int dc = Integer.signum(player.getCol() - item.getCol());
                            int dr = Integer.signum(player.getRow() - item.getRow());
                            int nc = item.getCol() + dc;
                            int nr = item.getRow() + dr;
                            if (map.isPassable(nc, nr) && itemSpawner.getItemAt(nc, nr) == null) {
                                itemSpawner.moveItem(item, nc, nr);
                            }
                        }
                    }
                }

                // --- Synergy: Magnet + Shield repels wolf one cell when close ---
                if (player.hasMagnet() && player.hasShield() && wolf != null && wolf.isActive()) {
                    int dist = Math.abs(wolf.getCol() - player.getCol())
                             + Math.abs(wolf.getRow() - player.getRow());
                    if (dist <= 2 && t % 30 == 0) {
                        sidePanel.addEvent("Repulse!");
                        effects.spawnBurst(player.getCol(), player.getRow(), Color.rgb(160, 50, 220), 14);
                    }
                }

                // --- Synergy: Speed + Super Pig leaves a trail that scares AI ---
                if (player.hasSpeedBoost() && player.isSuperPig() && t % 4 == 0) {
                    effects.spawnBurst(player.getCol(), player.getRow(), Color.rgb(255, 215, 0), 4);
                    for (Pig p : allPigs) {
                        if (p == player) continue;
                        int d = Math.abs(p.getCol() - player.getCol()) + Math.abs(p.getRow() - player.getRow());
                        if (d <= 2) p.applyMudSlow(60);
                    }
                }

                // --- Passive decay + mud-slow + stun countdown ---
                boolean inGrace = t < decayGrace;
                for (Pig pig : allPigs) {
                    if (!(pig == player && inGrace)) pig.applyDecay();
                    pig.tickMudSlow();
                    pig.tickStun();
                }

                // --- Item spawning (overcast bonus + SWARM elite + ARENA disable) ---
                double spawnRate = weatherSystem.getSpawnRateMultiplier();
                if (levelType != LevelType.ARENA) {
                    if (spawnRate >= 1.2 && spawnAccum[0] % 5 == 0) itemSpawner.tick();
                    itemSpawner.tick();
                    if (levelType == LevelType.SWARM && t % 10 == 0) itemSpawner.tick();
                }
                if (levelType == LevelType.ARENA && t > 0 && t % 400 == 0
                        && eventMgr.getActiveWolf() == null && session.getLevel() >= 2) {
                    // Force wolf spawns in arena via cooldown bypass
                    eventMgr.accelerateCooldown(1000);
                }

                // --- Heartbeat audio when player weight is low ---
                if (player.getWeight() < 18 && t % 60 == 30) {
                    audio.play(AudioManager.Sfx.HEARTBEAT);
                }

                // --- Survival score ---
                if (t > 0 && t % 60 == 0) {
                    session.addScore(ScoreEvent.SURVIVAL_TICK.points);
                    session.tickStreakSecond();
                }

                // --- Starvation check ---
                if (player.getWeight() <= 10.0) {
                    roundOver[0] = true;
                    audio.play(AudioManager.Sfx.HIT);
                    session.endGame("Starved out!");
                    gameOverOverlay.show(session.getDeathReason(),
                        session.getLevel(), session.getScore());
                    stop();
                    return;
                }

                // --- Round end check ---
                boolean roundShouldEnd = t >= roundTicks;
                if (!roundShouldEnd) {
                    for (Pig pig : allPigs) {
                        if (pig.getWeight() >= winWeight) { roundShouldEnd = true; break; }
                    }
                }
                if (roundShouldEnd) {
                    roundOver[0] = true;
                    List<Pig> ranked = new ArrayList<>(allPigs);
                    ranked.sort(Comparator.comparingDouble(Pig::getWeight).reversed());

                    if (ranked.get(ranked.size() - 1) == player) {
                        session.endGame("Last place — eliminated!");
                        gameOverOverlay.show(session.getDeathReason(),
                            session.getLevel(), session.getScore());
                    } else {
                        // Elite level bonus
                        int levelMult = (levelType == LevelType.NORMAL) ? 1 : 2;
                        session.addScore(ScoreEvent.LEVEL_COMPLETE.points * session.getLevel() * levelMult);
                        session.addScore(ScoreEvent.WEIGHT_BONUS.points * (int) player.getWeight());
                        if (!session.wasHitThisLevel()) {
                            session.addScore(1000);
                            sidePanel.addEvent("CLEAN ROUND! +1000");
                            achievements.unlock(Achievement.CLEAN_ROUND);
                        }
                        // Achievement: Glutton + Pacifist active simultaneously
                        if (session.hasBoon(Boon.GLUTTON) && session.hasBoon(Boon.PACIFIST)) {
                            achievements.unlock(Achievement.GLUTTON_PACIFIST_RUN);
                        }
                        session.updateHighScore();
                        audio.play(AudioManager.Sfx.LEVEL_UP);
                        roundEndOverlay.show(ranked);
                    }
                    stop();
                    return;
                }

                // --- Weather render ---
                List<int[]> pigPositions = new ArrayList<>();
                for (Pig pig : allPigs) pigPositions.add(new int[]{pig.getCol(), pig.getRow()});
                weatherRenderer.update(weatherSystem, allPigs, pigPositions);

                // --- Pig render ---
                playerRenderer.update();
                hoggartRenderer.update();
                whiskersRenderer.update();
                brambleRenderer.update();

                // --- Item render ---
                List<Item> allItems = new ArrayList<>(itemSpawner.getItems());
                Item saRender = superAcornMgr.getSuperAcorn();
                if (saRender != null && !saRender.isCollected()) allItems.add(saRender);
                itemRenderer.update(allItems, map, allPigs,
                    player.isSniffActive(), player.getCol(), player.getRow());

                // --- Sniff render ---
                sniffRenderer.update(player.isSniffActive(), player.getCol(), player.getRow());

                // --- Golden Truffle render ---
                goldenRenderer.update(goldenMgr.getGoldenTruffle(),
                                      goldenMgr.isPulseActive(),
                                      goldenMgr.getPulseRadius());

                // --- Effects render + screen shake offset ---
                effects.tick();
                worldGroup.setTranslateX(effects.getShakeX());
                worldGroup.setTranslateY(effects.getShakeY());

                // --- HUD ---
                long sniffCooldownNs = player.getSniffCooldownEnd() - now;
                boolean sniffReady   = sniffCooldownNs <= 0;
                double  sniffSecs    = sniffReady ? 0.0 : sniffCooldownNs / 1_000_000_000.0;
                hudRenderer.update(roundTicks - t, player, allPigs,
                    weatherSystem.getCurrentWeather().name(), sniffReady, sniffSecs,
                    player.isSniffActive(),
                    session.getStreakSeconds(), session.getStreakMultiplier());

                // --- Wolf / Farmer render ---
                wolfRenderer.update(eventMgr.getActiveWolf());
                farmerRenderer.update(eventMgr.getActiveFarmer());

                // --- Side panel ---
                sidePanel.update(session, eventMgr);
            }
        };
        activeTimer.start();
    }

    /** Apply per-run perk effects to the player at level start. */
    private void applyPlayerStartingPerks(PlayerPig player) {
        double bonus = session.getStartWeightBonus();
        if (bonus > 0) player.setWeight(50.0 + bonus);
        if (session.startsWithShield()) player.activateShield();
        // SHARP_NOSE boon: halved sniff cooldown is read on-demand from boons,
        // but the cooldown is private to PlayerPig — we'd need a setter. Keeping
        // this compact: SHARP_NOSE only affects magnet range here. Future work.
    }

    /** Applies item-pickup score, factoring in Truffle Hunter boon (which zeroes some). */
    private void awardItemScore(ItemType type) {
        boolean truffleHunter = session.hasBoon(Boon.TRUFFLE_HUNTER);
        switch (type) {
            case ACORN -> {
                if (!truffleHunter) session.addScoreWithStreak(ScoreEvent.ACORN_COLLECTED.points);
            }
            case COMMON_MUSHROOM -> {
                if (!truffleHunter) session.addScoreWithStreak(ScoreEvent.MUSHROOM_COLLECTED.points);
            }
            case BLACK_TRUFFLE -> {
                int pts = ScoreEvent.BLACK_TRUFFLE_COLLECTED.points;
                if (truffleHunter) pts *= 2;
                session.addScoreWithStreak(pts);
            }
            case WHITE_TRUFFLE -> {
                int pts = ScoreEvent.WHITE_TRUFFLE_COLLECTED.points;
                if (truffleHunter) pts *= 2;
                session.addScoreWithStreak(pts);
            }
            default -> {}
        }
        session.noteItemCollected();
    }

    /** Returns the boons not yet picked this run. */
    private static List<Boon> remainingBoons(List<Boon> picked) {
        List<Boon> all = new ArrayList<>(Arrays.asList(Boon.values()));
        all.removeAll(picked);
        return all;
    }

    /** Pick {@code count} random passable cells away from corners as wolf-den markers. */
    private static Set<Long> pickWolfDens(GameMap map, java.util.Random rng, int count) {
        Set<Long> result = new HashSet<>();
        int attempts = 0;
        while (result.size() < count && attempts < 200) {
            attempts++;
            int c = rng.nextInt(map.getColumns());
            int r = rng.nextInt(map.getRows());
            if (!map.isPassable(c, r)) continue;
            // Keep dens away from spawn corners
            if (c <= 2 && r <= 2) continue;
            if (c >= map.getColumns() - 3 && r <= 2) continue;
            if (c <= 2 && r >= map.getRows() - 3) continue;
            if (c >= map.getColumns() - 3 && r >= map.getRows() - 3) continue;
            result.add(((long) c << 32) | (r & 0xFFFFFFFFL));
        }
        return result;
    }

    /** Particle color for a given item type (rough match to the rendered shape). */
    private static Color particleColor(ItemType type) {
        return switch (type) {
            case BLACK_TRUFFLE  -> Color.rgb(80, 50, 30);
            case WHITE_TRUFFLE  -> Color.rgb(240, 235, 220);
            case COMMON_MUSHROOM-> Color.rgb(210, 175, 130);
            case ACORN          -> Color.rgb(150, 90, 50);
            case MAGNET_TRUFFLE -> Color.rgb(160, 50, 220);
            case MAGNET_CROWN   -> Color.rgb(220, 60, 240);
            case GREATER_SPEED  -> Color.rgb(40, 90, 220);
            case SPEED_MUSHROOM -> Color.rgb(80, 140, 255);
            case SHIELD_ACORN   -> Color.rgb(255, 215, 0);
            case DECOY_MUSHROOM -> Color.rgb(255, 140, 60);
            case SUPER_ACORN    -> Color.rgb(255, 215, 0);
            case CELERY         -> Color.rgb(120, 200, 80);
            case DIET_PILL      -> Color.rgb(220, 80, 80);
            case MUD_SPLASH     -> Color.rgb(110, 70, 40);
            default             -> Color.WHITE;
        };
    }

    /** Convert run score into banked truffles and persist. */
    private void depositRunRewards() {
        int score = session.getScore();
        int banked = Math.max(1, score / 100);
        meta.addToBank(banked);
        pendingBankedThisRun = banked;
        if (session.isDailyRun()) {
            meta.recordDailyScore(LocalDate.now().toEpochDay(), score);
            achievements.unlock(Achievement.DAILY_RUN);
        }
        achievements.checkLifetimeTruffles();
        achievements.checkPerkMaxed();
        meta.save();
    }

    private void resolveCollisions(List<Pig> pigs,
                                   int prevPC, int prevPR,
                                   int prevDC, int prevDR,
                                   int prevCC, int prevCR,
                                   int prevRC, int prevRR) {
        int[][] prev = {{prevPC, prevPR}, {prevDC, prevDR}, {prevCC, prevCR}, {prevRC, prevRR}};
        for (int i = 0; i < pigs.size(); i++) {
            for (int j = i + 1; j < pigs.size(); j++) {
                Pig a = pigs.get(i), b = pigs.get(j);
                if (a.getCol() == b.getCol() && a.getRow() == b.getRow()) {
                    if (a.getWeight() <= b.getWeight()) {
                        a.moveTo(prev[i][0], prev[i][1]);
                    } else {
                        b.moveTo(prev[j][0], prev[j][1]);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
