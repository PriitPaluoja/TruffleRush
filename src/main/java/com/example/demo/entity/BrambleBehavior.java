package com.example.demo.entity;

import com.example.demo.util.BFS;
import com.example.demo.world.GameMap;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Bramble the Bully: chases the player and tries to ram them (push them onto bad cells).
 * If the player is far away, wanders toward map centre rather than collecting items.
 */
public class BrambleBehavior implements PigBehavior {

    private static final Direction[] CARDINALS = {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
    };

    private final Random random = new Random();

    @Override
    public Direction nextMove(AIPig self, GameMap map, List<Pig> allPigs) {
        Pig player = findPlayer(allPigs);
        if (player != null) {
            int dist = Math.abs(player.getCol() - self.getCol())
                     + Math.abs(player.getRow() - self.getRow());
            // Real bullies pick on smaller pigs: only chase when Bramble is heavier
            // than the player AND within range. Don't push the player into starvation.
            if (player.getWeight() > 25.0
                    && self.getWeight() >= player.getWeight()
                    && dist <= 8) {
                List<Direction> path = BFS.findPath(map, self.getCol(), self.getRow(),
                    player.getCol(), player.getRow(), Collections.emptyList());
                if (!path.isEmpty()) return path.get(0);
            }
        }

        // Default: drift toward map centre
        int targetC = map.getColumns() / 2;
        int targetR = map.getRows() / 2;
        List<Direction> path = BFS.findPath(map, self.getCol(), self.getRow(),
            targetC, targetR, Collections.emptyList());
        if (!path.isEmpty()) return path.get(0);
        return randomValidDirection(self, map);
    }

    private Pig findPlayer(List<Pig> pigs) {
        for (Pig p : pigs) {
            if ("Player".equals(p.getName())) return p;
        }
        return null;
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
