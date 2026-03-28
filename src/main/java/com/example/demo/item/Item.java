package com.example.demo.item;

/**
 * Represents a single item instance placed on the game map.
 *
 * <p>An item has a fixed grid position and a collected state.  Once collected
 * via {@link #collect()} it should no longer be rendered or interactable.
 */
public class Item {

    private final ItemType type;
    private final int col;
    private final int row;
    private boolean collected;

    /**
     * Creates a new, uncollected item at the given grid coordinates.
     *
     * @param type the kind of item
     * @param col  column index on the game map
     * @param row  row index on the game map
     */
    public Item(ItemType type, int col, int row) {
        this.type      = type;
        this.col       = col;
        this.row       = row;
        this.collected = false;
    }

    /** Returns the type of this item. */
    public ItemType getType() {
        return type;
    }

    /** Returns the column position of this item on the game map. */
    public int getCol() {
        return col;
    }

    /** Returns the row position of this item on the game map. */
    public int getRow() {
        return row;
    }

    /** Returns {@code true} if this item has already been collected. */
    public boolean isCollected() {
        return collected;
    }

    /**
     * Marks this item as collected.  Has no effect if already collected.
     */
    public void collect() {
        collected = true;
    }
}
