package com.example.demo.util;

import com.example.demo.entity.Direction;
import com.example.demo.item.Item;
import com.example.demo.world.GameMap;

import java.util.*;

/**
 * Stateless BFS utility for pathfinding on the game grid.
 */
public final class BFS {

    private BFS() {
        // utility class – no instances
    }

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

        // Build a set of blocked positions for fast lookup
        Set<Long> blocked = new HashSet<>();
        for (int[] cell : blockedCells) {
            blocked.add(encodeCell(cell[0], cell[1]));
        }

        int cols = map.getColumns();
        int rows = map.getRows();

        boolean[][] visited = new boolean[cols][rows];
        // Store parent direction: which direction was taken to reach this cell
        Direction[][] cameFrom = new Direction[cols][rows];

        Queue<int[]> queue = new ArrayDeque<>();
        visited[startCol][startRow] = true;
        queue.add(new int[]{startCol, startRow});

        boolean found = false;

        outer:
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int c = current[0];
            int r = current[1];

            for (Direction dir : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
                int nc = c + dir.dc;
                int nr = r + dir.dr;

                if (!map.isInBounds(nc, nr)) continue;
                if (!map.isPassable(nc, nr)) continue;
                if (visited[nc][nr]) continue;
                if (blocked.contains(encodeCell(nc, nr))) continue;

                visited[nc][nr] = true;
                cameFrom[nc][nr] = dir;
                queue.add(new int[]{nc, nr});

                if (nc == targetCol && nr == targetRow) {
                    found = true;
                    break outer;
                }
            }
        }

        if (!found) {
            return Collections.emptyList();
        }

        // Reconstruct path by tracing back from target to start
        LinkedList<Direction> path = new LinkedList<>();
        int c = targetCol;
        int r = targetRow;

        while (c != startCol || r != startRow) {
            Direction dir = cameFrom[c][r];
            path.addFirst(dir);
            c -= dir.dc;
            r -= dir.dr;
        }

        return path;
    }

    /**
     * Finds the closest uncollected item from a list, within an optional max BFS distance.
     *
     * @param map         the game map
     * @param fromCol     starting column
     * @param fromRow     starting row
     * @param items       list of items to consider
     * @param maxDistance maximum BFS distance (use Integer.MAX_VALUE for unlimited)
     * @return the closest uncollected item, or null if none found within range
     */
    public static Item findClosestItem(
            GameMap map,
            int fromCol, int fromRow,
            List<Item> items,
            int maxDistance
    ) {
        // Build a quick lookup map of uncollected item positions
        Map<Long, Item> itemMap = new HashMap<>();
        for (Item item : items) {
            if (!item.isCollected()) {
                itemMap.put(encodeCell(item.getCol(), item.getRow()), item);
            }
        }

        if (itemMap.isEmpty()) {
            return null;
        }

        int cols = map.getColumns();
        int rows = map.getRows();

        boolean[][] visited = new boolean[cols][rows];
        // Queue entries: {col, row, distance}
        Queue<int[]> queue = new ArrayDeque<>();
        visited[fromCol][fromRow] = true;
        queue.add(new int[]{fromCol, fromRow, 0});

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int c = current[0];
            int r = current[1];
            int dist = current[2];

            if (dist > maxDistance) {
                break;
            }

            Item found = itemMap.get(encodeCell(c, r));
            if (found != null) {
                return found;
            }

            for (Direction dir : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
                int nc = c + dir.dc;
                int nr = r + dir.dr;

                if (!map.isInBounds(nc, nr)) continue;
                if (!map.isPassable(nc, nr)) continue;
                if (visited[nc][nr]) continue;

                visited[nc][nr] = true;
                queue.add(new int[]{nc, nr, dist + 1});
            }
        }

        return null;
    }

    /** Encodes (col, row) into a single long for use as a hash key. */
    private static long encodeCell(int col, int row) {
        return ((long) col << 16) | (row & 0xFFFF);
    }
}
