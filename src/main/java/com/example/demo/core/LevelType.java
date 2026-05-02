package com.example.demo.core;

/**
 * Categorises a level into a normal round or one of three "elite" challenge
 * variants. Picked by {@link GameSession#getLevelType()} — every 5th level
 * cycles through ARENA → SWARM → GAUNTLET, the rest are NORMAL.
 */
public enum LevelType {
    /** Standard forage / survive level. */
    NORMAL,
    /** Small map, no items, multiple wolves chase you. Survive = bonus. */
    ARENA,
    /** Items rain at high rate; hazards scattered; a feast in chaos. */
    SWARM,
    /** Faster AI rivals, dense obstacles, race to last man standing. */
    GAUNTLET;

    /** Display label for the HUD. */
    public String label() {
        return switch (this) {
            case ARENA    -> "ARENA";
            case SWARM    -> "SWARM";
            case GAUNTLET -> "GAUNTLET";
            default       -> "";
        };
    }
}
