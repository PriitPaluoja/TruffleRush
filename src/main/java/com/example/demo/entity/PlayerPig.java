package com.example.demo.entity;

import com.example.demo.world.GameMap;
import javafx.scene.paint.Color;

/**
 * The human-controlled pig.
 *
 * Adds rate-limited movement and a "sniff" ability that reveals nearby truffles.
 */
public class PlayerPig extends Pig {

    // -------------------------------------------------------------------------
    // Movement timing
    // -------------------------------------------------------------------------

    /** Timestamp (nanoTime) of the last accepted move. */
    private long lastMoveTime;

    /** Minimum time between moves: 150 ms expressed in nanoseconds. */
    public static final long MOVE_DELAY_NS = 150_000_000L;

    // -------------------------------------------------------------------------
    // Sniff ability
    // -------------------------------------------------------------------------

    /** Whether the sniff ability is currently active. */
    private boolean sniffActive;

    /** Timestamp (nanoTime) at which the active sniff expires. */
    private long sniffEndTime;

    /** Timestamp (nanoTime) before which sniff cannot be re-activated. */
    private long sniffCooldownEnd;

    /** How long a single sniff lasts: 2 seconds. */
    public static final long SNIFF_DURATION_NS = 2_000_000_000L;

    /** Cooldown between sniffs: 8 seconds. */
    public static final long SNIFF_COOLDOWN_NS = 8_000_000_000L;

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
        this.lastMoveTime    = 0L;
        this.sniffActive     = false;
        this.sniffEndTime    = 0L;
        this.sniffCooldownEnd = 0L;
    }

    // -------------------------------------------------------------------------
    // Movement
    // -------------------------------------------------------------------------

    /**
     * Attempts to move the pig one step in the given direction.
     *
     * The move is accepted only when:
     * <ol>
     *   <li>{@link #MOVE_DELAY_NS} nanoseconds have elapsed since the last move.</li>
     *   <li>The target cell exists and is passable ({@link #canMove}).</li>
     * </ol>
     *
     * On a successful move the pig's {@code facing} direction is updated and
     * {@code lastMoveTime} is set to {@code now}.
     *
     * @param dir  the desired direction of movement
     * @param map  the current game map for bounds/passability checks
     * @param now  current time from {@link System#nanoTime()}
     * @return {@code true} if the pig actually moved
     */
    public boolean tryMove(Direction dir, GameMap map, long now) {
        if (dir == Direction.NONE) return false;

        // Base delay: tripled when mud-slowed by an item debuff
        long baseDelay = isMudSlowed() ? MOVE_DELAY_NS * 3 : MOVE_DELAY_NS;
        // Cell speed multiplier: 0.5 when standing on a mud pit (doubles delay)
        double cellMult = map.getCell(col, row).getSpeedMultiplier();
        // Combined: weather and cell effects both lengthen the delay
        long effectiveDelay = (long)(baseDelay / (cellMult * externalSpeedMult));
        if (now - lastMoveTime < effectiveDelay) return false;

        int targetCol = col + dir.dc;
        int targetRow = row + dir.dr;

        if (!canMove(targetCol, targetRow, map)) return false;

        moveTo(targetCol, targetRow);
        facing       = dir;
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

        sniffActive    = true;
        sniffEndTime   = now + SNIFF_DURATION_NS;
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
    // Getters
    // -------------------------------------------------------------------------

    public boolean isSniffActive()    { return sniffActive; }
    public long    getSniffEndTime()  { return sniffEndTime; }
    public long    getSniffCooldownEnd() { return sniffCooldownEnd; }
    public long    getLastMoveTime()  { return lastMoveTime; }
}
