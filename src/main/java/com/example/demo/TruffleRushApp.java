package com.example.demo;

import com.example.demo.core.EventBus;
import com.example.demo.core.GameEvent;
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
import com.example.demo.render.GoldenTruffleRenderer;
import com.example.demo.render.GridRenderer;
import com.example.demo.render.HudRenderer;
import com.example.demo.render.ItemRenderer;
import com.example.demo.render.ObstacleRenderer;
import com.example.demo.render.PigRenderer;
import com.example.demo.render.RoundEndOverlay;
import com.example.demo.render.SniffRenderer;
import com.example.demo.render.WeatherRenderer;
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

    /** 3-minute round at 60 ticks/s. */
    static final int ROUND_TICKS = 10_800;

    /** Win by weight threshold. */
    static final double WIN_WEIGHT = 150.0;

    private Direction heldDirection = Direction.NONE;
    private AnimationTimer activeTimer;

    @Override
    public void start(Stage stage) {
        // Stop any previous game loop
        if (activeTimer != null) {
            activeTimer.stop();
        }

        // --- World ---
        GameMap map = new GameMap();
        new MapGenerator(System.currentTimeMillis()).generate(map);

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
            18, 1, new RandomBehavior(), 1
        );
        AIPig cunning = new AIPig(
            "Cunning", Color.rgb(139, 90, 43),
            1, 13, new CunningBehavior(itemSpawner), 6
        );
        AIPig ruthless = new AIPig(
            "Ruthless", Color.rgb(60, 60, 60),
            18, 13, ruthlessBehavior, 3
        );
        List<Pig> allPigs = new ArrayList<>(List.of(player, dumb, cunning, ruthless));

        // --- Golden Truffle ---
        GoldenTruffleManager goldenMgr = new GoldenTruffleManager(ROUND_TICKS, map, allPigs);

        // --- Renderers ---
        int mapW = GameMap.COLS * GameMap.TILE_SIZE;
        int mapH = GameMap.ROWS * GameMap.TILE_SIZE;

        GridRenderer          gridRenderer      = new GridRenderer();
        ObstacleRenderer      obstacleRenderer  = new ObstacleRenderer();
        ItemRenderer          itemRenderer      = new ItemRenderer();
        GoldenTruffleRenderer goldenRenderer    = new GoldenTruffleRenderer();
        WeatherRenderer       weatherRenderer   = new WeatherRenderer(mapW, mapH);
        PigRenderer playerRenderer   = new PigRenderer(player);
        PigRenderer dumbRenderer     = new PigRenderer(dumb);
        PigRenderer cunningRenderer  = new PigRenderer(cunning);
        PigRenderer ruthlessRenderer = new PigRenderer(ruthless);
        SniffRenderer         sniffRenderer     = new SniffRenderer();
        HudRenderer           hudRenderer       = new HudRenderer(mapW);
        RoundEndOverlay       roundEndOverlay   = new RoundEndOverlay(mapW, mapH);

        obstacleRenderer.render(map);

        // --- Play Again wiring ---
        roundEndOverlay.setOnPlayAgain(() -> {
            heldDirection = Direction.NONE;
            start(stage);
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
            hudRenderer.getGroup(),
            roundEndOverlay.getGroup()
        );

        Scene scene = new Scene(root, mapW, mapH, Color.BLACK);

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

        stage.setTitle("TruffleRush");
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
                double roundProgress = Math.min(1.0, (double) t / ROUND_TICKS);
                gridRenderer.update(roundProgress);

                // --- Weather tick ---
                weatherSystem.tick();
                boolean isFog = weatherSystem.getCurrentWeather() == Weather.FOG;

                // --- Apply weather speed multiplier to all pigs (interpolated during transitions) ---
                double weatherSpeed = weatherSystem.getSpeedMultiplier();
                // Fog halves AI recompute frequency by halving their external speed mult
                // (AIPig uses this to scale its recomputeInterval effectively)
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

                // --- Collision resolution (always runs, regardless of weather) ---
                resolveCollisions(allPigs, prevPC, prevPR,
                    prevDC, prevDR, prevCC, prevCR, prevRC, prevRR);
                spawnAccum[0]++;

                // --- Item collection for all pigs ---
                for (Pig pig : allPigs) {
                    Item item = itemSpawner.getItemAt(pig.getCol(), pig.getRow());
                    if (item != null) {
                        pig.addWeight(item.getType().weightDelta);
                        if (item.getType() == com.example.demo.item.ItemType.MUD_SPLASH) {
                            pig.applyMudSlow(180);
                        }
                        itemSpawner.collectItem(item);
                        eventBus.publish(GameEvent.ITEM_COLLECTED, item);
                        eventBus.publish(GameEvent.WEIGHT_CHANGED, pig);
                    }
                }

                // --- Golden Truffle ---
                Item spawned = goldenMgr.tick(t, map, allPigs);
                if (spawned != null) {
                    // Notify Ruthless of the golden truffle location
                    ruthlessBehavior.setGoldenTruffleTarget(spawned.getCol(), spawned.getRow());
                    eventBus.publish(GameEvent.GOLDEN_TRUFFLE_SPAWNED, spawned);
                }
                // Check if any pig is on the golden truffle cell
                Item gt = goldenMgr.getGoldenTruffle();
                if (gt != null && !gt.isCollected()) {
                    for (Pig pig : allPigs) {
                        if (pig.getCol() == gt.getCol() && pig.getRow() == gt.getRow()) {
                            pig.addWeight(gt.getType().weightDelta);
                            goldenMgr.onCollected();
                            ruthlessBehavior.setGoldenTruffleTarget(-1, -1);
                            eventBus.publish(GameEvent.ITEM_COLLECTED, gt);
                            break;
                        }
                    }
                }

                // --- Passive decay + mud-slow countdown ---
                for (Pig pig : allPigs) {
                    pig.applyDecay();
                    pig.tickMudSlow();
                }

                // --- Item spawning (overcast bonus) ---
                double spawnRate = weatherSystem.getSpawnRateMultiplier();
                if (spawnRate >= 1.2 && spawnAccum[0] % 5 == 0) {
                    itemSpawner.tick();
                }
                itemSpawner.tick();

                // --- Round end check ---
                boolean roundShouldEnd = t >= ROUND_TICKS;
                if (!roundShouldEnd) {
                    for (Pig pig : allPigs) {
                        if (pig.getWeight() >= WIN_WEIGHT) { roundShouldEnd = true; break; }
                    }
                }
                if (roundShouldEnd) {
                    roundOver[0] = true;
                    List<Pig> ranked = new ArrayList<>(allPigs);
                    ranked.sort(Comparator.comparingDouble(Pig::getWeight).reversed());
                    roundEndOverlay.show(ranked);
                    eventBus.publish(GameEvent.ROUND_ENDED, ranked);
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

                // --- Item render (sniff reveals bush items within 2 cells) ---
                itemRenderer.update(itemSpawner.getItems(), map, allPigs,
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
                hudRenderer.update(ROUND_TICKS - t, player, allPigs,
                    weatherSystem.getCurrentWeather().name(), sniffReady, sniffSecs,
                    player.isSniffActive());
            }
        };
        activeTimer.start();
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
