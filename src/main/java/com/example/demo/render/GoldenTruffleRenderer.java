package com.example.demo.render;

import com.example.demo.item.Item;
import com.example.demo.world.GameMap;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;

/**
 * Renders the Golden Truffle item and its pulse announcement animation.
 *
 * <p>The truffle is a faceted gold gem (outer hex, inner hex highlight, four
 * facet triangles, two light blooms) surrounded by a slowly rotating ring of
 * sparkles. A sinusoidal scale pulse plays continuously, and an expanding gold
 * circle plays once per spawn to draw the player's attention.
 */
public class GoldenTruffleRenderer {

    private static final int    TILE          = GameMap.TILE_SIZE; // 40 px
    private static final double HEX_RADIUS    = 18.0;
    private static final Color  GOLD          = Color.rgb(255, 215, 0);
    private static final Color  GOLD_BRIGHT   = Color.rgb(255, 235, 80);

    /** Root group added to the scene. */
    private final Group group = new Group();

    /** The expanding announcement circle (one shot per spawn). */
    private final Circle pulseCircle;

    /** The combined truffle visual (hex, facets, blooms). Translated each frame. */
    private final Group truffle;

    /** Sub-group for sparkles, rotated slowly each frame for shimmer. */
    private final Group sparkles;

    /** Internal tick counter used for the sinusoidal size animation. */
    private long animTick = 0;

    public GoldenTruffleRenderer() {
        // Outer light bloom
        Circle bloomOuter = new Circle(0, 0, HEX_RADIUS + 8, Color.rgb(255, 220, 100, 0.16));
        Circle bloomInner = new Circle(0, 0, HEX_RADIUS + 2, Color.rgb(255, 230, 120, 0.28));

        // Outer hex
        Polygon outerHex = regularHexagon(HEX_RADIUS, GOLD);
        outerHex.setStroke(GOLD_BRIGHT);
        outerHex.setStrokeWidth(2.0);

        // Facet triangles giving the gem-cut look
        Polygon facetTopL = new Polygon(0.0, -HEX_RADIUS, -HEX_RADIUS * 0.85, -HEX_RADIUS * 0.5, 0.0, 0.0);
        Polygon facetTopR = new Polygon(0.0, -HEX_RADIUS,  HEX_RADIUS * 0.85, -HEX_RADIUS * 0.5, 0.0, 0.0);
        Polygon facetBotL = new Polygon(0.0,  HEX_RADIUS, -HEX_RADIUS * 0.85,  HEX_RADIUS * 0.5, 0.0, 0.0);
        Polygon facetBotR = new Polygon(0.0,  HEX_RADIUS,  HEX_RADIUS * 0.85,  HEX_RADIUS * 0.5, 0.0, 0.0);
        facetTopL.setFill(Color.rgb(255, 240, 130, 0.55));
        facetTopR.setFill(Color.rgb(220, 160, 0, 0.45));
        facetBotL.setFill(Color.rgb(220, 160, 0, 0.45));
        facetBotR.setFill(Color.rgb(255, 240, 130, 0.55));

        // Inner highlight hex
        Polygon innerHex = regularHexagon(HEX_RADIUS * 0.45, Color.rgb(255, 250, 200, 0.85));
        innerHex.setTranslateX(-2.5);
        innerHex.setTranslateY(-2.5);

        // Inner shine ellipse
        Ellipse shine = new Ellipse(-HEX_RADIUS * 0.3, -HEX_RADIUS * 0.4,
                                    HEX_RADIUS * 0.3, HEX_RADIUS * 0.18);
        shine.setFill(Color.rgb(255, 255, 255, 0.7));

        // Five sparkles in a slowly rotating sub-group
        sparkles = new Group();
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(i * 72);
            double r = HEX_RADIUS + 6;
            sparkles.getChildren().add(sparkle(Math.cos(a) * r, Math.sin(a) * r, 2.6));
        }

        truffle = new Group(bloomOuter, bloomInner,
                            outerHex,
                            facetTopL, facetTopR, facetBotL, facetBotR,
                            innerHex, shine,
                            sparkles);
        truffle.setVisible(false);

        // Pulse circle
        pulseCircle = new Circle(0, 0, 0);
        pulseCircle.setFill(Color.TRANSPARENT);
        pulseCircle.setStroke(GOLD);
        pulseCircle.setStrokeWidth(3.0);
        pulseCircle.setVisible(false);

        group.getChildren().addAll(pulseCircle, truffle);
    }

    public Group getGroup() {
        return group;
    }

    /**
     * Refreshes the visual state of the golden truffle and its pulse animation.
     */
    public void update(Item item, boolean pulseActive, double pulseRadius) {
        animTick++;

        if (item != null && !item.isCollected()) {
            double cx = item.getCol() * TILE + TILE / 2.0;
            double cy = item.getRow() * TILE + TILE / 2.0;

            truffle.setTranslateX(cx);
            truffle.setTranslateY(cy);
            truffle.setVisible(true);

            // Sinusoidal scale: oscillates between 0.85 and 1.15
            double scale = 1.0 + 0.15 * Math.sin(animTick * 0.12);
            truffle.setScaleX(scale);
            truffle.setScaleY(scale);

            // Slow sparkle rotation (full rotation every ~10 seconds at 60fps)
            sparkles.setRotate(animTick * 0.6);

            if (pulseActive) {
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
            truffle.setVisible(false);
            pulseCircle.setVisible(false);
        }
    }

    /** Tiny 4-point sparkle star at (cx, cy). */
    private static Polygon sparkle(double cx, double cy, double size) {
        Polygon s = new Polygon(
            cx,                  cy - size,
            cx + size * 0.35,    cy - size * 0.35,
            cx + size,           cy,
            cx + size * 0.35,    cy + size * 0.35,
            cx,                  cy + size,
            cx - size * 0.35,    cy + size * 0.35,
            cx - size,           cy,
            cx - size * 0.35,    cy - size * 0.35
        );
        s.setFill(Color.rgb(255, 255, 200, 0.9));
        return s;
    }

    /**
     * Builds a regular flat-top hexagon centred at (0, 0).
     */
    private static Polygon regularHexagon(double radius, Color fill) {
        Polygon hex = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60.0 * i);
            hex.getPoints().add(radius * Math.cos(angle));
            hex.getPoints().add(radius * Math.sin(angle));
        }
        hex.setFill(fill);
        return hex;
    }
}
