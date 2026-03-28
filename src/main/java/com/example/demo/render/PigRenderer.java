package com.example.demo.render;

import com.example.demo.entity.Pig;
import javafx.scene.Group;

/**
 * Manages the JavaFX scene-graph node that visually represents a {@link Pig}.
 *
 * The node is a {@link Group} whose translate-X/Y are kept synchronised with
 * the pig's grid coordinates each game tick via {@link #update()}.
 *
 * <p>Tile size is fixed at 40 px; each tile's visual centre is at
 * {@code col * TILE_SIZE + TILE_SIZE / 2}.
 */
public class PigRenderer {

    /** Pixels per grid tile. */
    public static final int TILE_SIZE = 40;

    /** Tolerance for triggering a shape rebuild when the radius changes. */
    private static final double RADIUS_REBUILD_THRESHOLD = 1.0;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Pig pig;

    /** The JavaFX node that is added to the scene graph. */
    private Group pigGroup;

    /** Radius used when the current shape group was built; used for change-detection. */
    private double lastRadius;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a renderer for the given pig.
     * Builds the initial shape group immediately.
     *
     * @param pig the pig to render
     */
    public PigRenderer(Pig pig) {
        this.pig       = pig;
        this.lastRadius = pig.getRadius();
        this.pigGroup  = ShapeFactory.createPigShape(pig.getColor(), lastRadius, pig.getFacing());
        repositionGroup();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the JavaFX {@link Group} that represents the pig in the scene graph.
     * Add this node to a parent pane once and keep it there; {@link #update()}
     * will move and reshape it as needed.
     *
     * @return the scene-graph node
     */
    public Group getNode() {
        return pigGroup;
    }

    /**
     * Synchronises the visual node with the pig's current state.
     *
     * <ol>
     *   <li>Repositions the group to the pig's current grid cell.</li>
     *   <li>Rebuilds the shape if the radius has changed significantly,
     *       preserving the group's position in the scene graph by replacing
     *       the group's children in-place.</li>
     * </ol>
     *
     * Call this method once per animation frame / game tick.
     */
    public void update() {
        double currentRadius = pig.getRadius();

        if (Math.abs(currentRadius - lastRadius) > RADIUS_REBUILD_THRESHOLD) {
            // Rebuild the shape children in-place so the group reference
            // held by the scene graph remains valid.
            Group newShape = ShapeFactory.createPigShape(pig.getColor(), currentRadius, pig.getFacing());
            pigGroup.getChildren().setAll(newShape.getChildren());
            lastRadius = currentRadius;
        } else {
            // Only the snout direction may have changed — rebuild cheaply.
            // For simplicity (and because Direction changes are infrequent)
            // we also do a full children-replace here.
            Group newShape = ShapeFactory.createPigShape(pig.getColor(), currentRadius, pig.getFacing());
            pigGroup.getChildren().setAll(newShape.getChildren());
        }

        repositionGroup();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Moves the group's translate-X/Y so that the pig's body is centred on
     * its current tile.
     */
    private void repositionGroup() {
        pigGroup.setTranslateX(pig.getCol() * TILE_SIZE + TILE_SIZE / 2.0);
        pigGroup.setTranslateY(pig.getRow() * TILE_SIZE + TILE_SIZE / 2.0);
    }
}
