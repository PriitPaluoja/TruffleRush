package com.example.demo.entity;

import com.example.demo.item.Item;
import com.example.demo.item.ItemSpawner;
import com.example.demo.util.BFS;
import com.example.demo.world.GameMap;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Hoggart the Greedy: always heads for the closest visible item, regardless of value.
 * Doesn't care about hazards — will gladly grab a celery if it's nearest.
 */
public class HoggartBehavior implements PigBehavior {

    private static final int VISION_RADIUS = 8;
    private static final Direction[] CARDINALS = {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
    };

    private final ItemSpawner itemSpawner;
    private final Random random = new Random();

    public HoggartBehavior(ItemSpawner itemSpawner) {
        this.itemSpawner = itemSpawner;
    }

    @Override
    public Direction nextMove(AIPig self, GameMap map, List<Pig> allPigs) {
        Item nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (Item item : itemSpawner.getItems()) {
            if (item.isCollected()) continue;
            int dist = Math.abs(item.getCol() - self.getCol())
                     + Math.abs(item.getRow() - self.getRow());
            if (dist > VISION_RADIUS) continue;
            if (dist < bestDist) {
                bestDist = dist;
                nearest = item;
            }
        }

        if (nearest != null) {
            List<Direction> path = BFS.findPath(map, self.getCol(), self.getRow(),
                nearest.getCol(), nearest.getRow(), Collections.emptyList());
            if (!path.isEmpty()) return path.get(0);
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
