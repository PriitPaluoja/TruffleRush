package com.example.demo.item;

import com.example.demo.world.GameMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages the spawning lifecycle of {@link Item} objects on the {@link GameMap}.
 *
 * <p>Call {@link #tick()} once per game tick (assumed ~60 ticks/second).
 * Roughly every 60–120 ticks a new item is placed on a random passable,
 * unoccupied cell using weighted-random selection over {@link ItemType} values
 * whose {@code rarityWeight} is greater than zero (i.e. excluding
 * {@link ItemType#GOLDEN_TRUFFLE}).
 *
 * <p>At most {@value #MAX_UNCOLLECTED} uncollected items exist at any time.
 */
public class ItemSpawner {

    /** Maximum number of uncollected items allowed on the map simultaneously. */
    private static final int MAX_UNCOLLECTED = 30;

    /**
     * Minimum ticks between spawn attempts.
     * At 60 ticks/s this equals roughly 1 second.
     */
    private static final int MIN_SPAWN_INTERVAL = 60;

    /**
     * Maximum ticks between spawn attempts.
     * At 60 ticks/s this equals roughly 2 seconds.
     */
    private static final int MAX_SPAWN_INTERVAL = 120;

    // Pre-built weighted table (excludes GOLDEN_TRUFFLE and anything with weight 0)
    private static final List<ItemType> WEIGHTED_TABLE;
    static {
        List<ItemType> table = new ArrayList<>();
        for (ItemType t : ItemType.values()) {
            if (t.rarityWeight > 0) {
                for (int i = 0; i < t.rarityWeight; i++) {
                    table.add(t);
                }
            }
        }
        WEIGHTED_TABLE = Collections.unmodifiableList(table);
    }

    private final GameMap map;
    private final Random  random = new Random();
    private final List<Item> items = new ArrayList<>();

    /** Counts down to zero, then a spawn attempt is made. */
    private int ticksUntilSpawn;

    /**
     * Creates a new spawner bound to the given map.
     *
     * @param map the game map on which items will be placed
     */
    public ItemSpawner(GameMap map) {
        this.map = map;
        this.ticksUntilSpawn = nextSpawnInterval();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Advances the spawner by one game tick.
     *
     * <p>When the internal countdown reaches zero a spawn attempt is made:
     * a random passable, unoccupied cell is chosen and a weighted-random
     * item type is placed there (unless the uncollected-item cap is reached).
     */
    public void tick() {
        ticksUntilSpawn--;
        if (ticksUntilSpawn <= 0) {
            ticksUntilSpawn = nextSpawnInterval();
            trySpawn();
        }
    }

    /**
     * Returns the full list of all items ever spawned (collected and uncollected).
     *
     * @return unmodifiable view of the item list
     */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Returns the uncollected item at the given grid position, or {@code null}
     * if no such item exists.
     *
     * @param col column index
     * @param row row index
     * @return the item, or {@code null}
     */
    public Item getItemAt(int col, int row) {
        for (Item item : items) {
            if (!item.isCollected() && item.getCol() == col && item.getRow() == row) {
                return item;
            }
        }
        return null;
    }

    /**
     * Marks the given item as collected.
     *
     * @param item the item to collect
     */
    public void collectItem(Item item) {
        item.collect();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Attempts to spawn one item on a random free passable cell. */
    private void trySpawn() {
        if (countUncollected() >= MAX_UNCOLLECTED) {
            return;
        }
        if (WEIGHTED_TABLE.isEmpty()) {
            return;
        }

        // Pick a random passable, unoccupied cell.
        // Give up after a reasonable number of attempts to avoid an infinite loop
        // on a very crowded map.
        int cols = map.getColumns();
        int rows = map.getRows();
        int maxAttempts = cols * rows; // worst-case full scan equivalent

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int col = random.nextInt(cols);
            int row = random.nextInt(rows);

            if (map.isPassable(col, row) && getItemAt(col, row) == null) {
                ItemType type = WEIGHTED_TABLE.get(random.nextInt(WEIGHTED_TABLE.size()));
                items.add(new Item(type, col, row));
                return;
            }
        }
        // No free cell found — skip this spawn cycle
    }

    /** Counts items that have not yet been collected. */
    private int countUncollected() {
        int count = 0;
        for (Item item : items) {
            if (!item.isCollected()) {
                count++;
            }
        }
        return count;
    }

    /** Returns a random interval in [MIN_SPAWN_INTERVAL, MAX_SPAWN_INTERVAL]. */
    private int nextSpawnInterval() {
        return MIN_SPAWN_INTERVAL + random.nextInt(MAX_SPAWN_INTERVAL - MIN_SPAWN_INTERVAL + 1);
    }
}
