package com.example.demo.entity;

import com.example.demo.world.GameMap;
import javafx.scene.paint.Color;

/**
 * Base class for all pigs in TruffleRush.
 *
 * Manages grid position, weight, facing direction, and visual identity.
 * Weight drives the pig's rendered size via {@link #getRadius()}.
 */
public class Pig {

    /** Horizontal grid position (column index). */
    protected int col;

    /** Vertical grid position (row index). */
    protected int row;

    /**
     * Current weight of the pig.
     * Starts at 50.0, decays over time, grows when truffles are eaten.
     * Clamped to a minimum of 10.0.
     */
    protected double weight;

    /** The direction the pig last moved (or is currently facing). */
    protected Direction facing;

    /** Display name of this pig. */
    protected final String name;

    /** Body colour used when rendering the pig. */
    protected final Color color;

    /**
     * Remaining ticks of mud-slow debuff.
     * Counts down to zero each tick; movement is slowed while {@code > 0}.
     */
    protected int mudSlowTicks;

    /**
     * External speed multiplier set by the game loop (e.g. from weather).
     * Values &lt; 1.0 slow the pig; 1.0 = no effect.
     */
    protected double externalSpeedMult = 1.0;

    /** Stun state — stunned pigs cannot move. */
    protected boolean stunned;
    protected int stunTicks;

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final double WEIGHT_INITIAL = 50.0;
    private static final double WEIGHT_MIN     = 10.0;
    private static final double DEFAULT_WEIGHT_DECAY = 0.008;

    /** Configurable decay rate (can be increased for harder levels). */
    protected double weightDecayRate = DEFAULT_WEIGHT_DECAY;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a pig with the given identity and starting grid position.
     *
     * @param name     display name
     * @param color    body colour
     * @param startCol starting column on the grid
     * @param startRow starting row on the grid
     */
    public Pig(String name, Color color, int startCol, int startRow) {
        this.name   = name;
        this.color  = color;
        this.col    = startCol;
        this.row    = startRow;
        this.weight       = WEIGHT_INITIAL;
        this.facing       = Direction.NONE;
        this.mudSlowTicks = 0;
    }

    // -------------------------------------------------------------------------
    // Weight management
    // -------------------------------------------------------------------------

    /**
     * Subtracts the passive decay amount from the pig's weight,
     * clamping the result to the minimum weight.
     */
    public void applyDecay() {
        weight = Math.max(WEIGHT_MIN, weight - weightDecayRate);
    }

    /** Sets the weight decay rate per tick. */
    public void setWeightDecayRate(double rate) {
        this.weightDecayRate = rate;
    }

    /**
     * Adds {@code delta} to the pig's weight, clamping to the minimum weight.
     *
     * @param delta the amount to add (may be negative)
     */
    public void addWeight(double delta) {
        weight = Math.max(WEIGHT_MIN, weight + delta);
    }

    // -------------------------------------------------------------------------
    // Mud-slow debuff
    // -------------------------------------------------------------------------

    /**
     * Applies a mud-slow debuff for the given number of ticks.
     * Replaces any currently active debuff duration.
     *
     * @param ticks number of ticks the pig should be slowed
     */
    public void applyMudSlow(int ticks) {
        this.mudSlowTicks = ticks;
    }

    /**
     * Returns {@code true} while a mud-slow debuff is active.
     *
     * @return {@code true} if mud-slowed
     */
    public boolean isMudSlowed() {
        return mudSlowTicks > 0;
    }

    /**
     * Decrements the mud-slow timer by one tick (stops at zero).
     * Call once per game tick for every pig.
     */
    public void tickMudSlow() {
        if (mudSlowTicks > 0) {
            mudSlowTicks--;
        }
    }

    // -------------------------------------------------------------------------
    // Radius
    // -------------------------------------------------------------------------

    /**
     * Returns the pig's render radius derived from its current weight.
     * Range: [12, 28].
     *
     * @return radius in pixels
     */
    public double getRadius() {
        return Math.min(28, Math.max(12, 12 + weight * 0.15));
    }

    // -------------------------------------------------------------------------
    // Movement
    // -------------------------------------------------------------------------

    /**
     * Checks whether the pig can move to the given grid position.
     * Returns {@code false} if the position is out of bounds or the target
     * cell is not passable.
     *
     * @param newCol target column
     * @param newRow target row
     * @param map    the current game map
     * @return {@code true} if the move is valid
     */
    public boolean canMove(int newCol, int newRow, GameMap map) {
        if (newCol < 0 || newCol >= map.getColumns()) return false;
        if (newRow < 0 || newRow >= map.getRows())    return false;
        return map.getCell(newCol, newRow).isPassable();
    }

    /**
     * Teleports the pig to the specified grid position without validation.
     * Callers should check {@link #canMove} first.
     *
     * @param newCol target column
     * @param newRow target row
     */
    public void moveTo(int newCol, int newRow) {
        this.col = newCol;
        this.row = newRow;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** Sets the external speed multiplier (e.g. from weather). */
    public void setExternalSpeedMult(double mult) {
        this.externalSpeedMult = Math.max(0.01, mult);
    }

    public double getExternalSpeedMult() { return externalSpeedMult; }

    /** Stuns the pig for the given number of ticks. */
    public void stun(int ticks) { this.stunned = true; this.stunTicks = ticks; }

    /** Returns true if the pig is currently stunned. */
    public boolean isStunned() { return stunned; }

    /** Decrements stun timer. Call once per tick. */
    public void tickStun() {
        if (stunTicks > 0) stunTicks--;
        if (stunTicks <= 0) stunned = false;
    }

    public int getCol()          { return col; }
    public int getRow()          { return row; }
    public double getWeight()    { return weight; }
    public Direction getFacing() { return facing; }
    public String getName()      { return name; }
    public Color getColor()      { return color; }
}
