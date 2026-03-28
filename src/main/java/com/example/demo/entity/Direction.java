package com.example.demo.entity;

/**
 * Represents a cardinal movement direction on the grid.
 * dc = column delta, dr = row delta.
 */
public enum Direction {

    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    NONE(0, 0);

    /** Column delta: negative = left, positive = right. */
    public final int dc;

    /** Row delta: negative = up, positive = down. */
    public final int dr;

    Direction(int dc, int dr) {
        this.dc = dc;
        this.dr = dr;
    }
}
