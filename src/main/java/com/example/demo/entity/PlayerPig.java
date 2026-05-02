package com.example.demo.entity;

import com.example.demo.world.GameMap;
import javafx.scene.paint.Color;

/**
 * The human-controlled pig.
 * <p>
 * Adds rate-limited movement and a "sniff" ability that reveals nearby truffles.
 */
public class PlayerPig extends Pig {

    // -------------------------------------------------------------------------
    // Movement timing
    // -------------------------------------------------------------------------

    /**
     * Minimum time between moves: 150 ms expressed in nanoseconds.
     */
    public static final long MOVE_DELAY_NS = 150_000_000L;
    /**
     * How long a single sniff lasts: 2 seconds.
     */
    public static final long SNIFF_DURATION_NS = 2_000_000_000L;

    // -------------------------------------------------------------------------
    // Sniff ability
    // -------------------------------------------------------------------------
    /**
     * Cooldown between sniffs: 8 seconds.
     */
    public static final long SNIFF_COOLDOWN_NS = 8_000_000_000L;
    private long lastMoveTime;
    private boolean sniffActive;
    private long sniffEndTime;
    private long sniffCooldownEnd;

    // --- Power-ups ---
    private int speedBoostTicks;
    private boolean hasShield;
    private int magnetTicks;
    private boolean superPigActive;
    private int superPigTicks;
    private boolean stunned;
    private int stunTicks;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates the player pig at the given starting grid position.
     *
     * @param startCol starting column on the grid
     * @param startRow starting row on the grid
     */
    public PlayerPig(int startCol, int startRow) {
        super("Player", Color.rgb(255, 180, 200), startCol, startRow);
        this.lastMoveTime = 0L;
        this.sniffActive = false;
        this.sniffEndTime = 0L;
        this.sniffCooldownEnd = 0L;
    }

    // -------------------------------------------------------------------------
    // Movement
    // -------------------------------------------------------------------------

    /**
     * Attempts to move the pig one step in the given direction.
     * <p>
     * The move is accepted only when:
     * <ol>
     *   <li>{@link #MOVE_DELAY_NS} nanoseconds have elapsed since the last move.</li>
     *   <li>The target cell exists and is passable ({@link #canMove}).</li>
     * </ol>
     * <p>
     * On a successful move the pig's {@code facing} direction is updated and
     * {@code lastMoveTime} is set to {@code now}.
     *
     * @param dir the desired direction of movement
     * @param map the current game map for bounds/passability checks
     * @param now current time from {@link System#nanoTime()}
     * @return {@code true} if the pig actually moved
     */
    public boolean tryMove(Direction dir, GameMap map, long now) {
        if (dir == Direction.NONE) return false;

        if (stunned) return false;
        // Base delay: tripled when mud-slowed, halved when speed-boosted
        long baseDelay = isMudSlowed() ? MOVE_DELAY_NS * 3 : MOVE_DELAY_NS;
        if (speedBoostTicks > 0 || superPigActive) baseDelay /= 2;
        // Cell speed multiplier: 0.5 when standing on a mud pit (doubles delay)
        double cellMult = map.getCell(col, row).getSpeedMultiplier();
        // Combined: weather and cell effects both lengthen the delay
        long effectiveDelay = (long) (baseDelay / (cellMult * externalSpeedMult));
        if (now - lastMoveTime < effectiveDelay) return false;

        int targetCol = col + dir.dc;
        int targetRow = row + dir.dr;

        if (!canMove(targetCol, targetRow, map)) return false;

        moveTo(targetCol, targetRow);
        facing = dir;
        lastMoveTime = now;
        return true;
    }

    // -------------------------------------------------------------------------
    // Sniff ability
    // -------------------------------------------------------------------------

    /**
     * Activates the sniff ability if the cooldown has expired.
     *
     * @param now current time from {@link System#nanoTime()}
     * @return {@code true} if sniff was successfully activated
     */
    public boolean trySniff(long now) {
        if (now < sniffCooldownEnd) return false;

        sniffActive = true;
        sniffEndTime = now + SNIFF_DURATION_NS;
        sniffCooldownEnd = now + SNIFF_COOLDOWN_NS;
        return true;
    }

    /**
     * Deactivates sniff once its duration has expired.
     * Should be called every game tick.
     *
     * @param now current time from {@link System#nanoTime()}
     */
    public void updateSniff(long now) {
        if (sniffActive && now >= sniffEndTime) {
            sniffActive = false;
        }
    }

    // -------------------------------------------------------------------------
    // Power-ups
    // -------------------------------------------------------------------------

    /** Adds {@code ticks} to the speed-boost timer (stacks instead of overwriting). */
    public void activateSpeedBoost(int ticks) { this.speedBoostTicks += ticks; }
    public void activateShield() { this.hasShield = true; }
    public boolean consumeShield() {
        if (hasShield) { hasShield = false; return true; }
        return false;
    }
    /** Adds {@code ticks} to the magnet timer (stacks instead of overwriting). */
    public void activateMagnet(int ticks) { this.magnetTicks += ticks; }
    public void activateSuperPig(int ticks) {
        this.superPigActive = true;
        this.superPigTicks = ticks;
    }

    public void tickPowerUps() {
        if (speedBoostTicks > 0) speedBoostTicks--;
        if (magnetTicks > 0) magnetTicks--;
        if (superPigTicks > 0) {
            superPigTicks--;
            if (superPigTicks <= 0) superPigActive = false;
        }
        if (stunTicks > 0) stunTicks--;
        if (stunTicks <= 0) stunned = false;
    }

    public void stun(int ticks) { this.stunned = true; this.stunTicks = ticks; }
    public boolean isStunned() { return stunned; }

    public boolean hasSpeedBoost() { return speedBoostTicks > 0; }
    public boolean hasShield() { return hasShield; }
    public boolean hasMagnet() { return magnetTicks > 0; }
    public boolean isSuperPig() { return superPigActive; }
    public int getSpeedBoostTicks() { return speedBoostTicks; }
    public int getShieldTicks() { return hasShield ? 1 : 0; }
    public int getMagnetTicks() { return magnetTicks; }
    public int getSuperPigTicks() { return superPigTicks; }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public boolean isSniffActive() {
        return sniffActive;
    }

    public long getSniffCooldownEnd() {
        return sniffCooldownEnd;
    }
}
