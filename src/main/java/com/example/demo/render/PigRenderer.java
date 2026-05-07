package com.example.demo.render;

import com.example.demo.entity.Direction;
import com.example.demo.entity.Pig;
import com.example.demo.entity.PlayerPig;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

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

    // --- Speech bubble (rival taunts, biome flavor, etc.) ---
    private static final int SPEECH_DEFAULT_TICKS = 150; // ~2.5 s at 60 fps
    private static final double SPEECH_FONT_SIZE = 10;
    private static final double SPEECH_PAD_X = 6;
    private static final double SPEECH_PAD_Y = 3;
    private final Group speechGroup = new Group();
    private final Rectangle speechBg = new Rectangle();
    private final Polygon speechTail = new Polygon();
    private final Text speechText = new Text();
    private int speechTicks;

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

        // Speech bubble — hidden until showSpeech() is called.
        speechBg.setFill(Color.rgb(255, 255, 255, 0.92));
        speechBg.setStroke(Color.rgb(40, 40, 40, 0.85));
        speechBg.setStrokeWidth(1.0);
        speechBg.setArcWidth(8);
        speechBg.setArcHeight(8);
        speechTail.setFill(Color.rgb(255, 255, 255, 0.92));
        speechTail.setStroke(Color.rgb(40, 40, 40, 0.85));
        speechTail.setStrokeWidth(1.0);
        speechText.setFont(Font.font("System", FontWeight.NORMAL, SPEECH_FONT_SIZE));
        speechText.setFill(Color.rgb(20, 20, 20));
        speechGroup.getChildren().addAll(speechBg, speechTail, speechText);
        speechGroup.setVisible(false);

        pigGroup.getChildren().addAll(auraLayer, bodyLayer, speechGroup);
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

        if (speechTicks > 0) {
            speechTicks--;
            if (speechTicks == 0) {
                speechGroup.setVisible(false);
            }
        }

        repositionGroup();
    }

    /**
     * Pops a small speech bubble above the pig with the given line of text.
     * Bubble auto-hides after about 2.5 seconds (or when replaced by another
     * call). Call only on AI pigs — the player rarely needs one.
     */
    public void showSpeech(String text) {
        if (text == null || text.isEmpty()) return;
        speechText.setText(text);
        // JavaFX Text reports its layout bounds once a font is set.
        double textW = speechText.getLayoutBounds().getWidth();
        double textH = speechText.getLayoutBounds().getHeight();
        double bgW = textW + SPEECH_PAD_X * 2;
        double bgH = textH + SPEECH_PAD_Y * 2;
        // Anchor: bubble bottom-centre sits just above the pig body.
        double bgX = -bgW / 2.0;
        double bgY = -lastRadius - bgH - 6;
        speechBg.setX(bgX);
        speechBg.setY(bgY);
        speechBg.setWidth(bgW);
        speechBg.setHeight(bgH);
        // Text inside bubble (Text's y is its baseline).
        speechText.setX(bgX + SPEECH_PAD_X);
        speechText.setY(bgY + SPEECH_PAD_Y + textH * 0.8);
        // Tail: small triangle pointing down to the pig's head.
        speechTail.getPoints().setAll(
            -3.0, bgY + bgH - 0.5,
             3.0, bgY + bgH - 0.5,
             0.0, bgY + bgH + 5.0
        );
        speechGroup.setVisible(true);
        speechTicks = SPEECH_DEFAULT_TICKS;
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
