package com.example.demo.world;

/**
 * Represents a single tile on the game grid.
 * Holds the grid coordinates of the cell and an optional {@link Obstacle}.
 */
public class Cell {

    private final int col;
    private final int row;

    /** The obstacle occupying this cell, or {@code null} when the cell is clear. */
    private Obstacle obstacle;

    public Cell(int col, int row) {
        this.col      = col;
        this.row      = row;
        this.obstacle = null;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    // -------------------------------------------------------------------------
    // Obstacle support
    // -------------------------------------------------------------------------

    /**
     * Returns the obstacle on this cell, or {@code null} if none is present.
     *
     * @return the current obstacle, may be {@code null}
     */
    public Obstacle getObstacle() {
        return obstacle;
    }

    /**
     * Places or removes an obstacle on this cell.
     *
     * @param o the obstacle to place, or {@code null} to clear the cell
     */
    public void setObstacle(Obstacle o) {
        this.obstacle = o;
    }

    /**
     * Returns whether this cell can be entered by an entity.
     * A cell is passable when it has no obstacle, or when its obstacle is
     * itself passable (e.g. {@link Obstacle#MUD_PIT}).
     *
     * @return {@code true} if an entity may enter this cell
     */
    public boolean isPassable() {
        return obstacle == null || obstacle.isPassable();
    }

    /**
     * Returns the movement-speed multiplier that applies when an entity
     * crosses this cell.
     *
     * <ul>
     *   <li>{@link Obstacle#MUD_PIT} → {@code 0.5} (half speed)</li>
     *   <li>All other cases → {@code 1.0} (full speed)</li>
     * </ul>
     *
     * @return speed multiplier in the range (0, 1]
     */
    public double getSpeedMultiplier() {
        if (obstacle == Obstacle.MUD_PIT) {
            return 0.5;
        }
        return 1.0;
    }

    @Override
    public String toString() {
        return "Cell(" + col + ", " + row + ", obstacle=" + obstacle + ")";
    }
}
