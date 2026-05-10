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
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

import java.util.Random;

/**
 * Renders the game grid as a flat group of {@link Rectangle} tiles plus a
 * dense decoration layer (grass tufts, ferns, flowers, reeds, lily pads,
 * pebbles, wheat stalks, cart tracks).
 *
 * <p>Tile fill blends the current time-of-day base color with a per-biome tint
 * (~25% strength). The decoration layer is built once per level via
 * {@link #setLevel(GameMap, Biome)} and stays static thereafter — only the
 * underlying tile fills update each frame.
 *
 * <p>Decoration randomness is seeded by {@code (col, row, biome)} so daily
 * seeded runs reproduce the same sprinkle.
 */
public class GridRenderer {

    private static final int COLS      = GameMap.COLS;
    private static final int ROWS      = GameMap.ROWS;
    private static final int TILE_SIZE = GameMap.TILE_SIZE;

    /** Maximum per-channel variation applied to each tile (±value). */
    private static final double VARIATION = 0.06;

    /** Strength of the biome tint mixed into the base tile color. */
    private static final double BIOME_TINT_MIX = 0.25;

    /** Probability that a non-obstacle tile gets at least one decoration. */
    private static final double DECORATION_CHANCE = 0.50;

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
        group.getChildren().addAll(tileLayer, decorationLayer);
    }

    public Group getGroup() {
        return group;
    }

    public void setLevel(GameMap map, Biome biome) {
        this.biome = biome;
        rebuildDecorations(map, biome);
    }

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

                // Deterministic per-tile RNG (2-prime hash + biome) so daily
                // runs always get the same decoration sprinkle.
                long seed = (long) col * 73856093L + (long) row * 19349663L
                          + (long) biome.ordinal() * 83492791L;
                Random rng = new Random(seed);
                if (rng.nextDouble() > DECORATION_CHANCE) continue;

                // 1–3 decorations per chosen tile, each at a small offset
                int count = 1 + rng.nextInt(3);
                for (int n = 0; n < count; n++) {
                    double cx = col * TILE_SIZE + TILE_SIZE / 2.0
                              + (rng.nextDouble() - 0.5) * TILE_SIZE * 0.7;
                    double cy = row * TILE_SIZE + TILE_SIZE / 2.0
                              + (rng.nextDouble() - 0.5) * TILE_SIZE * 0.7;

                    switch (biome) {
                        case FOREST: addForestDecoration(cx, cy, decoColor, rng);  break;
                        case SWAMP:  addSwampDecoration(cx, cy, decoColor, rng);   break;
                        case FARM:   addFarmDecoration(cx, cy, decoColor, rng);    break;
                    }
                }
            }
        }
    }

    // --- FOREST --------------------------------------------------------------

    /** Forest: 5-blade grass tuft, 4-petal flower, 3-segment fern, or acorn shell. */
    private void addForestDecoration(double cx, double cy, Color color, Random rng) {
        double pick = rng.nextDouble();
        if (pick < 0.55) {
            addGrassTuft(cx, cy, color, rng);
        } else if (pick < 0.78) {
            addForestFlower(cx, cy, rng);
        } else if (pick < 0.93) {
            addFern(cx, cy, color, rng);
        } else {
            addAcornShell(cx, cy);
        }
    }

    /** 5-blade grass tuft fanning up. */
    private void addGrassTuft(double cx, double cy, Color color, Random rng) {
        double h = 4 + rng.nextDouble() * 3;
        for (int i = -2; i <= 2; i++) {
            double lean = i * 0.6;
            Line blade = new Line(cx + i * 1.0, cy + 1.5,
                                  cx + i * 1.0 + lean, cy - h);
            blade.setStroke(color);
            blade.setStrokeWidth(0.8);
            decorationLayer.getChildren().add(blade);
        }
    }

    /** Tiny 4-petal flower: 4 petal Circles + a darker core. */
    private void addForestFlower(double cx, double cy, Random rng) {
        Color[] palette = {
            Color.rgb(255, 200, 100, 0.85),
            Color.rgb(220, 120, 200, 0.85),
            Color.rgb(255, 240, 240, 0.85)
        };
        Color petal = palette[rng.nextInt(palette.length)];
        double pr = 1.0 + rng.nextDouble() * 0.4;
        Circle pN = new Circle(cx,            cy - pr * 1.2, pr, petal);
        Circle pS = new Circle(cx,            cy + pr * 1.2, pr, petal);
        Circle pE = new Circle(cx + pr * 1.2, cy,            pr, petal);
        Circle pW = new Circle(cx - pr * 1.2, cy,            pr, petal);
        Circle core = new Circle(cx, cy, pr * 0.55, Color.rgb(200, 150, 60, 0.9));
        decorationLayer.getChildren().addAll(pN, pS, pE, pW, core);
    }

    /** 3-segment fern frond using a Polyline. */
    private void addFern(double cx, double cy, Color color, Random rng) {
        double lean = (rng.nextDouble() - 0.5) * 2;
        Polyline frond = new Polyline(
            cx,         cy + 3,
            cx + lean,  cy,
            cx + lean * 0.5, cy - 3,
            cx,         cy - 6
        );
        frond.setStroke(color);
        frond.setStrokeWidth(0.9);
        frond.setFill(null);
        decorationLayer.getChildren().add(frond);
    }

    /** Tiny brown acorn-shell speck. */
    private void addAcornShell(double cx, double cy) {
        Ellipse shell = new Ellipse(cx, cy, 1.6, 1.0);
        shell.setFill(Color.rgb(120, 80, 40, 0.75));
        shell.setStroke(Color.rgb(80, 50, 20, 0.9));
        shell.setStrokeWidth(0.4);
        decorationLayer.getChildren().add(shell);
    }

    // --- SWAMP ---------------------------------------------------------------

    /** Swamp: 2-segment reed, lily pad, bubble cluster, or algae cluster. */
    private void addSwampDecoration(double cx, double cy, Color color, Random rng) {
        double pick = rng.nextDouble();
        if (pick < 0.40) {
            addReed(cx, cy, color, rng);
        } else if (pick < 0.65) {
            addLilyPad(cx, cy);
        } else if (pick < 0.85) {
            addBubbleCluster(cx, cy, rng);
        } else {
            addAlgaeCluster(cx, cy, color, rng);
        }
    }

    /** 2-segment reed with a small tip ellipse. */
    private void addReed(double cx, double cy, Color color, Random rng) {
        double lean = (rng.nextDouble() - 0.5) * 2;
        Line lower = new Line(cx, cy + 3, cx + lean * 0.4, cy - 1);
        Line upper = new Line(cx + lean * 0.4, cy - 1, cx + lean, cy - 6);
        for (Line l : new Line[]{lower, upper}) {
            l.setStroke(color);
            l.setStrokeWidth(0.9);
            decorationLayer.getChildren().add(l);
        }
        Ellipse tip = new Ellipse(cx + lean, cy - 7, 1.0, 1.6);
        tip.setFill(color);
        decorationLayer.getChildren().add(tip);
    }

    /** Small lily pad: green ellipse with a notch line. */
    private void addLilyPad(double cx, double cy) {
        Ellipse pad = new Ellipse(cx, cy, 4, 2.6);
        pad.setFill(Color.rgb(60, 130, 60, 0.75));
        pad.setStroke(Color.rgb(30, 90, 30, 0.85));
        pad.setStrokeWidth(0.5);
        Line notch = new Line(cx, cy, cx + 4, cy);
        notch.setStroke(Color.rgb(30, 90, 30, 0.85));
        notch.setStrokeWidth(0.5);
        decorationLayer.getChildren().addAll(pad, notch);
    }

    /** A few tiny bubble circles. */
    private void addBubbleCluster(double cx, double cy, Random rng) {
        Color bubbleColor = Color.rgb(200, 220, 200, 0.6);
        for (int i = 0; i < 3; i++) {
            double bx = cx + (rng.nextDouble() - 0.5) * 5;
            double by = cy + (rng.nextDouble() - 0.5) * 4;
            double br = 0.8 + rng.nextDouble() * 0.6;
            Circle b = new Circle(bx, by, br, bubbleColor);
            decorationLayer.getChildren().add(b);
        }
    }

    /** 3 small algae specks in a tight cluster. */
    private void addAlgaeCluster(double cx, double cy, Color color, Random rng) {
        for (int i = 0; i < 3; i++) {
            double ax = cx + (rng.nextDouble() - 0.5) * 4;
            double ay = cy + (rng.nextDouble() - 0.5) * 3;
            Ellipse algae = new Ellipse(ax, ay, 1.6 + rng.nextDouble() * 0.6, 0.9);
            algae.setFill(color);
            decorationLayer.getChildren().add(algae);
        }
    }

    // --- FARM ----------------------------------------------------------------

    /** Farm: dirt patch w/ pebbles, wheat stalk, or cart tracks. */
    private void addFarmDecoration(double cx, double cy, Color color, Random rng) {
        double pick = rng.nextDouble();
        if (pick < 0.45) {
            addDirtPatchWithPebbles(cx, cy, color, rng);
        } else if (pick < 0.78) {
            addWheatStalk(cx, cy, rng);
        } else {
            addCartTracks(cx, cy, rng);
        }
    }

    /** A dirt patch ellipse surrounded by 3 small pebbles. */
    private void addDirtPatchWithPebbles(double cx, double cy, Color color, Random rng) {
        Ellipse patch = new Ellipse(cx, cy, 4 + rng.nextDouble() * 2, 2 + rng.nextDouble());
        patch.setFill(color);
        decorationLayer.getChildren().add(patch);
        Color pebbleColor = Color.rgb(150, 140, 120, 0.75);
        for (int i = 0; i < 3; i++) {
            double ang = Math.toRadians(120 * i + rng.nextDouble() * 30);
            double px = cx + Math.cos(ang) * 5;
            double py = cy + Math.sin(ang) * 3;
            Circle pebble = new Circle(px, py, 1.0 + rng.nextDouble() * 0.6, pebbleColor);
            decorationLayer.getChildren().add(pebble);
        }
    }

    /** Small wheat stalk: vertical line + 3 grain dots. */
    private void addWheatStalk(double cx, double cy, Random rng) {
        double lean = (rng.nextDouble() - 0.5) * 1.5;
        Line stalk = new Line(cx, cy + 4, cx + lean, cy - 5);
        stalk.setStroke(Color.rgb(180, 150, 80, 0.85));
        stalk.setStrokeWidth(0.8);
        decorationLayer.getChildren().add(stalk);
        Color grain = Color.rgb(220, 190, 100, 0.9);
        for (int i = 0; i < 3; i++) {
            double y = cy - 1 - i * 1.6;
            double offX = (i % 2 == 0) ? 1.2 : -1.2;
            Circle dot = new Circle(cx + offX + lean * 0.5, y, 0.9, grain);
            decorationLayer.getChildren().add(dot);
        }
    }

    /** Two short parallel cart-track lines. */
    private void addCartTracks(double cx, double cy, Random rng) {
        double angle = rng.nextDouble() * Math.PI;
        double len = 8;
        double dx = Math.cos(angle) * len / 2;
        double dy = Math.sin(angle) * len / 2;
        // Perpendicular offset for the second track
        double px = -Math.sin(angle) * 2.5;
        double py = Math.cos(angle) * 2.5;
        Line t1 = new Line(cx - dx + px, cy - dy + py, cx + dx + px, cy + dy + py);
        Line t2 = new Line(cx - dx - px, cy - dy - py, cx + dx - px, cy + dy - py);
        for (Line t : new Line[]{t1, t2}) {
            t.setStroke(Color.rgb(110, 80, 50, 0.5));
            t.setStrokeWidth(1.0);
            decorationLayer.getChildren().add(t);
        }
    }

    // -------------------------------------------------------------------------
    // Color utilities
    // -------------------------------------------------------------------------

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
