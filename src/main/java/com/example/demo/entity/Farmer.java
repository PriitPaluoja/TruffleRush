package com.example.demo.entity;

import com.example.demo.util.BFS;
import com.example.demo.world.GameMap;

import java.util.List;
import java.util.Random;

/**
 * A farmer that chases the player pig. Player must reach an escape hole to survive.
 * If caught = game over (unless shield or super pig).
 */
public class Farmer {

    private int col;
    private int row;
    private int escapeCol;
    private int escapeRow;
    private int tickCounter;
    private int lifetime;
    private boolean active;
    private boolean caughtPlayer;
    private boolean playerEscaped;
    private boolean usedPlayerShield;

    private static final int MOVE_INTERVAL = 18;
    private static final int MAX_LIFETIME = 480;
    private static final int MAX_SPAWN_ATTEMPTS = 50;

    private Direction currentDir = Direction.NONE;
    private int recomputeCounter;

    public Farmer(GameMap map, PlayerPig player) {
        Random rng = new Random();
        // Spawn at random edge
        int edge = rng.nextInt(4);
        switch (edge) {
            case 0 -> { col = 0; row = rng.nextInt(map.getRows()); }
            case 1 -> { col = map.getColumns() - 1; row = rng.nextInt(map.getRows()); }
            case 2 -> { col = rng.nextInt(map.getColumns()); row = 0; }
            default -> { col = rng.nextInt(map.getColumns()); row = map.getRows() - 1; }
        }
        if (!map.isPassable(col, row)) {
            col = map.getColumns() - 2; row = map.getRows() - 2;
        }

        // Spawn escape hole far from farmer
        boolean placed = false;
        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            int ec = rng.nextInt(map.getColumns());
            int er = rng.nextInt(map.getRows());
            if (map.isPassable(ec, er)) {
                int dist = Math.abs(ec - col) + Math.abs(er - row);
                if (dist >= 10) {
                    escapeCol = ec;
                    escapeRow = er;
                    placed = true;
                    break;
                }
            }
        }
        if (!placed) {
            // Fallback: opposite corner; if that's not passable, scan outward
            // for the first passable cell so the round stays winnable.
            int targetCol = (col < map.getColumns() / 2) ? map.getColumns() - 2 : 1;
            int targetRow = (row < map.getRows() / 2) ? map.getRows() - 2 : 1;
            int[] found = nearestPassable(map, targetCol, targetRow);
            escapeCol = found[0];
            escapeRow = found[1];
        }

        this.active = true;
        this.lifetime = MAX_LIFETIME;
    }

    public void tick(GameMap map, PlayerPig player) {
        if (!active) return;
        lifetime--;
        if (lifetime <= 0) { active = false; return; }

        tickCounter++;
        recomputeCounter++;

        // Check if player reached escape hole
        if (player.getCol() == escapeCol && player.getRow() == escapeRow) {
            playerEscaped = true;
            active = false;
            return;
        }

        // Recompute direction every 6 ticks
        if (recomputeCounter >= 6) {
            recomputeCounter = 0;
            List<Direction> path = BFS.findPath(map, col, row,
                player.getCol(), player.getRow(), List.of());
            currentDir = path.isEmpty() ? Direction.NONE : path.get(0);
        }

        // Move
        if (tickCounter % MOVE_INTERVAL == 0 && currentDir != Direction.NONE) {
            int nc = col + currentDir.dc;
            int nr = row + currentDir.dr;
            if (map.isInBounds(nc, nr) && map.isPassable(nc, nr)) {
                col = nc;
                row = nr;
            }
        }

        // Check collision with player
        if (col == player.getCol() && row == player.getRow()) {
            if (player.isSuperPig()) {
                active = false; // super pig ignores farmer
                return;
            } else if (player.consumeShield()) {
                usedPlayerShield = true;
                active = false; // shield blocks farmer
                return;
            } else {
                caughtPlayer = true;
                active = false;
            }
        }
    }

    /**
     * Returns the nearest passable cell starting from (startCol, startRow)
     * via an expanding-ring scan. Falls back to the start coordinate if the
     * scan cannot find anything (which only happens on a degenerate map).
     */
    private static int[] nearestPassable(GameMap map, int startCol, int startRow) {
        if (map.isInBounds(startCol, startRow) && map.isPassable(startCol, startRow)) {
            return new int[]{startCol, startRow};
        }
        int maxRadius = Math.max(map.getColumns(), map.getRows());
        for (int r = 1; r < maxRadius; r++) {
            for (int dc = -r; dc <= r; dc++) {
                for (int dr = -r; dr <= r; dr++) {
                    if (Math.abs(dc) != r && Math.abs(dr) != r) continue; // ring border only
                    int c = startCol + dc;
                    int rr = startRow + dr;
                    if (map.isInBounds(c, rr) && map.isPassable(c, rr)) {
                        return new int[]{c, rr};
                    }
                }
            }
        }
        return new int[]{startCol, startRow};
    }

    public int getCol() { return col; }
    public int getRow() { return row; }
    public int getEscapeCol() { return escapeCol; }
    public int getEscapeRow() { return escapeRow; }
    public boolean isActive() { return active; }
    public boolean hasCaughtPlayer() { return caughtPlayer; }
    public boolean hasPlayerEscaped() { return playerEscaped; }
    public boolean wasShieldedByPlayer() { return usedPlayerShield; }
}
