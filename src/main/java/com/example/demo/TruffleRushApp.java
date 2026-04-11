package com.example.demo;

import com.example.demo.core.EventBus;
import com.example.demo.core.GameEvent;
import com.example.demo.core.GameSession;
import com.example.demo.core.RandomEventManager;
import com.example.demo.core.ScoreEvent;
import com.example.demo.entity.AIPig;
import com.example.demo.entity.CunningBehavior;
import com.example.demo.entity.Direction;
import com.example.demo.entity.Pig;
import com.example.demo.entity.PlayerPig;
import com.example.demo.entity.RandomBehavior;
import com.example.demo.entity.RuthlessBehavior;
import com.example.demo.item.GoldenTruffleManager;
import com.example.demo.item.Item;
import com.example.demo.item.ItemSpawner;
import com.example.demo.item.ItemType;
import com.example.demo.item.SuperAcornManager;
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
import com.example.demo.render.SidePanelRenderer;
import com.example.demo.render.SniffRenderer;
import com.example.demo.render.WeatherRenderer;
import com.example.demo.render.WolfRenderer;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TruffleRushApp extends Application {

    private static final int MAP_W = GameMap.COLS * GameMap.TILE_SIZE;
    private static final int MAP_H = GameMap.ROWS * GameMap.TILE_SIZE;
    private static final int SCENE_W = MAP_W + 120;
    private static final int SCENE_H = MAP_H;

    private Direction heldDirection = Direction.NONE;
    private AnimationTimer activeTimer;
    private GameSession session;

    @Override
    public void start(Stage stage) {
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
            startLevel(stage);
        });

        stage.setTitle("TruffleRush");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void startLevel(Stage stage) {
        if (activeTimer != null) {
            activeTimer.stop();
        }

        final int roundTicks = session.getLevelTimeTicks();
        final double winWeight = session.getWinWeight();

        // --- World ---
        GameMap map = new GameMap();
        new MapGenerator(System.currentTimeMillis(), session.getObstacleDensityMultiplier())
            .generate(map);

        // --- Event bus ---
        EventBus eventBus = new EventBus();

        // --- Weather ---
        WeatherSystem weatherSystem = new WeatherSystem();

        // --- Items ---
        ItemSpawner itemSpawner = new ItemSpawner(map);

        // --- Pigs ---
        PlayerPig player = new PlayerPig(1, 1);
        RuthlessBehavior ruthlessBehavior = new RuthlessBehavior(itemSpawner);
        AIPig dumb = new AIPig(
            "Dumb", Color.rgb(255, 150, 180),
            23, 1, new RandomBehavior(), 1
        );
        AIPig cunning = new AIPig(
            "Cunning", Color.rgb(139, 90, 43),
            1, 15, new CunningBehavior(itemSpawner), 6
        );
        AIPig ruthless = new AIPig(
            "Ruthless", Color.rgb(60, 60, 60),
            23, 15, ruthlessBehavior, 3
        );
        List<Pig> allPigs = new ArrayList<>(List.of(player, dumb, cunning, ruthless));

        // --- Level scaling ---
        double decayRate = session.getWeightDecayRate();
        int aiMoveInterval = session.getAiMoveInterval();
        for (Pig pig : allPigs) pig.setWeightDecayRate(decayRate);
        dumb.setMoveInterval(aiMoveInterval);
        cunning.setMoveInterval(aiMoveInterval);
        ruthless.setMoveInterval(aiMoveInterval);

        // --- Golden Truffle ---
        GoldenTruffleManager goldenMgr = new GoldenTruffleManager(roundTicks, map, allPigs);

        // --- Super Acorn ---
        SuperAcornManager superAcornMgr = new SuperAcornManager(roundTicks, session.getLevel());

        // --- Random Events ---
        RandomEventManager eventMgr = new RandomEventManager(session.getLevel());

        // --- Renderers ---
        GridRenderer          gridRenderer      = new GridRenderer();
        ObstacleRenderer      obstacleRenderer  = new ObstacleRenderer();
        ItemRenderer          itemRenderer      = new ItemRenderer();
        GoldenTruffleRenderer goldenRenderer    = new GoldenTruffleRenderer();
        WeatherRenderer       weatherRenderer   = new WeatherRenderer(MAP_W, MAP_H);
        PigRenderer playerRenderer   = new PigRenderer(player);
        PigRenderer dumbRenderer     = new PigRenderer(dumb);
        PigRenderer cunningRenderer  = new PigRenderer(cunning);
        PigRenderer ruthlessRenderer = new PigRenderer(ruthless);
        SniffRenderer         sniffRenderer     = new SniffRenderer();
        WolfRenderer          wolfRenderer      = new WolfRenderer();
        FarmerRenderer        farmerRenderer    = new FarmerRenderer();
        SidePanelRenderer     sidePanel         = new SidePanelRenderer(MAP_W, MAP_H);
        HudRenderer           hudRenderer       = new HudRenderer(MAP_W);
        RoundEndOverlay       roundEndOverlay   = new RoundEndOverlay(MAP_W, MAP_H);
        GameOverOverlay       gameOverOverlay   = new GameOverOverlay(SCENE_W, SCENE_H);

        obstacleRenderer.render(map);

        // --- Next Level wiring (only when session is alive) ---
        roundEndOverlay.setOnPlayAgain(() -> {
            heldDirection = Direction.NONE;
            session.nextLevel();
            startLevel(stage);
        });

        // --- Game Over → Main Menu wiring ---
        gameOverOverlay.setOnMainMenu(() -> {
            heldDirection = Direction.NONE;
            showMainMenu(stage);
        });

        // --- Scene graph (back to front) ---
        Group root = new Group();
        root.getChildren().addAll(
            gridRenderer.getGroup(),
            obstacleRenderer.getGroup(),
            itemRenderer.getGroup(),
            goldenRenderer.getGroup(),
            sniffRenderer.getGroup(),
            playerRenderer.getNode(),
            dumbRenderer.getNode(),
            cunningRenderer.getNode(),
            ruthlessRenderer.getNode(),
            weatherRenderer.getGroup(),
            wolfRenderer.getGroup(),
            farmerRenderer.getGroup(),
            hudRenderer.getGroup(),
            sidePanel.getGroup(),
            roundEndOverlay.getGroup(),
            gameOverOverlay.getGroup()
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

        stage.setTitle("TruffleRush — Level " + session.getLevel());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        gridRenderer.update(0.0);

        // State
        final int[]     tick         = {0};
        final boolean[] roundOver    = {false};
        final int[]     spawnAccum   = {0};

        // --- Game loop ---
        activeTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (roundOver[0]) return;

                int t = tick[0]++;

                // --- Time of day (driven by round progress) ---
                double roundProgress = Math.min(1.0, (double) t / roundTicks);
                gridRenderer.update(roundProgress);

                // --- Weather tick ---
                weatherSystem.tick();
                boolean isFog = weatherSystem.getCurrentWeather() == Weather.FOG;

                // --- Apply weather speed multiplier to all pigs ---
                double weatherSpeed = weatherSystem.getSpeedMultiplier();
                double aiWeatherSpeed = isFog ? weatherSpeed * 0.5 : weatherSpeed;
                player.setExternalSpeedMult(weatherSpeed);
                dumb.setExternalSpeedMult(aiWeatherSpeed);
                cunning.setExternalSpeedMult(aiWeatherSpeed);
                ruthless.setExternalSpeedMult(aiWeatherSpeed);

                // --- Capture previous positions for collision resolution ---
                int prevPC = player.getCol(), prevPR = player.getRow();
                int prevDC = dumb.getCol(),    prevDR = dumb.getRow();
                int prevCC = cunning.getCol(), prevCR = cunning.getRow();
                int prevRC = ruthless.getCol(), prevRR = ruthless.getRow();

                // --- Player movement ---
                if (heldDirection != Direction.NONE) {
                    player.tryMove(heldDirection, map, now);
                }
                player.updateSniff(now);

                // --- AI ticks ---
                dumb.tick(map, allPigs);
                cunning.tick(map, allPigs);
                ruthless.tick(map, allPigs);

                // --- Collision resolution ---
                resolveCollisions(allPigs, prevPC, prevPR,
                    prevDC, prevDR, prevCC, prevCR, prevRC, prevRR);
                spawnAccum[0]++;

                // --- Item collection for all pigs ---
                for (Pig pig : allPigs) {
                    Item item = itemSpawner.getItemAt(pig.getCol(), pig.getRow());
                    if (item != null) {
                        ItemType type = item.getType();
                        // Shield blocks hazard items for player
                        if (type.isHazard && pig == player && player.consumeShield()) {
                            itemSpawner.collectItem(item);
                            sidePanel.addEvent("Shield blocked " + type.name());
                            continue;
                        }
                        pig.addWeight(type.weightDelta);
                        if (type == ItemType.MUD_SPLASH) {
                            pig.applyMudSlow(180);
                        }
                        // Power-up effects (player only)
                        if (pig == player) {
                            awardItemScore(type);
                            switch (type) {
                                case SPEED_MUSHROOM -> { player.activateSpeedBoost(300); sidePanel.addEvent("Speed Boost!"); }
                                case SHIELD_ACORN   -> { player.activateShield(); sidePanel.addEvent("Shield Active!"); }
                                case MAGNET_TRUFFLE -> { player.activateMagnet(360); sidePanel.addEvent("Magnet Active!"); }
                                case DECOY_MUSHROOM -> sidePanel.addEvent("Decoy placed!");
                                default -> {}
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
                    ruthlessBehavior.setGoldenTruffleTarget(spawned.getCol(), spawned.getRow());
                    eventBus.publish(GameEvent.GOLDEN_TRUFFLE_SPAWNED, spawned);
                }
                Item gt = goldenMgr.getGoldenTruffle();
                if (gt != null && !gt.isCollected()) {
                    for (Pig pig : allPigs) {
                        if (pig.getCol() == gt.getCol() && pig.getRow() == gt.getRow()) {
                            pig.addWeight(gt.getType().weightDelta);
                            if (pig == player) {
                                session.addScore(ScoreEvent.GOLDEN_TRUFFLE_COLLECTED.points);
                            }
                            goldenMgr.onCollected();
                            ruthlessBehavior.setGoldenTruffleTarget(-1, -1);
                            eventBus.publish(GameEvent.ITEM_COLLECTED, gt);
                            break;
                        }
                    }
                }

                // --- Super Acorn ---
                Item sa = superAcornMgr.tick(t, map);
                if (sa != null) {
                    sidePanel.addEvent("Super Acorn spawned!");
                }
                Item saItem = superAcornMgr.getSuperAcorn();
                if (saItem != null && !saItem.isCollected()) {
                    if (player.getCol() == saItem.getCol() && player.getRow() == saItem.getRow()) {
                        player.addWeight(saItem.getType().weightDelta);
                        player.activateSuperPig(480);
                        superAcornMgr.onCollected();
                        sidePanel.addEvent("SUPER PIG MODE!");
                    }
                }

                // --- Random Events ---
                eventMgr.tick(map, player, allPigs);

                // Wolf result handling
                com.example.demo.entity.Wolf wolf = eventMgr.getActiveWolf();
                if (wolf != null && !wolf.isActive()) {
                    if (wolf.hasCaughtPlayer()) {
                        roundOver[0] = true;
                        session.endGame("Eaten by a wolf!");
                        gameOverOverlay.show(session.getDeathReason(),
                            session.getLevel(), session.getScore());
                        eventMgr.clearWolf();
                        return;
                    } else if (wolf.wasStunnedByPlayer()) {
                        session.addScore(ScoreEvent.WOLF_STUNNED.points);
                        sidePanel.addEvent("Wolf stunned! +" + ScoreEvent.WOLF_STUNNED.points);
                    }
                    eventMgr.clearWolf();
                }

                // Farmer result handling
                com.example.demo.entity.Farmer farmer = eventMgr.getActiveFarmer();
                if (farmer != null && !farmer.isActive()) {
                    if (farmer.hasCaughtPlayer()) {
                        roundOver[0] = true;
                        session.endGame("Caught by the farmer!");
                        gameOverOverlay.show(session.getDeathReason(),
                            session.getLevel(), session.getScore());
                        eventMgr.clearFarmer();
                        return;
                    } else if (farmer.hasPlayerEscaped()) {
                        session.addScore(ScoreEvent.FARMER_ESCAPED.points);
                        sidePanel.addEvent("Escaped! +" + ScoreEvent.FARMER_ESCAPED.points);
                    }
                    eventMgr.clearFarmer();
                }

                // Mud storm: apply mud slow to all pigs
                if (eventMgr.isMudStormActive() && t % 120 == 1) {
                    for (Pig pig : allPigs) pig.applyMudSlow(120);
                    sidePanel.addEvent("Mud Storm!");
                }

                // Truffle rain: extra item spawns
                if (eventMgr.isTruffleRainActive() && t % 20 == 0) {
                    itemSpawner.tick();
                }

                // --- Player power-up ticks ---
                player.tickPowerUps();

                // --- Magnet pull (every 60 ticks, pull items within 3 cells) ---
                if (player.hasMagnet() && t % 60 == 0) {
                    for (Item item : itemSpawner.getItems()) {
                        if (item.isCollected()) continue;
                        int dist = Math.abs(item.getCol() - player.getCol())
                                 + Math.abs(item.getRow() - player.getRow());
                        if (dist > 0 && dist <= 3 && !item.getType().isHazard) {
                            int dc = Integer.signum(player.getCol() - item.getCol());
                            int dr = Integer.signum(player.getRow() - item.getRow());
                            int nc = item.getCol() + dc;
                            int nr = item.getRow() + dr;
                            if (map.isPassable(nc, nr)) {
                                item.setPosition(nc, nr);
                            }
                        }
                    }
                }

                // --- Passive decay + mud-slow + stun countdown ---
                for (Pig pig : allPigs) {
                    pig.applyDecay();
                    pig.tickMudSlow();
                    pig.tickStun();
                }

                // --- Item spawning (overcast bonus) ---
                double spawnRate = weatherSystem.getSpawnRateMultiplier();
                if (spawnRate >= 1.2 && spawnAccum[0] % 5 == 0) {
                    itemSpawner.tick();
                }
                itemSpawner.tick();

                // --- Survival score (every 60 ticks = 1 second) ---
                if (t > 0 && t % 60 == 0) {
                    session.addScore(ScoreEvent.SURVIVAL_TICK.points);
                }

                // --- Starvation check ---
                if (player.getWeight() <= 10.0) {
                    roundOver[0] = true;
                    session.endGame("Starved out!");
                    gameOverOverlay.show(session.getDeathReason(),
                        session.getLevel(), session.getScore());
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
                        session.addScore(ScoreEvent.LEVEL_COMPLETE.points * session.getLevel());
                        session.addScore(ScoreEvent.WEIGHT_BONUS.points * (int) player.getWeight());
                        session.updateHighScore();
                        roundEndOverlay.show(ranked);
                    }
                    return;
                }

                // --- Weather render ---
                List<int[]> pigPositions = new ArrayList<>();
                for (Pig pig : allPigs) pigPositions.add(new int[]{pig.getCol(), pig.getRow()});
                weatherRenderer.update(weatherSystem, allPigs, pigPositions);

                // --- Pig render ---
                playerRenderer.update();
                dumbRenderer.update();
                cunningRenderer.update();
                ruthlessRenderer.update();

                // --- Item render (include super acorn in list) ---
                List<Item> allItems = new ArrayList<>(itemSpawner.getItems());
                Item saRender = superAcornMgr.getSuperAcorn();
                if (saRender != null && !saRender.isCollected()) allItems.add(saRender);
                itemRenderer.update(allItems, map, allPigs,
                    player.isSniffActive(), player.getCol(), player.getRow());

                // --- Sniff pulse render ---
                sniffRenderer.update(player.isSniffActive(), player.getCol(), player.getRow());

                // --- Golden Truffle render ---
                goldenRenderer.update(goldenMgr.getGoldenTruffle(),
                                      goldenMgr.isPulseActive(),
                                      goldenMgr.getPulseRadius());

                // --- HUD ---
                long sniffCooldownNs = player.getSniffCooldownEnd() - now;
                boolean sniffReady   = sniffCooldownNs <= 0;
                double  sniffSecs    = sniffReady ? 0.0 : sniffCooldownNs / 1_000_000_000.0;
                hudRenderer.update(roundTicks - t, player, allPigs,
                    weatherSystem.getCurrentWeather().name(), sniffReady, sniffSecs,
                    player.isSniffActive());

                // --- Wolf / Farmer render ---
                wolfRenderer.update(eventMgr.getActiveWolf());
                farmerRenderer.update(eventMgr.getActiveFarmer());

                // --- Side panel ---
                sidePanel.update(session, eventMgr);
            }
        };
        activeTimer.start();
    }

    private void awardItemScore(ItemType type) {
        switch (type) {
            case ACORN            -> session.addScore(ScoreEvent.ACORN_COLLECTED.points);
            case COMMON_MUSHROOM  -> session.addScore(ScoreEvent.MUSHROOM_COLLECTED.points);
            case BLACK_TRUFFLE    -> session.addScore(ScoreEvent.BLACK_TRUFFLE_COLLECTED.points);
            case WHITE_TRUFFLE    -> session.addScore(ScoreEvent.WHITE_TRUFFLE_COLLECTED.points);
            default -> {}
        }
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
