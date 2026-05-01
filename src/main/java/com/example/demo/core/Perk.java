package com.example.demo.core;

/**
 * Permanent run-start perks bought with banked truffles between runs.
 * Each level is bought additively; cost grows with level.
 */
public enum Perk {
    EXTRA_WEIGHT       ("Well Fed",       "+5 starting weight per level",     3, 5),
    SLOWER_DECAY       ("Iron Stomach",   "-0.001 weight decay per level",    3, 6),
    START_WITH_SHIELD  ("Lucky Acorn",    "Start each level with a shield",   1, 12),
    EXTRA_MAGNET_RANGE ("Big Snout",      "+1 magnet range per level",        2, 8),
    DECAY_GRACE        ("Slow Starter",   "Skip decay for first 5s per level",1, 10);

    public final String displayName;
    public final String description;
    public final int    maxLevel;
    public final int    baseCost;

    Perk(String displayName, String description, int maxLevel, int baseCost) {
        this.displayName = displayName;
        this.description = description;
        this.maxLevel    = maxLevel;
        this.baseCost    = baseCost;
    }

    /** Cost to buy the next level (current = currentLevel). Returns -1 if maxed. */
    public int costForLevel(int currentLevel) {
        if (currentLevel >= maxLevel) return -1;
        return baseCost * (currentLevel + 1);
    }
}
