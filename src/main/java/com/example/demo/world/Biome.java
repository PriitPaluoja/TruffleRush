package com.example.demo.world;

/**
 * Visual + obstacle theme for a level. Picked from the level number, every 3
 * levels: levels 1–3 = FOREST, 4–6 = SWAMP, 7–9 = FARM, then loops.
 *
 * <p>Each biome shifts {@link MapGenerator}'s obstacle ratios — forest favours
 * bushes & rocks, swamp favours mud pits, farm favours fences. The grid tint
 * and a small label in the HUD let the player feel the rotation.
 */
public enum Biome {
    FOREST("Forest"),
    SWAMP ("Swamp"),
    FARM  ("Farm");

    public final String displayName;

    Biome(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the biome for the given level (1-indexed). Cycles every 9 levels. */
    public static Biome forLevel(int level) {
        int idx = ((level - 1) / 3) % values().length;
        return values()[idx];
    }

    public double rockMultiplier()  { return this == FOREST ? 1.3 : (this == FARM ? 0.7 : 1.0); }
    public double bushMultiplier()  { return this == FOREST ? 1.6 : (this == FARM ? 0.5 : 0.8); }
    public double mudMultiplier()   { return this == SWAMP  ? 2.5 : (this == FARM ? 0.5 : 1.0); }
    public double fenceMultiplier() { return this == FARM   ? 2.5 : (this == SWAMP ? 0.5 : 1.0); }
}
