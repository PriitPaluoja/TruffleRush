package com.example.demo.entity;

import com.example.demo.item.Item;
import com.example.demo.item.ItemSpawner;
import com.example.demo.util.BFS;
import com.example.demo.world.GameMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tier-3 AI behaviour (dark grey pig).
 *
 * <p>Like {@link CunningBehavior} but more aggressive:
 * <ul>
 *   <li>Also targets items with negative {@code weightDelta} (not just positive ones)
 *       to maximise score gain at the expense of other pigs.</li>
 *   <li>Routes around other pigs (uses them as soft blocked cells).</li>
 *   <li>Golden Truffle special rule: if a Golden Truffle target has been set and
 *       this pig is not the heaviest pig, it races to the Golden Truffle.</li>
 * </ul>
 */
public class RuthlessBehavior implements PigBehavior {

    private final ItemSpawner itemSpawner;
    private final Random random = new Random();

    /** Column of the Golden Truffle target, or -1 if not set. */
    private int goldenTruffleCol = -1;

    /** Row of the Golden Truffle target, or -1 if not set. */
    private int goldenTruffleRow = -1;

    private static final Direction[] CARDINALS = {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
    };

    /**
     * Creates a new RuthlessBehavior.
     *
     * @param itemSpawner the spawner that tracks all current items on the map
     */
    public RuthlessBehavior(ItemSpawner itemSpawner) {
        this.itemSpawner = itemSpawner;
    }

    // -------------------------------------------------------------------------
    // Golden Truffle targeting
    // -------------------------------------------------------------------------

    /**
     * Sets the grid coordinates of the Golden Truffle so the pig can race to it.
     *
     * @param col column of the Golden Truffle
     * @param row row of the Golden Truffle
     */
    public void setGoldenTruffleTarget(int col, int row) {
        this.goldenTruffleCol = col;
        this.goldenTruffleRow = row;
    }

    // -------------------------------------------------------------------------
    // PigBehavior
    // -------------------------------------------------------------------------

    @Override
    public Direction nextMove(AIPig self, GameMap map, List<Pig> allPigs) {
        // Build list of other pigs' positions to route around
        List<int[]> blockedCells = new ArrayList<>();
        for (Pig pig : allPigs) {
            if (pig != self) {
                blockedCells.add(new int[]{pig.getCol(), pig.getRow()});
            }
        }

        // Golden Truffle special rule: if target is set and we're not the heaviest, race to it
        if (goldenTruffleCol != -1 && goldenTruffleRow != -1) {
            if (!isHeaviest(self, allPigs)) {
                List<Direction> path = BFS.findPath(
                        map,
                        self.getCol(), self.getRow(),
                        goldenTruffleCol, goldenTruffleRow,
                        blockedCells
                );
                if (!path.isEmpty()) {
                    return path.get(0);
                }
            }
        }

        // Find the best item (any weightDelta, excluding hazards)
        Item bestItem = findBestItem();

        if (bestItem != null) {
            List<Direction> path = BFS.findPath(
                    map,
                    self.getCol(), self.getRow(),
                    bestItem.getCol(), bestItem.getRow(),
                    blockedCells
            );
            if (!path.isEmpty()) {
                return path.get(0);
            }

            // Try again without blocked cells in case routing is impossible otherwise
            path = BFS.findPath(
                    map,
                    self.getCol(), self.getRow(),
                    bestItem.getCol(), bestItem.getRow(),
                    Collections.emptyList()
            );
            if (!path.isEmpty()) {
                return path.get(0);
            }
        }

        // Fall back to a random valid direction
        return randomValidDirection(self, map, blockedCells);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the best uncollected non-hazard item on the map.
     * Considers all weightDelta values (positive and negative are both acceptable
     * since the ruthless pig wants maximum score gain and may feed opponents hazards).
     * Sorts by highest absolute weightDelta among positive items first, then any item.
     */
    private Item findBestItem() {
        Item bestItem = null;
        int bestDelta = Integer.MIN_VALUE;

        for (Item item : itemSpawner.getItems()) {
            if (item.isCollected()) continue;
            if (item.getType().isHazard) continue;

            int delta = item.getType().weightDelta;
            if (delta > bestDelta) {
                bestDelta = delta;
                bestItem = item;
            }
        }

        return bestItem;
    }

    /**
     * Returns true if {@code self} has the highest weight among all pigs.
     */
    private boolean isHeaviest(AIPig self, List<Pig> allPigs) {
        double myWeight = self.getWeight();
        for (Pig pig : allPigs) {
            if (pig != self && pig.getWeight() > myWeight) {
                return false;
            }
        }
        return true;
    }

    private Direction randomValidDirection(AIPig self, GameMap map, List<int[]> blockedCells) {
        List<Direction> dirs = new ArrayList<>(List.of(CARDINALS));
        Collections.shuffle(dirs, random);

        for (Direction dir : dirs) {
            int nc = self.getCol() + dir.dc;
            int nr = self.getRow() + dir.dr;
            if (self.canMove(nc, nr, map)) {
                return dir;
            }
        }
        return Direction.NONE;
    }
}
