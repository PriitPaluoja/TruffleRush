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
 * <p>Layout:
 * <pre>
 *   pigGroup
 *     ├─ auraLayer   (cached Circles for power-up halos, visibility-toggled)
 *     └─ bodyLayer   (body shape, rebuilt on radius/facing change)
 * </pre>
 */
public class PigRenderer {

    public static final int TILE_SIZE = 40;

    private static final double RADIUS_REBUILD_THRESHOLD = 1.0;

    private final Pig pig;
    private final Group pigGroup = new Group();
    private final Group auraLayer = new Group();
    private final Group bodyLayer;

    private final Circle superHalo;
    private final Circle speedHalo;
    private final Circle shieldHalo;
    private final Circle magnetHalo;

    private double lastRadius;
    private Direction lastFacing;

    public PigRenderer(Pig pig) {
        this.pig = pig;
        this.lastRadius = pig.getRadius();
        this.lastFacing = pig.getFacing();

        this.superHalo  = makeHalo(lastRadius + 8, Color.rgb(255, 215, 0, 0.25),
                                   Color.rgb(255, 200, 0, 0.6),  2.0);
        this.speedHalo  = makeHalo(lastRadius + 6, Color.rgb(60, 120, 255, 0.2),
                                   Color.rgb(80, 140, 255, 0.5), 1.5);
        this.shieldHalo = makeHalo(lastRadius + 5, Color.TRANSPARENT,
                                   Color.rgb(100, 255, 100, 0.7), 2.0);
        this.magnetHalo = makeHalo(lastRadius + 7, Color.TRANSPARENT,
                                   Color.rgb(160, 50, 220, 0.5), 1.5);
        superHalo.setVisible(false);
        speedHalo.setVisible(false);
        shieldHalo.setVisible(false);
        magnetHalo.setVisible(false);
        auraLayer.getChildren().addAll(superHalo, speedHalo, shieldHalo, magnetHalo);

        Group initialBody = ShapeFactory.createPigShape(pig.getColor(), lastRadius, lastFacing);
        this.bodyLayer = new Group();
        bodyLayer.getChildren().setAll(initialBody.getChildren());

        pigGroup.getChildren().addAll(auraLayer, bodyLayer);
        repositionGroup();
    }

    public Group getNode() {
        return pigGroup;
    }

    public void update() {
        double currentRadius = pig.getRadius();
        Direction currentFacing = pig.getFacing();
        boolean radiusChanged = Math.abs(currentRadius - lastRadius) > RADIUS_REBUILD_THRESHOLD;
        boolean facingChanged = currentFacing != lastFacing;

        if (radiusChanged || facingChanged) {
            Group newShape = ShapeFactory.createPigShape(pig.getColor(), currentRadius, currentFacing);
            bodyLayer.getChildren().setAll(newShape.getChildren());
            lastRadius = currentRadius;
            lastFacing = currentFacing;
            // Keep halo radii proportional to body radius.
            superHalo.setRadius(currentRadius + 8);
            speedHalo.setRadius(currentRadius + 6);
            shieldHalo.setRadius(currentRadius + 5);
            magnetHalo.setRadius(currentRadius + 7);
        }

        if (pig instanceof PlayerPig pp) {
            superHalo.setVisible(pp.isSuperPig());
            speedHalo.setVisible(!pp.isSuperPig() && pp.hasSpeedBoost());
            shieldHalo.setVisible(pp.hasShield());
            magnetHalo.setVisible(pp.hasMagnet());
        }

        pigGroup.setOpacity(pig.isStunned() ? 0.5 : 1.0);
        repositionGroup();
    }

    private static Circle makeHalo(double radius, Color fill, Color stroke, double strokeWidth) {
        Circle c = new Circle(0, 0, radius);
        c.setFill(fill);
        c.setStroke(stroke);
        c.setStrokeWidth(strokeWidth);
        return c;
    }

    private void repositionGroup() {
        pigGroup.setTranslateX(pig.getCol() * TILE_SIZE + TILE_SIZE / 2.0);
        pigGroup.setTranslateY(pig.getRow() * TILE_SIZE + TILE_SIZE / 2.0);
    }
}
