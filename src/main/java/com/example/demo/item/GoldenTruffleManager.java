package com.example.demo.item;

import com.example.demo.entity.Pig;
import com.example.demo.world.GameMap;

import java.util.List;
import java.util.Random;

/**
 * Manages the Golden Truffle spawning event for a single round.
 *
 * <p>The Golden Truffle spawns exactly once per round, at a random tick within
 * the final 30% of the round duration.  If uncollected, it despawns after
 * 480 ticks (~8 seconds at 60 ticks/s).
 *
 * <p>Usage:
 * <pre>
 *   GoldenTruffleManager gtm = new GoldenTruffleManager(roundTicks, map, allPigs);
 *
 *   // In the game loop:
 *   Item spawned = gtm.tick(currentTick, map, allPigs);
 *   if (spawned != null) {
 *       eventBus.publish(GameEvent.GOLDEN_TRUFFLE_SPAWNED, spawned);
 *   }
 * </pre>
 */
public class GoldenTruffleManager {

    /** Ticks the golden truffle survives uncollected before despawning (~8 s). */
    private static final int DESPAWN_TICKS = 480;

    /** Number of ticks over which the pulse announcement expands. */
    private static final int PULSE_DURATION = 60;

    /** Maximum radius the pulse circle reaches before deactivating. */
    private static final double PULSE_MAX_RADIUS = 200.0;

    /** Maximum attempts to find a valid spawn cell. */
    private static final int MAX_SPAWN_ATTEMPTS = 50;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** The live golden truffle item, or {@code null} if not yet spawned / already collected. */
    private Item goldenTruffle;

    /** Whether the truffle has already spawned this round (can only happen once). */
    private boolean spawned;

    /** The tick number at which the truffle should spawn. */
    private final int spawnTick;

    /** Countdown (in ticks) until the truffle despawns; set to DESPAWN_TICKS on spawn. */
    private int despawnCountdown;

    /** Whether the pulse announcement animation is currently active. */
    private boolean pulseActive;

    /** Current pulse radius in pixels. */
    private double pulseRadius;

    /** How many ticks the pulse has been active (used to compute radius growth). */
    private int pulseTick;

    private final Random random = new Random();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new manager for one round.
     *
     * @param roundTicks total number of ticks in the round
     * @param map        the current game map (used to validate spawn cells)
     * @param allPigs    all pigs in the game (used to avoid spawning near heaviest pig)
     */
    public GoldenTruffleManager(int roundTicks, GameMap map, List<Pig> allPigs) {
        // Spawn tick is a random moment in the final 30% of the round
        double earlyStart = roundTicks * 0.7;
        double window     = roundTicks * 0.3;
        this.spawnTick = (int) (earlyStart + random.nextDouble() * window);

        this.spawned          = false;
        this.goldenTruffle    = null;
        this.despawnCountdown = 0;
        this.pulseActive      = false;
        this.pulseRadius      = 0.0;
        this.pulseTick        = 0;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Advances the manager by one game tick.
     *
     * @param currentTick the current tick counter for the round (0-based)
     * @param map         the game map
     * @param allPigs     all pigs in the game
     * @return the newly spawned {@link Item} on the tick it first appears,
     *         {@code null} on every other tick
     */
    public Item tick(int currentTick, GameMap map, List<Pig> allPigs) {
        // Pulse animation update (independent of whether truffle is still live)
        if (pulseActive) {
            pulseTick++;
            pulseRadius = PULSE_MAX_RADIUS * ((double) pulseTick / PULSE_DURATION);
            if (pulseTick >= PULSE_DURATION) {
                pulseActive  = false;
                pulseRadius  = 0.0;
            }
        }

        // If already spawned but not yet collected, handle despawn countdown
        if (spawned && goldenTruffle != null && !goldenTruffle.isCollected()) {
            despawnCountdown--;
            if (despawnCountdown <= 0) {
                // Despawn: mark as collected so renderers stop drawing it
                goldenTruffle.collect();
                goldenTruffle = null;
            }
            return null;
        }

        // If already collected externally, clear our reference
        if (spawned && goldenTruffle != null && goldenTruffle.isCollected()) {
            goldenTruffle = null;
            return null;
        }

        // Attempt to spawn on the designated tick (only once per round)
        if (!spawned && currentTick >= spawnTick) {
            spawned = true;
            Item newItem = trySpawn(map, allPigs);
            if (newItem != null) {
                goldenTruffle    = newItem;
                despawnCountdown = DESPAWN_TICKS;
                // Start pulse announcement
                pulseActive = true;
                pulseRadius = 0.0;
                pulseTick   = 0;
                return goldenTruffle;
            }
        }

        return null;
    }

    /**
     * Returns the currently active golden truffle item, or {@code null} if none is present.
     *
     * @return the live truffle item, or {@code null}
     */
    public Item getGoldenTruffle() {
        return goldenTruffle;
    }

    /**
     * Returns {@code true} while the pulse announcement animation is running.
     *
     * @return {@code true} if the pulse is active
     */
    public boolean isPulseActive() {
        return pulseActive;
    }

    /**
     * Returns the current pulse radius in pixels (grows from 0 to ~200 over ~60 ticks).
     *
     * @return pulse radius in pixels
     */
    public double getPulseRadius() {
        return pulseRadius;
    }

    /**
     * Notifies the manager that the golden truffle has been collected.
     * Clears the internal item reference.
     */
    public void onCollected() {
        if (goldenTruffle != null) {
            goldenTruffle.collect();
            goldenTruffle = null;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Attempts to place the golden truffle on a passable cell that is not
     * 4-directionally adjacent to the current heaviest pig.
     *
     * @param map     the game map
     * @param allPigs all pigs (used to find the heaviest)
     * @return a new {@link Item} at the chosen cell, or {@code null} if no valid
     *         cell could be found within {@value #MAX_SPAWN_ATTEMPTS} attempts
     */
    private Item trySpawn(GameMap map, List<Pig> allPigs) {
        Pig heaviest = findHeaviest(allPigs);

        int cols = map.getColumns();
        int rows = map.getRows();

        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            int col = random.nextInt(cols);
            int row = random.nextInt(rows);

            if (!map.isPassable(col, row)) {
                continue;
            }

            if (heaviest != null && isAdjacentTo(col, row, heaviest.getCol(), heaviest.getRow())) {
                continue;
            }

            return new Item(ItemType.GOLDEN_TRUFFLE, col, row);
        }

        return null; // no valid cell found
    }

    /**
     * Returns the pig with the highest weight, or {@code null} if the list is empty.
     */
    private Pig findHeaviest(List<Pig> allPigs) {
        Pig heaviest = null;
        for (Pig pig : allPigs) {
            if (heaviest == null || pig.getWeight() > heaviest.getWeight()) {
                heaviest = pig;
            }
        }
        return heaviest;
    }

    /**
     * Returns {@code true} if (col, row) is 4-directionally adjacent to (targetCol, targetRow).
     */
    private boolean isAdjacentTo(int col, int row, int targetCol, int targetRow) {
        int dc = Math.abs(col - targetCol);
        int dr = Math.abs(row - targetRow);
        // Adjacent means exactly one step in a cardinal direction (Manhattan distance == 1)
        return (dc + dr) <= 1;
    }
}
