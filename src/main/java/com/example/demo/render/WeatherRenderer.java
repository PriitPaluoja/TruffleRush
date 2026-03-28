package com.example.demo.render;

import com.example.demo.entity.Pig;
import com.example.demo.world.GameMap;
import com.example.demo.world.Weather;
import com.example.demo.world.WeatherSystem;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.List;
import java.util.Random;

/**
 * Renders weather visual effects on top of the game world.
 *
 * <p>Three separate layers are managed (back to front inside the returned group):</p>
 * <ol>
 *   <li><b>Tint overlay</b> – a full-map semi-transparent {@link Rectangle} whose
 *       colour is driven by the interpolated {@link WeatherSystem} tint.</li>
 *   <li><b>Rain layer</b> – a {@link Group} that is repopulated each tick with
 *       ~15–20 short diagonal {@link Line} objects while {@link Weather#RAIN}
 *       is active.</li>
 *   <li><b>Fog layer</b> – a white {@link Rectangle} with circular cutouts around
 *       every pig position while {@link Weather#FOG} is active.</li>
 * </ol>
 *
 * <p>Use {@link #getGroup()} to obtain the root node and add it to the scene
 * graph after the game-world nodes but before the HUD.</p>
 */
public class WeatherRenderer {

    private static final int TILE_SIZE = GameMap.TILE_SIZE;

    /** Fog hole radius: 4 cells × TILE_SIZE pixels. */
    private static final double FOG_HOLE_RADIUS = 4.0 * TILE_SIZE;

    // -------------------------------------------------------------------------
    // Scene-graph nodes
    // -------------------------------------------------------------------------

    /** Root group returned to callers. */
    private final Group root;

    /** Full-map tint rectangle. */
    private final Rectangle tintRect;

    /** Group holding per-frame rain {@link Line} objects. */
    private final Group rainGroup;

    /** Group holding the fog {@link Shape} (replaced every frame when fogging). */
    private final Group fogGroup;

    private final int mapWidth;
    private final int mapHeight;

    private final Random random = new Random();

    // --- Rain drop pool (persistent across frames for smooth animation) ---
    private static final int RAIN_DROP_COUNT = 100;
    private static final double RAIN_FALL_SPEED = 8.0;   // px per tick
    private static final double RAIN_DRIFT      = 2.5;   // px horizontal drift per tick
    private double[] rainX;
    private double[] rainY;
    private double[] rainLen;
    private boolean  rainInitialized = false;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a WeatherRenderer sized to the given pixel dimensions.
     *
     * @param mapWidth  total pixel width  of the game map
     * @param mapHeight total pixel height of the game map
     */
    public WeatherRenderer(int mapWidth, int mapHeight) {
        this.mapWidth  = mapWidth;
        this.mapHeight = mapHeight;

        // Tint overlay – starts fully transparent
        tintRect = new Rectangle(0, 0, mapWidth, mapHeight);
        tintRect.setFill(Color.TRANSPARENT);
        tintRect.setMouseTransparent(true);

        // Rain group – populated each frame
        rainGroup = new Group();
        rainGroup.setMouseTransparent(true);

        // Fog group – populated each frame
        fogGroup = new Group();
        fogGroup.setMouseTransparent(true);

        root = new Group(tintRect, rainGroup, fogGroup);
        root.setMouseTransparent(true);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the root {@link Group} that contains all weather visual layers.
     * Add this to the scene graph on top of the game world.
     */
    public Group getGroup() {
        return root;
    }

    /**
     * Updates all weather visual layers for the current tick.
     *
     * @param weather      the live weather state machine
     * @param pigs         the list of all pigs (used for fog cutouts)
     * @param pigPositions pre-extracted pig grid positions as {@code int[]{col, row}}
     *                     (may be derived from {@code pigs} if needed)
     */
    public void update(WeatherSystem weather, List<Pig> pigs, List<int[]> pigPositions) {
        // 1. Tint
        updateTint(weather.getInterpolatedTintColor());

        // 2. Rain – active only during RAIN (or transitioning into/out of it)
        boolean raining = weather.getCurrentWeather() == Weather.RAIN
                || (weather.isTransitioning()
                    && weatherMatchesRain(weather));
        updateRain(raining);

        // 3. Fog – active only during FOG (or transitioning into/out of it)
        boolean fogging = weather.getCurrentWeather() == Weather.FOG
                || (weather.isTransitioning()
                    && weatherMatchesFog(weather));
        updateFog(fogging, pigPositions);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Updates the tint rectangle's fill to the given interpolated colour.
     *
     * @param tintColor the colour to apply (includes desired opacity)
     */
    private void updateTint(Color tintColor) {
        tintRect.setFill(tintColor);
    }

    /**
     * Animates a persistent pool of rain drops that fall diagonally each tick.
     * When {@code raining} is {@code false}, the rain group is cleared and the
     * pool is reset so the next rain starts fresh.
     *
     * @param raining whether rain lines should be drawn this frame
     */
    private void updateRain(boolean raining) {
        rainGroup.getChildren().clear();

        if (!raining) {
            rainInitialized = false;
            return;
        }

        // Initialize pool on first rain tick
        if (!rainInitialized) {
            rainX   = new double[RAIN_DROP_COUNT];
            rainY   = new double[RAIN_DROP_COUNT];
            rainLen = new double[RAIN_DROP_COUNT];
            for (int i = 0; i < RAIN_DROP_COUNT; i++) {
                rainX[i]   = random.nextDouble() * mapWidth;
                rainY[i]   = random.nextDouble() * mapHeight;
                rainLen[i] = 14.0 + random.nextDouble() * 12.0; // [14, 26] px
            }
            rainInitialized = true;
        }

        // Advance each drop and draw
        for (int i = 0; i < RAIN_DROP_COUNT; i++) {
            rainX[i] += RAIN_DRIFT;
            rainY[i] += RAIN_FALL_SPEED;

            // Wrap around when a drop falls off screen
            if (rainY[i] > mapHeight) {
                rainY[i] = -rainLen[i];
                rainX[i] = random.nextDouble() * mapWidth;
            }
            if (rainX[i] > mapWidth) {
                rainX[i] -= mapWidth;
            }

            double len = rainLen[i];
            Line line = new Line(rainX[i], rainY[i],
                                 rainX[i] + len * 0.3, rainY[i] + len);
            line.setStroke(Color.rgb(170, 200, 255, 0.55));
            line.setStrokeWidth(1.5);
            rainGroup.getChildren().add(line);
        }
    }

    /**
     * Builds a fog overlay – a white rectangle covering the whole map with
     * circular holes cut around each pig's pixel position.
     *
     * <p>When {@code fogging} is {@code false} the fog group is simply cleared.
     * When {@code true}, {@link Shape#subtract(Shape, Shape)} is used to cut
     * a {@link Circle} of radius {@link #FOG_HOLE_RADIUS} for each pig out of a
     * base {@link Rectangle}, resulting in a shape that is opaque everywhere
     * except within 4 tiles of any pig.</p>
     *
     * @param fogging      whether the fog overlay should be rendered
     * @param pigPositions grid positions as {@code int[]{col, row}} for each pig
     */
    private void updateFog(boolean fogging, List<int[]> pigPositions) {
        fogGroup.getChildren().clear();

        if (!fogging || pigPositions.isEmpty()) {
            return;
        }

        // Base opaque white rectangle covering the entire map
        Shape fog = new Rectangle(0, 0, mapWidth, mapHeight);
        fog.setFill(Color.rgb(240, 240, 255, 0.85));

        // Subtract a circle for each pig
        for (int[] pos : pigPositions) {
            double cx = pos[0] * TILE_SIZE + TILE_SIZE / 2.0;
            double cy = pos[1] * TILE_SIZE + TILE_SIZE / 2.0;

            Circle hole = new Circle(cx, cy, FOG_HOLE_RADIUS);
            fog = Shape.subtract(fog, hole);
        }

        fog.setFill(Color.rgb(240, 240, 255, 0.85));
        fog.setStroke(null);
        fog.setMouseTransparent(true);
        fogGroup.getChildren().add(fog);
    }

    // -------------------------------------------------------------------------
    // Transition helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the weather system is currently transitioning
     * toward or away from {@link Weather#RAIN}.
     */
    private boolean weatherMatchesRain(WeatherSystem weather) {
        // WeatherSystem does not expose nextWeather directly; infer from the
        // fact that RAIN follows OVERCAST and precedes FOG in the cycle.
        // We activate rain lines if the current weather is adjacent to RAIN.
        Weather cur = weather.getCurrentWeather();
        return cur == Weather.OVERCAST || cur == Weather.RAIN;
    }

    /**
     * Returns {@code true} when the weather system is currently transitioning
     * toward or away from {@link Weather#FOG}.
     */
    private boolean weatherMatchesFog(WeatherSystem weather) {
        Weather cur = weather.getCurrentWeather();
        return cur == Weather.RAIN || cur == Weather.FOG;
    }
}
