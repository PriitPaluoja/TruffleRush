package com.example.demo.world;

/**
 * Defines the types of obstacles that can occupy a {@link Cell} on the game map.
 *
 * Each constant carries two attributes:
 *   passable        – whether an entity may enter the cell
 *   speedMultiplier – fraction of normal movement speed when crossing the cell
 */
public enum Obstacle {

    /** Solid rock — cannot be entered, no speed effect. */
    ROCK(false, 1.0),

    /** Dense bush — cannot be entered, hides items until an entity is adjacent. */
    BUSH(false, 1.0),

    /** Muddy pit — can be entered but movement speed is halved. */
    MUD_PIT(true, 0.5),

    /** Wooden fence — cannot be entered, no speed effect. */
    FENCE(false, 1.0);

    // -------------------------------------------------------------------------

    private final boolean passable;
    private final double  speedMultiplier;

    Obstacle(boolean passable, double speedMultiplier) {
        this.passable        = passable;
        this.speedMultiplier = speedMultiplier;
    }

    /**
     * Returns {@code true} when an entity may enter a cell carrying this obstacle.
     *
     * @return passability flag
     */
    public boolean isPassable() {
        return passable;
    }

    /**
     * Returns the fraction of normal movement speed that applies when crossing
     * a cell carrying this obstacle.  A value of {@code 1.0} means no penalty;
     * {@code 0.5} means half speed.
     *
     * @return speed multiplier in the range (0, 1]
     */
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }
}
