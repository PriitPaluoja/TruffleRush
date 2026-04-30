package com.example.demo.util;

import com.example.demo.entity.Direction;
import com.example.demo.world.GameMap;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Stateless BFS utility for pathfinding on the game grid.
 *
 * <p>Scratch storage is reused across calls via a per-thread {@link Workspace}.
 * A generation counter avoids clearing the visited / blocked arrays between calls.
 * Coordinates are packed into a single int for an allocation-free queue.
 */
public final class BFS {

    private BFS() {}

    /** Reusable scratch space for one BFS call. */
    private static final class Workspace {
        int[][] visitedGen;
        int[][] blockedGen;
        Direction[][] cameFrom;
        int[] queue = new int[256];
        int generation;
        int cols;
        int rows;

        void ensureSize(int c, int r) {
            if (visitedGen == null || visitedGen.length < c || visitedGen[0].length < r) {
                visitedGen = new int[c][r];
                blockedGen = new int[c][r];
                cameFrom = new Direction[c][r];
            }
            int needed = c * r;
            if (queue.length < needed) {
                queue = new int[Math.max(needed, queue.length * 2)];
            }
            cols = c;
            rows = r;
        }
    }

    private static final ThreadLocal<Workspace> WORKSPACE = ThreadLocal.withInitial(Workspace::new);

    /**
     * Finds the shortest path from (startCol, startRow) to (targetCol, targetRow).
     *
     * @param map          the game map
     * @param startCol     starting column
     * @param startRow     starting row
     * @param targetCol    target column
     * @param targetRow    target row
     * @param blockedCells list of {col, row} pairs treated as soft obstacles
     * @return list of Directions to follow, or empty list if no path found
     */
    public static List<Direction> findPath(
            GameMap map,
            int startCol, int startRow,
            int targetCol, int targetRow,
            List<int[]> blockedCells
    ) {
        if (startCol == targetCol && startRow == targetRow) {
            return Collections.emptyList();
        }

        Workspace ws = WORKSPACE.get();
        ws.ensureSize(map.getColumns(), map.getRows());

        int gen = ++ws.generation;
        if (gen == Integer.MAX_VALUE) {
            for (int c = 0; c < ws.cols; c++) {
                java.util.Arrays.fill(ws.visitedGen[c], 0);
                java.util.Arrays.fill(ws.blockedGen[c], 0);
            }
            ws.generation = 1;
            gen = 1;
        }

        for (int i = 0, n = blockedCells.size(); i < n; i++) {
            int[] cell = blockedCells.get(i);
            int bc = cell[0], br = cell[1];
            if (bc >= 0 && bc < ws.cols && br >= 0 && br < ws.rows) {
                ws.blockedGen[bc][br] = gen;
            }
        }

        int[] queue = ws.queue;
        int rows = ws.rows;
        int head = 0, tail = 0;
        ws.visitedGen[startCol][startRow] = gen;
        queue[tail++] = startCol * rows + startRow;

        boolean found = false;

        outer:
        while (head < tail) {
            int packed = queue[head++];
            int c = packed / rows;
            int r = packed - c * rows;

            for (Direction dir : Direction.CARDINALS) {
                int nc = c + dir.dc;
                int nr = r + dir.dr;

                if (!map.isInBounds(nc, nr)) continue;
                if (!map.isPassable(nc, nr)) continue;
                if (ws.visitedGen[nc][nr] == gen) continue;
                if (ws.blockedGen[nc][nr] == gen) continue;

                ws.visitedGen[nc][nr] = gen;
                ws.cameFrom[nc][nr] = dir;
                queue[tail++] = nc * rows + nr;

                if (nc == targetCol && nr == targetRow) {
                    found = true;
                    break outer;
                }
            }
        }

        if (!found) {
            return Collections.emptyList();
        }

        LinkedList<Direction> path = new LinkedList<>();
        int c = targetCol;
        int r = targetRow;

        while (c != startCol || r != startRow) {
            Direction dir = ws.cameFrom[c][r];
            path.addFirst(dir);
            c -= dir.dc;
            r -= dir.dr;
        }

        return path;
    }
}
