package com.example.demo.render;

import com.example.demo.entity.Direction;
import com.example.demo.entity.Pig;
import com.example.demo.entity.PlayerPig;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

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

    /** Facing direction used when the shape group was last built. */
    private Direction lastFacing;

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
        this.lastFacing = pig.getFacing();
        this.pigGroup  = ShapeFactory.createPigShape(pig.getColor(), lastRadius, lastFacing);
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

        Direction currentFacing = pig.getFacing();
        boolean radiusChanged = Math.abs(currentRadius - lastRadius) > RADIUS_REBUILD_THRESHOLD;
        boolean facingChanged = currentFacing != lastFacing;

        if (radiusChanged || facingChanged) {
            Group newShape = ShapeFactory.createPigShape(pig.getColor(), currentRadius, currentFacing);
            pigGroup.getChildren().setAll(newShape.getChildren());
            lastRadius = currentRadius;
            lastFacing = currentFacing;
        }

        // Power-up aura effects (only for PlayerPig)
        if (pig instanceof PlayerPig pp) {
            if (pp.isSuperPig()) {
                Circle glow = new Circle(0, 0, currentRadius + 8);
                glow.setFill(Color.rgb(255, 215, 0, 0.25));
                glow.setStroke(Color.rgb(255, 200, 0, 0.6));
                glow.setStrokeWidth(2);
                pigGroup.getChildren().addFirst(glow);
            } else if (pp.hasSpeedBoost()) {
                Circle glow = new Circle(0, 0, currentRadius + 6);
                glow.setFill(Color.rgb(60, 120, 255, 0.2));
                glow.setStroke(Color.rgb(80, 140, 255, 0.5));
                glow.setStrokeWidth(1.5);
                pigGroup.getChildren().addFirst(glow);
            }
            if (pp.hasShield()) {
                Circle shield = new Circle(0, 0, currentRadius + 5);
                shield.setFill(Color.TRANSPARENT);
                shield.setStroke(Color.rgb(100, 255, 100, 0.7));
                shield.setStrokeWidth(2);
                pigGroup.getChildren().add(shield);
            }
            if (pp.hasMagnet()) {
                Circle magnet = new Circle(0, 0, currentRadius + 7);
                magnet.setFill(Color.TRANSPARENT);
                magnet.setStroke(Color.rgb(160, 50, 220, 0.5));
                magnet.setStrokeWidth(1.5);
                pigGroup.getChildren().add(magnet);
            }
        }

        // Stun visual
        if (pig.isStunned()) {
            pigGroup.setOpacity(0.5);
        } else {
            pigGroup.setOpacity(1.0);
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
