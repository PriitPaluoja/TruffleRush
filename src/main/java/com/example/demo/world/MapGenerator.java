package com.example.demo.world;

import com.example.demo.util.FloodFill;

import java.util.Random;

/**
 * Procedurally generates obstacle layouts for a {@link GameMap}.
 *
 * <p>Obstacle counts (approximate):
 * <ul>
 *   <li>Rocks     – 15-20, scattered randomly</li>
 *   <li>Bushes    – 8-12, placed in small clusters of 2-3 adjacent cells</li>
 *   <li>Mud Pits  – 5-8, scattered with a preference for mid-map areas</li>
 *   <li>Fences    – 10-15 segments forming short corridors of 2-4 connected cells</li>
 * </ul>
 *
 * <p>After placement a {@link FloodFill} connectivity check is run.  If the map
 * is not fully connected all obstacles are cleared and the attempt is retried
 * using {@code seed + attempt} until connectivity is achieved.
 *
 * <p>The four 2×2 corner regions are always kept obstacle-free so that pig
 * spawn areas remain accessible.
 */
public class MapGenerator {

    private final long seed;

    /**
     * Creates a generator that will use the given seed for reproducible maps.
     *
     * @param seed the initial random seed
     */
    public MapGenerator(long seed) {
        this.seed = seed;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Fills {@code map} with obstacles.  The method guarantees that the
     * resulting map is fully connected (all passable cells reachable from
     * one another).
     *
     * @param map the map to populate; any existing obstacles are overwritten
     */
    public void generate(GameMap map) {
        long attempt = 0;
        while (true) {
            clearMap(map);
            Random rng = new Random(seed + attempt);
            placeObstacles(map, rng);

            if (FloodFill.isFullyConnected(map)) {
                break;
            }
            attempt++;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Removes every obstacle from the map. */
    private void clearMap(GameMap map) {
        for (int c = 0; c < map.getColumns(); c++) {
            for (int r = 0; r < map.getRows(); r++) {
                map.getCell(c, r).setObstacle(null);
            }
        }
    }

    /** Returns {@code true} when (col, row) falls inside one of the 4 corner spawn zones. */
    private boolean isCornerRegion(int col, int row) {
        int maxC = GameMap.COLS - 1;
        int maxR = GameMap.ROWS - 1;

        boolean topLeft     = col <= 1 && row <= 1;
        boolean topRight    = col >= maxC - 1 && row <= 1;
        boolean bottomLeft  = col <= 1 && row >= maxR - 1;
        boolean bottomRight = col >= maxC - 1 && row >= maxR - 1;

        return topLeft || topRight || bottomLeft || bottomRight;
    }

    /** Tries to place {@code obstacle} at (col, row); silently skips if the cell
     *  is out of bounds, is a corner region, or already has an obstacle. */
    private void tryPlace(GameMap map, int col, int row, Obstacle obstacle) {
        if (!map.isInBounds(col, row)) return;
        if (isCornerRegion(col, row))  return;
        Cell cell = map.getCell(col, row);
        if (cell.getObstacle() == null) {
            cell.setObstacle(obstacle);
        }
    }

    /** Places all obstacle groups onto the map using the given RNG. */
    private void placeObstacles(GameMap map, Random rng) {
        placeRocks(map, rng);
        placeBushes(map, rng);
        placeMudPits(map, rng);
        placeFences(map, rng);
    }

    // --- Rocks ----------------------------------------------------------------

    private void placeRocks(GameMap map, Random rng) {
        int count = 15 + rng.nextInt(6); // 15-20
        for (int i = 0; i < count; i++) {
            int col = rng.nextInt(GameMap.COLS);
            int row = rng.nextInt(GameMap.ROWS);
            tryPlace(map, col, row, Obstacle.ROCK);
        }
    }

    // --- Bushes ---------------------------------------------------------------

    /** Places bushes in small clusters of 2-3 adjacent cells. */
    private void placeBushes(GameMap map, Random rng) {
        int clusters = 8 + rng.nextInt(5); // 8-12 bushes across several clusters
        int placed = 0;
        int attempts = 0;

        while (placed < clusters && attempts < clusters * 10) {
            attempts++;
            int col = rng.nextInt(GameMap.COLS);
            int row = rng.nextInt(GameMap.ROWS);

            if (isCornerRegion(col, row) || !map.isInBounds(col, row)) continue;

            // Cluster size: 2 or 3
            int clusterSize = 2 + rng.nextInt(2);
            Cell first = map.getCell(col, row);
            if (first.getObstacle() != null) continue;
            first.setObstacle(Obstacle.BUSH);
            placed++;

            // Extend cluster orthogonally
            int[] dc = {0, 0, -1, 1};
            int[] dr = {-1, 1, 0, 0};
            int curC = col;
            int curR = row;

            for (int step = 1; step < clusterSize && placed < clusters; step++) {
                int dir = rng.nextInt(4);
                int nc = curC + dc[dir];
                int nr = curR + dr[dir];
                if (map.isInBounds(nc, nr) && !isCornerRegion(nc, nr)
                        && map.getCell(nc, nr).getObstacle() == null) {
                    map.getCell(nc, nr).setObstacle(Obstacle.BUSH);
                    placed++;
                    curC = nc;
                    curR = nr;
                }
            }
        }
    }

    // --- Mud Pits -------------------------------------------------------------

    /** Scatters mud pits with a preference for the middle of the map. */
    private void placeMudPits(GameMap map, Random rng) {
        int count = 5 + rng.nextInt(4); // 5-8

        int midColMin = GameMap.COLS / 4;
        int midColMax = GameMap.COLS * 3 / 4;
        int midRowMin = GameMap.ROWS / 4;
        int midRowMax = GameMap.ROWS * 3 / 4;

        for (int i = 0; i < count; i++) {
            // 70 % chance to prefer mid-map
            int col, row;
            if (rng.nextInt(10) < 7) {
                col = midColMin + rng.nextInt(midColMax - midColMin);
                row = midRowMin + rng.nextInt(midRowMax - midRowMin);
            } else {
                col = rng.nextInt(GameMap.COLS);
                row = rng.nextInt(GameMap.ROWS);
            }
            tryPlace(map, col, row, Obstacle.MUD_PIT);
        }
    }

    // --- Fences ---------------------------------------------------------------

    /**
     * Places fence segments as short corridors of 2-4 connected cells in a
     * single horizontal or vertical line.
     */
    private void placeFences(GameMap map, Random rng) {
        int corridors = 3 + rng.nextInt(3); // 3-5 corridors → 10-15 total segments

        for (int i = 0; i < corridors; i++) {
            boolean horizontal = rng.nextBoolean();
            int segLength = 2 + rng.nextInt(3); // 2-4 segments per corridor

            int col = rng.nextInt(GameMap.COLS);
            int row = rng.nextInt(GameMap.ROWS);

            int dc = horizontal ? 1 : 0;
            int dr = horizontal ? 0 : 1;

            for (int s = 0; s < segLength; s++) {
                tryPlace(map, col + dc * s, row + dr * s, Obstacle.FENCE);
            }
        }
    }
}
