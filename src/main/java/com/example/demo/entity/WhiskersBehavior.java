package com.example.demo.entity;

import com.example.demo.item.Item;
import com.example.demo.item.ItemSpawner;
import com.example.demo.util.BFS;
import com.example.demo.world.GameMap;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Whiskers the Coward: hunts items like the Cunning AI but flees aggressively
 * when a wolf is within {@value #FLEE_RADIUS} cells.
 */
public class WhiskersBehavior implements PigBehavior {

    private static final int VISION_RADIUS = 8;
    private static final int FLEE_RADIUS   = 6;
    private static final Direction[] CARDINALS = {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
    };

    private final ItemSpawner itemSpawner;
    /** Supplier that returns the active wolf, or null. Allows late binding. */
    private final Supplier<Wolf> wolfSupplier;
    private final Random random = new Random();

    public WhiskersBehavior(ItemSpawner itemSpawner, Supplier<Wolf> wolfSupplier) {
        this.itemSpawner = itemSpawner;
        this.wolfSupplier = wolfSupplier;
    }

    @Override
    public Direction nextMove(AIPig self, GameMap map, List<Pig> allPigs) {
        Wolf wolf = wolfSupplier.get();
        if (wolf != null && wolf.isActive()) {
            int dist = Math.abs(wolf.getCol() - self.getCol())
                     + Math.abs(wolf.getRow() - self.getRow());
            if (dist <= FLEE_RADIUS) {
                Direction flee = directionAwayFrom(self, wolf.getCol(), wolf.getRow(), map);
                if (flee != Direction.NONE) return flee;
            }
        }

        // No wolf nearby: pick the highest-value visible non-hazard item.
        Item bestItem = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Item item : itemSpawner.getItems()) {
            if (item.isCollected() || item.getType().isHazard) continue;
            if (item.getType().weightDelta <= 0) continue;
            int dist = Math.abs(item.getCol() - self.getCol())
                     + Math.abs(item.getRow() - self.getRow());
            if (dist > VISION_RADIUS) continue;
            double score = item.getType().weightDelta / Math.max(1.0, dist);
            if (score > bestScore) {
                bestScore = score;
                bestItem = item;
            }
        }

        if (bestItem != null) {
            List<Direction> path = BFS.findPath(map, self.getCol(), self.getRow(),
                bestItem.getCol(), bestItem.getRow(), Collections.emptyList());
            if (!path.isEmpty()) return path.get(0);
        }
        return randomValidDirection(self, map);
    }

    private Direction directionAwayFrom(AIPig self, int fromCol, int fromRow, GameMap map) {
        int dc = Integer.signum(self.getCol() - fromCol);
        int dr = Integer.signum(self.getRow() - fromRow);
        Direction primary;
        if (Math.abs(self.getCol() - fromCol) >= Math.abs(self.getRow() - fromRow)) {
            primary = dc > 0 ? Direction.RIGHT : (dc < 0 ? Direction.LEFT : Direction.NONE);
        } else {
            primary = dr > 0 ? Direction.DOWN : (dr < 0 ? Direction.UP : Direction.NONE);
        }
        if (primary != Direction.NONE) {
            int nc = self.getCol() + primary.dc;
            int nr = self.getRow() + primary.dr;
            if (self.canMove(nc, nr, map)) return primary;
        }
        return randomValidDirection(self, map);
    }

    private Direction randomValidDirection(AIPig self, GameMap map) {
        Direction[] dirs = CARDINALS.clone();
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Direction tmp = dirs[i]; dirs[i] = dirs[j]; dirs[j] = tmp;
        }
        for (Direction dir : dirs) {
            int nc = self.getCol() + dir.dc;
            int nr = self.getRow() + dir.dr;
            if (self.canMove(nc, nr, map)) return dir;
        }
        return Direction.NONE;
    }
}
