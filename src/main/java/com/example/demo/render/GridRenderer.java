package com.example.demo.render;

import com.example.demo.world.Biome;
import com.example.demo.world.Cell;
import com.example.demo.world.GameMap;
import com.example.demo.world.Obstacle;
import com.example.demo.world.TimeOfDay;
import com.example.demo.world.TimeOfDay.Phase;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.Random;

/**
 * Renders the game grid as a flat group of {@link Rectangle} tiles plus a
 * lightweight decoration layer (grass tufts, reeds, pebbles, dirt patches).
 *
 * <p>Tile fill blends the current time-of-day base color with a per-biome tint
 * (~25% strength). The decoration layer is built once per level via
 * {@link #setLevel(GameMap, Biome)} and stays static thereafter — only the
 * underlying tile fills update each frame.
 */
public class GridRenderer {

    private static final int COLS      = GameMap.COLS;
    private static final int ROWS      = GameMap.ROWS;
    private static final int TILE_SIZE = GameMap.TILE_SIZE;

    /** Maximum per-channel variation applied to each tile (±value). */
    private static final double VARIATION = 0.06;

    /** Strength of the biome tint mixed into the base tile color. */
    private static final double BIOME_TINT_MIX = 0.25;

    /** Probability that a non-obstacle tile gets a decoration. */
    private static final double DECORATION_CHANCE = 0.22;

    private final Group   group;
    private final Group   tileLayer;
    private final Group   decorationLayer;
    private final Rectangle[][] tiles;
    private final double[][]    brightnessOffset;

    private Biome biome = Biome.FOREST;

    public GridRenderer() {
        group            = new Group();
        tileLayer        = new Group();
        decorationLayer  = new Group();
        tiles            = new Rectangle[COLS][ROWS];
        brightnessOffset = new double[COLS][ROWS];

        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
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
                tileLayer.getChildren().add(rect);
            }
        }
        // Decorations sit on top of tiles but below obstacles/items (caller controls scene order).
        group.getChildren().addAll(tileLayer, decorationLayer);
    }

    public Group getGroup() {
        return group;
    }

    /**
     * Configures the renderer for the upcoming level: stores the biome (used
     * for tile tinting) and rebuilds the decoration layer for non-obstacle
     * cells. Call once per level.
     */
    public void setLevel(GameMap map, Biome biome) {
        this.biome = biome;
        rebuildDecorations(map, biome);
    }

    /**
     * Refreshes every tile's fill and stroke to match the current time-of-day
     * progress, blended with the active biome's tint.
     */
    public void update(double timeProgress) {
        Color base   = TimeOfDay.getTileBaseColor(timeProgress);
        Color tinted = blend(base, biome.tileTint(), BIOME_TINT_MIX);
        Phase phase  = TimeOfDay.getPhase(timeProgress);
        Color stroke = strokeColor(phase, timeProgress);

        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                double offset = brightnessOffset[col][row];
                tiles[col][row].setFill(varyBrightness(tinted, offset));
                tiles[col][row].setStroke(stroke);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Decoration layer
    // -------------------------------------------------------------------------

    private void rebuildDecorations(GameMap map, Biome biome) {
        decorationLayer.getChildren().clear();
        Color decoColor = biome.decorationColor();

        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                Cell cell = map.getCell(col, row);
                Obstacle obs = cell.getObstacle();
                if (obs != null) continue;

                // Deterministic per-tile RNG so the same map layout always
                // gets the same decoration sprinkle (daily runs reproduce).
                Random rng = new Random((long) col * 7919L + row * 31L + biome.ordinal() * 101L);
                if (rng.nextDouble() > DECORATION_CHANCE) continue;

                double cx = col * TILE_SIZE + TILE_SIZE / 2.0
                          + (rng.nextDouble() - 0.5) * TILE_SIZE * 0.5;
                double cy = row * TILE_SIZE + TILE_SIZE / 2.0
                          + (rng.nextDouble() - 0.5) * TILE_SIZE * 0.5;

                switch (biome) {
                    case FOREST: addGrassTuft(cx, cy, decoColor, rng);     break;
                    case SWAMP:  addReedsOrAlgae(cx, cy, decoColor, rng);  break;
                    case FARM:   addDirtPatch(cx, cy, decoColor, rng);     break;
                }
            }
        }
    }

    /** Forest: 3 short blade lines fanning up. */
    private void addGrassTuft(double cx, double cy, Color color, Random rng) {
        double h = 4 + rng.nextDouble() * 3;
        for (int i = -1; i <= 1; i++) {
            Line blade = new Line(cx + i * 1.2, cy + 1.5,
                                  cx + i * 1.2 + i * 0.8, cy - h);
            blade.setStroke(color);
            blade.setStrokeWidth(0.9);
            decorationLayer.getChildren().add(blade);
        }
    }

    /** Swamp: a vertical reed plus a tiny algae speck. */
    private void addReedsOrAlgae(double cx, double cy, Color color, Random rng) {
        if (rng.nextDouble() < 0.5) {
            Line reed = new Line(cx, cy + 3, cx + (rng.nextDouble() - 0.5) * 2, cy - 6);
            reed.setStroke(color);
            reed.setStrokeWidth(1.0);
            decorationLayer.getChildren().add(reed);
        } else {
            Ellipse algae = new Ellipse(cx, cy, 3.5, 1.5);
            algae.setFill(color);
            decorationLayer.getChildren().add(algae);
        }
    }

    /** Farm: small earthy patch or pebble. */
    private void addDirtPatch(double cx, double cy, Color color, Random rng) {
        if (rng.nextDouble() < 0.6) {
            Ellipse patch = new Ellipse(cx, cy, 4 + rng.nextDouble() * 2, 2 + rng.nextDouble());
            patch.setFill(color);
            decorationLayer.getChildren().add(patch);
        } else {
            Circle pebble = new Circle(cx, cy, 1.2 + rng.nextDouble() * 0.8);
            pebble.setFill(Color.rgb(150, 140, 120, 0.7));
            decorationLayer.getChildren().add(pebble);
        }
    }

    // -------------------------------------------------------------------------
    // Color utilities
    // -------------------------------------------------------------------------

    /** Blends two colors with the given mix factor in [0, 1]. */
    private static Color blend(Color a, Color b, double mix) {
        double r = a.getRed()   * (1 - mix) + b.getRed()   * mix;
        double g = a.getGreen() * (1 - mix) + b.getGreen() * mix;
        double bl = a.getBlue() * (1 - mix) + b.getBlue()  * mix;
        return new Color(clamp(r), clamp(g), clamp(bl), 1.0);
    }

    private static Color varyBrightness(Color c, double offset) {
        double r = clamp(c.getRed()   + offset);
        double g = clamp(c.getGreen() + offset);
        double b = clamp(c.getBlue()  + offset);
        return new Color(r, g, b, 1.0);
    }

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
