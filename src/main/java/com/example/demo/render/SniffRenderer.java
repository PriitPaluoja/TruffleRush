package com.example.demo.render;

import com.example.demo.world.GameMap;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Renders the sniff ability visual: an expanding faint green pulse circle
 * centred on the player pig's tile.
 *
 * <p>When sniff becomes active the circle expands from radius 0 to 80 px
 * over 30 ticks, then holds at 80 px for the remainder of the sniff
 * duration.  When sniff ends the circle is hidden.
 */
public class SniffRenderer {

    private static final int   TILE        = GameMap.TILE_SIZE; // 40 px
    private static final double MAX_RADIUS = 2.0 * TILE;       // 80 px
    private static final int   GROW_TICKS  = 30;

    /** Counts ticks since the pulse started (capped at GROW_TICKS). */
    private int     pulseTick = 0;

    /** Whether the pulse is currently running. */
    private boolean active    = false;

    /** The previous sniff-active state — used to detect the rising edge. */
    private boolean prevSniffActive = false;

    private final Circle pulseCircle;
    private final Group  group;

    public SniffRenderer() {
        pulseCircle = new Circle(0, 0, 0);
        pulseCircle.setFill(Color.TRANSPARENT);
        pulseCircle.setStroke(Color.rgb(200, 255, 200, 0.5));
        pulseCircle.setStrokeWidth(2.0);
        pulseCircle.setVisible(false);

        group = new Group(pulseCircle);
    }

    /** Returns the node that should be added to the scene graph. */
    public Group getGroup() {
        return group;
    }

    /**
     * Updates pulse state and circle geometry each game tick.
     *
     * @param sniffActive whether the player's sniff ability is currently active
     * @param playerCol   player's current column on the grid
     * @param playerRow   player's current row on the grid
     */
    public void update(boolean sniffActive, int playerCol, int playerRow) {
        // Detect rising edge: sniff just became active
        if (sniffActive && !prevSniffActive) {
            active    = true;
            pulseTick = 0;
        }

        // Detect falling edge: sniff just ended
        if (!sniffActive && prevSniffActive) {
            active = false;
            pulseCircle.setVisible(false);
        }

        prevSniffActive = sniffActive;

        if (!active) return;

        // Advance expansion counter
        if (pulseTick < GROW_TICKS) {
            pulseTick++;
        }

        // Compute radius: linearly expand from 0 → MAX_RADIUS over GROW_TICKS
        double radius = MAX_RADIUS * ((double) pulseTick / GROW_TICKS);

        // Centre the circle on the player's tile centre
        double cx = playerCol * TILE + TILE / 2.0;
        double cy = playerRow * TILE + TILE / 2.0;

        pulseCircle.setCenterX(cx);
        pulseCircle.setCenterY(cy);
        pulseCircle.setRadius(radius);
        pulseCircle.setVisible(true);
    }
}
