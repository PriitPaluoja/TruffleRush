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
 */
public class CunningBehavior implements PigBehavior {

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
        List<Item> candidates = getPositiveNonHazardItems();

        if (!candidates.isEmpty()) {
            // Find the item with the highest weightDelta
            Item bestItem = null;
            int bestDelta = Integer.MIN_VALUE;

            for (Item item : candidates) {
                if (item.getType().weightDelta > bestDelta) {
                    bestDelta = item.getType().weightDelta;
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
     * Returns uncollected items that are not hazards and have a positive weightDelta.
     */
    private List<Item> getPositiveNonHazardItems() {
        List<Item> result = new ArrayList<>();
        for (Item item : itemSpawner.getItems()) {
            if (!item.isCollected()
                    && !item.getType().isHazard
                    && item.getType().weightDelta > 0) {
                result.add(item);
            }
        }
        return result;
    }

    private Direction randomValidDirection(AIPig self, GameMap map) {
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
