package com.example.demo.world;

import javafx.scene.paint.Color;

/**
 * Defines the weather states available in TruffleRush.
 *
 * Each constant carries four attributes:
 *   tintColor             – semi-transparent overlay applied to the map
 *   spawnRateMultiplier   – multiplier on item spawn rate
 *   speedMultiplier       – multiplier on pig movement speed
 *   visibilityRadius      – cell radius of visibility (-1 = unlimited)
 *
 * Cycle order: SUNNY → OVERCAST → RAIN → FOG → SUNNY (repeating)
 */
public enum Weather {

    /** Warm yellow tint; baseline stats. */
    SUNNY    (Color.rgb(255, 240, 180, 0.15), 1.0, 1.0,  -1),

    /** Grey tint; +20 % spawn rate. */
    OVERCAST (Color.rgb(180, 180, 200, 0.20), 1.2, 1.0,  -1),

    /** Blue-grey tint; additional 50 % speed penalty. */
    RAIN     (Color.rgb(100, 120, 180, 0.25), 1.0, 0.5,  -1),

    /** White tint; limits pig visibility to a 4-cell radius. */
    FOG      (Color.rgb(240, 240, 255, 0.35), 1.0, 1.0,   4);

    // -------------------------------------------------------------------------

    /** Semi-transparent colour blended over the map. */
    public final Color  tintColor;

    /** Multiplier applied to item spawn rate. */
    public final double spawnRateMultiplier;

    /** Multiplier applied to pig movement speed. */
    public final double speedMultiplier;

    /**
     * Maximum cell distance at which a pig can see.
     * {@code -1} means unlimited visibility.
     */
    public final int visibilityRadius;

    // Duration ranges in ticks (at 60 fps)
    /** Minimum ticks a weather state lasts (~10 seconds at 60 fps). */
    public static final int MIN_DURATION = 600;

    /** Maximum ticks a weather state lasts (~30 seconds at 60 fps). */
    public static final int MAX_DURATION = 1800;

    // -------------------------------------------------------------------------

    Weather(Color tintColor,
            double spawnRateMultiplier,
            double speedMultiplier,
            int    visibilityRadius) {
        this.tintColor            = tintColor;
        this.spawnRateMultiplier  = spawnRateMultiplier;
        this.speedMultiplier      = speedMultiplier;
        this.visibilityRadius     = visibilityRadius;
    }

    /**
     * Returns the next weather in the cycle: SUNNY→OVERCAST→RAIN→FOG→SUNNY.
     *
     * @return the weather that follows this one
     */
    public Weather next() {
        Weather[] values = Weather.values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
