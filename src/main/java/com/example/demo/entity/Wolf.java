package com.example.demo.entity;

import com.example.demo.util.BFS;
import com.example.demo.world.GameMap;

import java.util.List;
import java.util.Random;

/**
 * A wolf that spawns at a map edge, chases the nearest pig using BFS.
 * If it catches a pig: AI pigs get stunned and lose weight; player = game over.
 * Flees from the player when player is in super pig mode.
 *
 * <p>Wolf speed scales with level: slower on early levels, full speed from level 4+.
 */
public class Wolf {

    private int col;
    private int row;
    private int tickCounter;
    private int lifetime;
    private boolean active;
    private boolean caughtPlayer;
    private boolean usedPlayerShield;
    private Pig caughtPig;

    private static final int MAX_LIFETIME = 600;

    /** Move interval scales by level: Level 2 = 24 ticks (slow), Level 3 = 18, Level 4+ = 12 (full). */
    private final int moveInterval;

    private Direction currentDir = Direction.NONE;
    private int recomputeCounter;

    public Wolf(GameMap map, int level) {
        // Scale wolf speed: slower on early levels
        this.moveInterval = Math.max(12, 30 - level * 6);

        Random rng = new Random();
        // Spawn at random edge
        int edge = rng.nextInt(4);
        switch (edge) {
            case 0 -> { col = 0; row = rng.nextInt(map.getRows()); }
            case 1 -> { col = map.getColumns() - 1; row = rng.nextInt(map.getRows()); }
            case 2 -> { col = rng.nextInt(map.getColumns()); row = 0; }
            default -> { col = rng.nextInt(map.getColumns()); row = map.getRows() - 1; }
        }
        // Ensure passable
        if (!map.isPassable(col, row)) {
            col = 1; row = 1; // fallback
        }
        this.active = true;
        this.lifetime = MAX_LIFETIME;
        this.caughtPlayer = false;
    }

    public void tick(GameMap map, List<Pig> allPigs, PlayerPig player) {
        if (!active) return;
        lifetime--;
        if (lifetime <= 0) { active = false; return; }

        tickCounter++;
        recomputeCounter++;

        // Recompute direction every 6 ticks
        if (recomputeCounter >= 6) {
            recomputeCounter = 0;
            if (player.isSuperPig()) {
                // Flee from player
                int dx = col - player.getCol();
                int dy = row - player.getRow();
                if (Math.abs(dx) >= Math.abs(dy)) {
                    currentDir = dx > 0 ? Direction.RIGHT : Direction.LEFT;
                } else {
                    currentDir = dy > 0 ? Direction.DOWN : Direction.UP;
                }
            } else {
                // Chase nearest pig
                Pig target = findNearest(allPigs);
                if (target != null) {
                    List<Direction> path = BFS.findPath(map, col, row,
                        target.getCol(), target.getRow(), List.of());
                    currentDir = path.isEmpty() ? Direction.NONE : path.get(0);
                }
            }
        }

        // Move
        if (tickCounter % moveInterval == 0 && currentDir != Direction.NONE) {
            int nc = col + currentDir.dc;
            int nr = row + currentDir.dr;
            if (map.isInBounds(nc, nr) && map.isPassable(nc, nr)) {
                col = nc;
                row = nr;
            }
        }

        // Check collision with pigs
        for (Pig pig : allPigs) {
            if (pig.getCol() == col && pig.getRow() == row) {
                if (pig instanceof PlayerPig pp) {
                    if (pp.isSuperPig()) {
                        // Player stuns wolf
                        active = false;
                        caughtPig = null;
                        return;
                    } else if (pp.consumeShield()) {
                        // Shield blocks wolf
                        usedPlayerShield = true;
                        active = false;
                        return;
                    } else {
                        caughtPlayer = true;
                        active = false;
                        return;
                    }
                } else {
                    // Stun AI pig and reduce weight
                    pig.stun(120);
                    pig.addWeight(-15);
                    caughtPig = pig;
                    active = false;
                    return;
                }
            }
        }
    }

    private Pig findNearest(List<Pig> pigs) {
        Pig nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (Pig p : pigs) {
            int dist = Math.abs(p.getCol() - col) + Math.abs(p.getRow() - row);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    public int getCol() { return col; }
    public int getRow() { return row; }
    public boolean isActive() { return active; }
    public boolean hasCaughtPlayer() { return caughtPlayer; }
    public boolean wasShieldedByPlayer() { return usedPlayerShield; }
    public Pig getCaughtPig() { return caughtPig; }
    public boolean wasStunnedByPlayer() { return !active && !caughtPlayer && caughtPig == null && lifetime > 0; }
}
