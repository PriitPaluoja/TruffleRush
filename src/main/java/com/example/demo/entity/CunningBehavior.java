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
 * Tier-2 AI behaviour (brown pig).
 *
 * <p>Uses BFS to find the highest-value visible item (highest positive
 * {@code weightDelta}) and heads towards it.  Hazard items are ignored.
 * Falls back to random movement when no reachable item exists.
 *
 * <p>AI pigs have limited vision: they can only "see" items within
 * {@value #VISION_RADIUS} Manhattan-distance cells.
 */
public class CunningBehavior implements PigBehavior {

    private static final int VISION_RADIUS = 8;

    private final ItemSpawner itemSpawner;
    private final Random random = new Random();

    private static final Direction[] CARDINALS = {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
    };

    /**
     * Creates a new CunningBehavior.
     *
     * @param itemSpawner the spawner that tracks all current items on the map
     */
    public CunningBehavior(ItemSpawner itemSpawner) {
        this.itemSpawner = itemSpawner;
    }

    @Override
    public Direction nextMove(AIPig self, GameMap map, List<Pig> allPigs) {
        List<Item> candidates = getVisiblePositiveItems(self);

        if (!candidates.isEmpty()) {
            // Score each item: prefer high value, break ties by proximity
            Item bestItem = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (Item item : candidates) {
                int dist = Math.abs(item.getCol() - self.getCol())
                         + Math.abs(item.getRow() - self.getRow());
                // Value per distance — higher is better
                double score = item.getType().weightDelta / Math.max(1.0, dist);
                if (score > bestScore) {
                    bestScore = score;
                    bestItem = item;
                }
            }

            if (bestItem != null) {
                List<Direction> path = BFS.findPath(
                        map,
                        self.getCol(), self.getRow(),
                        bestItem.getCol(), bestItem.getRow(),
                        Collections.emptyList()
                );
                if (!path.isEmpty()) {
                    return path.get(0);
                }
            }
        }

        // Fall back to a random valid direction
        return randomValidDirection(self, map);
    }

    /**
     * Returns uncollected positive non-hazard items within vision radius.
     */
    private List<Item> getVisiblePositiveItems(AIPig self) {
        List<Item> result = new ArrayList<>();
        for (Item item : itemSpawner.getItems()) {
            if (!item.isCollected()
                    && !item.getType().isHazard
                    && item.getType().weightDelta > 0) {
                int dist = Math.abs(item.getCol() - self.getCol())
                         + Math.abs(item.getRow() - self.getRow());
                if (dist <= VISION_RADIUS) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private Direction randomValidDirection(AIPig self, GameMap map) {
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
