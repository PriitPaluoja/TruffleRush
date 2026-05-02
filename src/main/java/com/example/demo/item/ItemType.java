package com.example.demo.item;

/**
 * Enumeration of all item types that can appear on the game map.
 *
 * <p>Each constant carries:
 * <ul>
 *   <li>{@code weightDelta}   — how much weight the pig gains (positive) or loses (negative)</li>
 *   <li>{@code rarityWeight}  — used in weighted-random spawning; higher = more common.
 *                               {@code 0} means the item is never spawned by {@link ItemSpawner}.</li>
 *   <li>{@code isHazard}      — {@code true} for items that harm or hinder the pig</li>
 *   <li>{@code description}   — brief visual description</li>
 * </ul>
 */
public enum ItemType {

    BLACK_TRUFFLE  ( +8,  10, false, "6-sided polygon, dark brown"),
    WHITE_TRUFFLE  (+15,   3, false, "6-sided polygon, white"),
    COMMON_MUSHROOM( +3,  20, false, "circle, tan"),
    ACORN          ( +1,  30, false, "small circle, brown"),
    CELERY         ( -5,  15, true,  "thin rectangle, green"),
    DIET_PILL      (-10,   3, true,  "small rectangle, white/red"),
    MUD_SPLASH     (  0,   8, true,  "ellipse, brown"),        // slows pig temporarily
    GOLDEN_TRUFFLE (+30,   0, false, "polygon, gold"),       // spawned separately
    SPEED_MUSHROOM (  0,   5, false, "circle, blue"),        // 2x speed for 5 seconds
    SHIELD_ACORN   (  0,   3, false, "circle, gold ring"),   // blocks next negative hit
    MAGNET_TRUFFLE (  0,   2, false, "hexagon, purple"),     // pulls nearby items for 6s
    DECOY_MUSHROOM (  0,   4, false, "circle, orange"),      // fake golden truffle lures AI
    SUPER_ACORN    ( +5,   0, false, "circle, golden glow"), // triggers super pig mode
    GREATER_SPEED  (  0,   1, false, "circle, deep blue"),   // 10s speed boost
    MAGNET_CROWN   (  0,   1, false, "hexagon, magenta");    // 12s magnet, also wider range

    /** Weight change applied to the pig when this item is collected. */
    public final int weightDelta;

    /**
     * Relative probability weight used by the spawner's weighted-random selection.
     * A value of {@code 0} means the item is excluded from normal spawning.
     */
    public final int rarityWeight;

    /** {@code true} for items that are harmful or cause a negative status effect. */
    public final boolean isHazard;

    /** Short human-readable visual description of the item's appearance. */
    public final String description;

    ItemType(int weightDelta, int rarityWeight, boolean isHazard, String description) {
        this.weightDelta  = weightDelta;
        this.rarityWeight = rarityWeight;
        this.isHazard     = isHazard;
        this.description  = description;
    }
}
