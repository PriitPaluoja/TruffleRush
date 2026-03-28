package com.example.demo.entity;

import com.example.demo.world.GameMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tier-1 AI behaviour (pink pig).
 *
 * <p>Picks a random passable direction each tick.  Directions are shuffled so
 * that the pig wanders unpredictably.  Falls back to {@link Direction#NONE} if
 * no valid direction exists (e.g. the pig is completely surrounded).
 */
public class RandomBehavior implements PigBehavior {

    private final Random random = new Random();

    /** All cardinal directions that the pig may try. */
    private static final Direction[] CARDINALS = {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
    };

    @Override
    public Direction nextMove(AIPig self, GameMap map, List<Pig> allPigs) {
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
