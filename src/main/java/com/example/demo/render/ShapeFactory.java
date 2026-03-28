package com.example.demo.render;

import com.example.demo.entity.Direction;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Factory that builds JavaFX shape groups representing a pig.
 *
 * All shapes are pure JavaFX primitives (Circle only).
 */
public final class ShapeFactory {

    private ShapeFactory() {
        // Utility class — no instances
    }

    /**
     * Creates a {@link Group} that visually represents a pig.
     *
     * The group is centred at (0, 0); callers position it by translating the group.
     *
     * <ul>
     *   <li>Body — a filled circle with the given {@code radius}.</li>
     *   <li>Snout — a smaller circle offset in the {@code facing} direction.</li>
     *   <li>Ears — two small circles placed at the top of the body.</li>
     * </ul>
     *
     * @param bodyColor colour used for the pig's body
     * @param radius    body radius in pixels
     * @param facing    the direction the pig is facing; {@code NONE} defaults to RIGHT
     * @return a new {@link Group} containing all pig shapes
     */
    public static Group createPigShape(Color bodyColor, double radius, Direction facing) {

        // --- Body ---
        Circle body = new Circle(0, 0, radius);
        body.setFill(bodyColor);
        body.setStroke(bodyColor.darker());
        body.setStrokeWidth(1.5);

        // --- Snout ---
        double snoutRadius = radius * 0.35;
        Direction snoutDir = (facing == Direction.NONE) ? Direction.RIGHT : facing;

        // Offset the snout from the body centre toward the facing direction.
        // The offset places the snout on the surface of the body.
        double snoutOffsetFactor = 0.65;
        double snoutX = snoutDir.dc * radius * snoutOffsetFactor;
        double snoutY = snoutDir.dr * radius * snoutOffsetFactor;

        Circle snout = new Circle(snoutX, snoutY, snoutRadius);
        snout.setFill(bodyColor.darker());
        snout.setStroke(bodyColor.darker().darker());
        snout.setStrokeWidth(1.0);

        // Nostril dots on the snout
        Circle nostrilLeft  = buildNostril(snoutDir, snoutX, snoutY, snoutRadius, -1);
        Circle nostrilRight = buildNostril(snoutDir, snoutX, snoutY, snoutRadius,  1);

        // --- Ears ---
        double earRadius = radius * 0.25;
        // Ears are placed at the upper-left and upper-right of the body
        double earOffset = radius * 0.55;
        Circle earLeft  = new Circle(-earOffset, -radius * 0.65, earRadius);
        Circle earRight = new Circle( earOffset, -radius * 0.65, earRadius);
        Color earColor  = bodyColor.darker();
        earLeft.setFill(earColor);
        earLeft.setStroke(earColor.darker());
        earLeft.setStrokeWidth(1.0);
        earRight.setFill(earColor);
        earRight.setStroke(earColor.darker());
        earRight.setStrokeWidth(1.0);

        Group group = new Group(earLeft, earRight, body, snout, nostrilLeft, nostrilRight);
        return group;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a small nostril dot on the snout, offset perpendicular to the
     * facing direction.
     *
     * @param dir        facing direction
     * @param snoutCX    snout centre X
     * @param snoutCY    snout centre Y
     * @param snoutR     snout radius
     * @param side       -1 = left side, +1 = right side
     * @return a tiny {@link Circle} representing a nostril
     */
    private static Circle buildNostril(Direction dir,
                                       double snoutCX, double snoutCY,
                                       double snoutR, int side) {
        double nostrilR = snoutR * 0.22;

        // Perpendicular axis to the facing direction
        double perpX, perpY;
        if (dir == Direction.UP || dir == Direction.DOWN) {
            // Facing vertically → perpendicular is horizontal
            perpX = side;
            perpY = 0;
        } else {
            // Facing horizontally → perpendicular is vertical
            perpX = 0;
            perpY = side;
        }

        double nx = snoutCX + perpX * snoutR * 0.35;
        double ny = snoutCY + perpY * snoutR * 0.35;

        Circle nostril = new Circle(nx, ny, nostrilR);
        nostril.setFill(Color.rgb(80, 40, 40, 0.7));
        return nostril;
    }
}
