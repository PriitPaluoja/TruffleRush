package com.example.demo.world;

/**
 * Holds the 2-D grid of {@link Cell} objects that make up the game world.
 *
 * Grid dimensions:
 *   COLS      = 20  (left → right)
 *   ROWS      = 15  (top  → bottom)
 *   TILE_SIZE = 40  (pixels per tile)
 */
public class GameMap {

    public static final int COLS      = 25;
    public static final int ROWS      = 17;
    public static final int TILE_SIZE = 40;

    private final Cell[][] cells;

    public GameMap() {
        cells = new Cell[COLS][ROWS];
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                cells[col][row] = new Cell(col, row);
            }
        }
    }

    /**
     * Returns the {@link Cell} at the given grid coordinates.
     *
     * @param col column index (0-based)
     * @param row row index (0-based)
     * @return the cell at (col, row)
     * @throws ArrayIndexOutOfBoundsException if coordinates are out of range
     */
    public Cell getCell(int col, int row) {
        return cells[col][row];
    }

    /**
     * Returns the number of columns in this map.
     *
     * @return column count
     */
    public int getColumns() {
        return COLS;
    }

    /**
     * Returns the number of rows in this map.
     *
     * @return row count
     */
    public int getRows() {
        return ROWS;
    }

    /**
     * Returns {@code true} if the given coordinates are within the map boundaries.
     *
     * @param col column index
     * @param row row index
     * @return {@code true} when in bounds
     */
    public boolean isInBounds(int col, int row) {
        return col >= 0 && col < COLS && row >= 0 && row < ROWS;
    }

    /**
     * Returns {@code true} when the given cell can be entered by an entity.
     * Delegates to {@link Cell#isPassable()} so that obstacle state is respected.
     *
     * @param col column index
     * @param row row index
     * @return {@code true} if the cell is in bounds and passable
     */
    public boolean isPassable(int col, int row) {
        return isInBounds(col, row) && cells[col][row].isPassable();
    }
}
