package com.example.demo.world;

import javafx.scene.paint.Color;

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

    /** Berry color sprinkled inside bushes — red in forest, blue in swamp, pale yellow on farms. */
    public Color bushAccent() {
        switch (this) {
            case SWAMP: return Color.rgb(40, 60, 130);
            case FARM:  return Color.rgb(230, 215, 110);
            case FOREST:
            default:    return Color.rgb(200, 40, 40);
        }
    }

    /** Light "facet" highlight color used on top-lit rocks. */
    public Color rockHighlight() {
        switch (this) {
            case SWAMP: return Color.rgb(150, 155, 145);
            case FARM:  return Color.rgb(190, 175, 150);
            case FOREST:
            default:    return Color.rgb(180, 180, 185);
        }
    }

    /** Small bubble color floating in mud pits. */
    public Color mudAccent() {
        switch (this) {
            case SWAMP: return Color.rgb(70, 55, 30, 0.85);
            case FARM:  return Color.rgb(140, 110, 70, 0.85);
            case FOREST:
            default:    return Color.rgb(80, 50, 25, 0.85);
        }
    }

    /**
     * Soft tint blended into the base tile color so the floor reads
     * differently in each biome. Mixed at ~25% strength by GridRenderer.
     */
    public Color tileTint() {
        switch (this) {
            case SWAMP: return Color.rgb(80, 95, 60);     // murky green-grey
            case FARM:  return Color.rgb(180, 150, 90);   // dry tan
            case FOREST:
            default:    return Color.rgb(70, 130, 50);    // saturated grass
        }
    }

    /** Color used for the decoration pass (grass tufts, reeds, dirt patches). */
    public Color decorationColor() {
        switch (this) {
            case SWAMP: return Color.rgb(60, 110, 70, 0.55);
            case FARM:  return Color.rgb(120, 90, 50, 0.55);
            case FOREST:
            default:    return Color.rgb(40, 110, 40, 0.65);
        }
    }
}
