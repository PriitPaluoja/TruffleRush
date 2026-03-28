package com.example.demo.render;

import com.example.demo.world.GameMap;
import com.example.demo.world.TimeOfDay;
import com.example.demo.world.TimeOfDay.Phase;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Random;

/**
 * Renders the game grid as a flat group of {@link Rectangle} tiles.
 *
 * Each tile is a 40×40 rectangle whose fill color is derived from the current
 * time-of-day progress.  A small, per-tile random brightness variation is baked
 * in at construction time so the grid looks natural rather than flat.
 *
 * The stroke color darkens during dusk and night to give stronger tile borders.
 */
public class GridRenderer {

    private static final int COLS      = GameMap.COLS;
    private static final int ROWS      = GameMap.ROWS;
    private static final int TILE_SIZE = GameMap.TILE_SIZE;

    /** Maximum per-channel variation applied to each tile (±value). */
    private static final double VARIATION = 0.06;

    private final Group   group;
    private final Rectangle[][] tiles;

    /**
     * Per-tile brightness offsets in [-VARIATION, +VARIATION].
     * Seeded from (col * ROWS + row) so the pattern is deterministic.
     */
    private final double[][] brightnessOffset;

    public GridRenderer() {
        group            = new Group();
        tiles            = new Rectangle[COLS][ROWS];
        brightnessOffset = new double[COLS][ROWS];

        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                // Deterministic per-tile seed
                Random rng = new Random((long) col * ROWS + row);
                brightnessOffset[col][row] = (rng.nextDouble() * 2.0 - 1.0) * VARIATION;

                Rectangle rect = new Rectangle(
                        col * TILE_SIZE,
                        row * TILE_SIZE,
                        TILE_SIZE,
                        TILE_SIZE
                );
                rect.setStrokeWidth(0.5);
                tiles[col][row] = rect;
                group.getChildren().add(rect);
            }
        }
    }

    /** Returns the JavaFX node that should be added to the scene graph. */
    public Group getGroup() {
        return group;
    }

    /**
     * Refreshes every tile's fill and stroke to match the given time-of-day progress.
     *
     * @param timeProgress a value in [0.0, 1.0] representing how far through the day cycle we are
     */
    public void update(double timeProgress) {
        Color base   = TimeOfDay.getTileBaseColor(timeProgress);
        Phase phase  = TimeOfDay.getPhase(timeProgress);
        Color stroke = strokeColor(phase, timeProgress);

        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                double offset = brightnessOffset[col][row];
                Color tileColor = varyBrightness(base, offset);
                tiles[col][row].setFill(tileColor);
                tiles[col][row].setStroke(stroke);
            }
        }
    }

    /**
     * Applies a brightness offset to a color without clamping hue/saturation.
     * Adds the offset to each RGB channel independently.
     */
    private static Color varyBrightness(Color c, double offset) {
        double r = clamp(c.getRed()   + offset);
        double g = clamp(c.getGreen() + offset);
        double b = clamp(c.getBlue()  + offset);
        return new Color(r, g, b, 1.0);
    }

    /**
     * Returns a stroke color that becomes more visible (darker/lighter) during
     * dusk and night phases to emphasise tile boundaries when the scene is dim.
     */
    private static Color strokeColor(Phase phase, double progress) {
        return switch (phase) {
            case DAWN  -> Color.rgb(180, 140, 100, 0.4);
            case DAY   -> Color.rgb( 80, 130,  50, 0.3);
            case DUSK  -> Color.rgb(130,  90,  40, 0.6);
            case NIGHT -> Color.rgb( 20,  25,  40, 0.7);
        };
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
