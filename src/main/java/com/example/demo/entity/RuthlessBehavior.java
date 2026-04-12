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
 *
 * <p>AI pigs have limited vision: they can only "see" items within
 * {@value #VISION_RADIUS} Manhattan-distance cells.
 */
public class RuthlessBehavior implements PigBehavior {

    private static final int VISION_RADIUS = 8;

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

        // Golden Truffle special rule
        if (goldenTruffleCol != -1 && goldenTruffleRow != -1) {
            if (!isHeaviest(self, allPigs)) {
                // Not heaviest: race directly to the Golden Truffle
                List<Direction> path = BFS.findPath(
                        map, self.getCol(), self.getRow(),
                        goldenTruffleCol, goldenTruffleRow, blockedCells);
                if (!path.isEmpty()) return path.get(0);
            } else {
                // IS heaviest: intercept the player's path to the Golden Truffle
                Pig player = findPlayer(allPigs);
                if (player != null) {
                    Direction intercept = computeIntercept(self, player, map, blockedCells);
                    if (intercept != Direction.NONE) return intercept;
                }
            }
        }

        // Find the best visible item (any weightDelta, excluding hazards)
        Item bestItem = findBestVisibleItem(self);

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
     * Returns the best uncollected non-hazard item within vision radius.
     */
    private Item findBestVisibleItem(AIPig self) {
        Item bestItem = null;
        int bestDelta = Integer.MIN_VALUE;

        for (Item item : itemSpawner.getItems()) {
            if (item.isCollected()) continue;
            if (item.getType().isHazard) continue;

            int dist = Math.abs(item.getCol() - self.getCol())
                     + Math.abs(item.getRow() - self.getRow());
            if (dist > VISION_RADIUS) continue;

            int delta = item.getType().weightDelta;
            if (delta > bestDelta) {
                bestDelta = delta;
                bestItem = item;
            }
        }

        return bestItem;
    }

    /**
     * Returns the player pig (named "Player") from the list, or {@code null}.
     */
    private Pig findPlayer(List<Pig> allPigs) {
        for (Pig pig : allPigs) {
            if ("Player".equals(pig.getName())) return pig;
        }
        return null;
    }

    /**
     * Computes an intercept move: find the player's BFS path to the Golden Truffle,
     * then move Ruthless toward the cell midway along that path to block it.
     * Falls back to NONE if no intercept is possible.
     */
    private Direction computeIntercept(AIPig self, Pig player, GameMap map, List<int[]> blocked) {
        // Compute the player's path to the golden truffle
        List<Direction> playerPath = BFS.findPath(
                map, player.getCol(), player.getRow(),
                goldenTruffleCol, goldenTruffleRow, Collections.emptyList());

        if (playerPath.isEmpty()) {
            // Player has no path; just race to the truffle ourselves
            List<Direction> path = BFS.findPath(
                    map, self.getCol(), self.getRow(),
                    goldenTruffleCol, goldenTruffleRow, blocked);
            return path.isEmpty() ? Direction.NONE : path.get(0);
        }

        // Pick the cell halfway along the player's path as our intercept target
        int interceptStep = Math.min(playerPath.size() - 1, playerPath.size() / 2);
        int ic = player.getCol();
        int ir = player.getRow();
        for (int i = 0; i <= interceptStep; i++) {
            Direction d = playerPath.get(i);
            ic += d.dc;
            ir += d.dr;
        }

        List<Direction> path = BFS.findPath(map, self.getCol(), self.getRow(), ic, ir, blocked);
        return path.isEmpty() ? Direction.NONE : path.get(0);
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
        Direction[] dirs = CARDINALS.clone();
        // Fisher-Yates shuffle
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Direction tmp = dirs[i]; dirs[i] = dirs[j]; dirs[j] = tmp;
        }

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
