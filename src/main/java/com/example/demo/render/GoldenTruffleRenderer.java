package com.example.demo.render;

import com.example.demo.item.Item;
import com.example.demo.world.GameMap;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

/**
 * Renders the Golden Truffle item and its pulse announcement animation.
 *
 * <p>The truffle is drawn as a gold hexagon with a subtle sinusoidal scale pulse.
 * When first spawned, an expanding translucent gold circle plays once to draw
 * the player's attention to the truffle's position.
 *
 * <p>Usage:
 * <pre>
 *   GoldenTruffleRenderer gtr = new GoldenTruffleRenderer();
 *   sceneRoot.getChildren().add(gtr.getGroup());
 *
 *   // In the game loop (pass state from GoldenTruffleManager):
 *   gtr.update(manager.getGoldenTruffle(),
 *              manager.isPulseActive(),
 *              manager.getPulseRadius());
 * </pre>
 */
public class GoldenTruffleRenderer {

    private static final int    TILE          = GameMap.TILE_SIZE; // 40 px
    private static final double HEX_RADIUS    = 18.0;
    private static final Color  GOLD          = Color.rgb(255, 215, 0);
    private static final Color  GOLD_BRIGHT   = Color.rgb(255, 235, 80);

    // -------------------------------------------------------------------------
    // Scene-graph nodes
    // -------------------------------------------------------------------------

    /** Root group added to the scene; all other nodes are children of this. */
    private final Group group = new Group();

    /** The expanding announcement circle (one shot per spawn). */
    private final Circle pulseCircle;

    /** The gold hexagon that represents the truffle. */
    private final Polygon hexagon;

    /** Internal tick counter used for the sinusoidal size animation. */
    private long animTick = 0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /** Creates a new renderer with all nodes initialised to hidden state. */
    public GoldenTruffleRenderer() {
        // Build hexagon (flat-top, centred at 0,0 — translated later)
        hexagon = regularHexagon(HEX_RADIUS, GOLD);
        hexagon.setStroke(GOLD_BRIGHT);
        hexagon.setStrokeWidth(2.0);
        hexagon.setVisible(false);

        // Build pulse circle
        pulseCircle = new Circle(0, 0, 0);
        pulseCircle.setFill(Color.TRANSPARENT);
        pulseCircle.setStroke(GOLD);
        pulseCircle.setStrokeWidth(3.0);
        pulseCircle.setVisible(false);

        group.getChildren().addAll(pulseCircle, hexagon);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the scene-graph group that should be added to the scene root.
     *
     * @return the root group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Refreshes the visual state of the golden truffle and its pulse animation.
     *
     * @param item        the active truffle item, or {@code null} if none is present
     * @param pulseActive whether the announcement pulse should be visible
     * @param pulseRadius current radius of the announcement pulse circle
     */
    public void update(Item item, boolean pulseActive, double pulseRadius) {
        animTick++;

        if (item != null && !item.isCollected()) {
            double cx = item.getCol() * TILE + TILE / 2.0;
            double cy = item.getRow() * TILE + TILE / 2.0;

            // Position the hexagon
            hexagon.setTranslateX(cx);
            hexagon.setTranslateY(cy);
            hexagon.setVisible(true);

            // Sinusoidal scale: oscillates between 0.85 and 1.15
            double scale = 1.0 + 0.15 * Math.sin(animTick * 0.12);
            hexagon.setScaleX(scale);
            hexagon.setScaleY(scale);

            // Pulse circle
            if (pulseActive) {
                // Opacity fades from 1.0 to 0 as the circle expands
                double maxRadius = 200.0;
                double opacity   = Math.max(0.0, 1.0 - pulseRadius / maxRadius);
                pulseCircle.setCenterX(cx);
                pulseCircle.setCenterY(cy);
                pulseCircle.setRadius(pulseRadius);
                pulseCircle.setStroke(Color.rgb(255, 215, 0, opacity));
                pulseCircle.setVisible(true);
            } else {
                pulseCircle.setVisible(false);
            }

        } else {
            hexagon.setVisible(false);
            pulseCircle.setVisible(false);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Builds a regular flat-top hexagon centred at (0, 0).
     *
     * @param radius pixel distance from centre to each vertex
     * @param fill   fill colour
     * @return configured {@link Polygon}
     */
    private static Polygon regularHexagon(double radius, Color fill) {
        Polygon hex = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60.0 * i); // flat-top: first vertex at 0°
            hex.getPoints().add(radius * Math.cos(angle));
            hex.getPoints().add(radius * Math.sin(angle));
        }
        hex.setFill(fill);
        return hex;
    }
}
