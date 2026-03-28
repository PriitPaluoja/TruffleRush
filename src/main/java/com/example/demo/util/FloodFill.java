package com.example.demo.util;

import com.example.demo.world.GameMap;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Utility that tests map connectivity using a BFS flood-fill.
 *
 * <p>A map is considered <em>fully connected</em> when every passable cell
 * can be reached from every other passable cell without crossing an
 * impassable obstacle.</p>
 */
public final class FloodFill {

    private FloodFill() {
        // utility class – no instances
    }

    /**
     * Checks whether all passable cells in {@code map} form a single
     * connected region.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Scan the grid to count all passable cells and find the first one.</li>
     *   <li>BFS from that first cell, visiting 4-connected passable neighbours.</li>
     *   <li>If the number of cells reached equals the total passable count the
     *       map is fully connected.</li>
     * </ol>
     *
     * @param map the game map to test
     * @return {@code true} if all passable cells are reachable from one another;
     *         {@code false} if isolated regions exist, or if there are no passable
     *         cells at all (treated as trivially connected – returns {@code true})
     */
    public static boolean isFullyConnected(GameMap map) {
        int cols = map.getColumns();
        int rows = map.getRows();

        // --- 1. locate first passable cell and count all passable cells -------
        int startCol = -1;
        int startRow = -1;
        int totalPassable = 0;

        outer:
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (map.isPassable(c, r)) {
                    totalPassable++;
                    if (startCol == -1) {
                        startCol = c;
                        startRow = r;
                    }
                }
            }
        }

        // No passable cells at all – vacuously connected.
        if (totalPassable == 0) {
            return true;
        }

        // --- 2. BFS flood fill from the starting cell -------------------------
        boolean[][] visited = new boolean[cols][rows];
        Queue<int[]> queue  = new ArrayDeque<>();

        visited[startCol][startRow] = true;
        queue.add(new int[]{startCol, startRow});
        int reached = 0;

        // 4-directional offsets: up, down, left, right
        int[] dc = {0, 0, -1, 1};
        int[] dr = {-1, 1, 0, 0};

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            reached++;

            for (int d = 0; d < 4; d++) {
                int nc = pos[0] + dc[d];
                int nr = pos[1] + dr[d];

                if (map.isInBounds(nc, nr) && !visited[nc][nr] && map.isPassable(nc, nr)) {
                    visited[nc][nr] = true;
                    queue.add(new int[]{nc, nr});
                }
            }
        }

        // --- 3. compare reached cells to total passable cells -----------------
        return reached == totalPassable;
    }
}
