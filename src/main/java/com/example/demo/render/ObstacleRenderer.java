package com.example.demo.render;

import com.example.demo.world.Biome;
import com.example.demo.world.Cell;
import com.example.demo.world.GameMap;
import com.example.demo.world.Obstacle;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * Renders obstacle shapes for every {@link Cell} in a {@link GameMap} that
 * carries an {@link Obstacle}.
 *
 * <p>Shape conventions (all fit within the 40×40 tile):
 * <ul>
 *   <li><b>ROCK</b>     – jagged 8-vertex polygon with multiple facets, cracks,
 *       mineral specks, and chip pebbles around the base</li>
 *   <li><b>BUSH</b>     – layered cluster of 6 leaf circles, foliage highlights,
 *       5–7 berries, thorny stem, and frondy leaf outlines</li>
 *   <li><b>MUD_PIT</b>  – outer ripple ring + 2-tone mud + 5–6 bubbles, splatter
 *       circles, and biome-specific accents (lily pads / reeds)</li>
 *   <li><b>FENCE</b>    – two-post crisscross with wood grain, nail heads,
 *       triangular caps, and weathering streaks</li>
 * </ul>
 *
 * <p>Per-cell randomness is seeded by {@code (col, row, biome.ordinal())} so
 * daily seeded runs reproduce identical obstacle visuals.
 */
public class ObstacleRenderer {

    private static final int TILE = GameMap.TILE_SIZE; // 40

    private final Group group = new Group();

    public Group getGroup() {
        return group;
    }

    /**
     * Iterates all cells in {@code map} and builds JavaFX shapes for every
     * cell that has an obstacle. Existing shapes in the group are replaced.
     * Called once per level (not per frame) by the game loop.
     */
    public void render(GameMap map, Biome biome) {
        group.getChildren().clear();

        for (int col = 0; col < map.getColumns(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Cell cell = map.getCell(col, row);
                Obstacle obs = cell.getObstacle();
                if (obs == null) continue;

                double x = col * TILE;
                double y = row * TILE;

                Group shape = createShape(obs, x, y, col, row, biome);
                group.getChildren().add(shape);
            }
        }
    }

    private Group createShape(Obstacle obs, double x, double y, int col, int row, Biome biome) {
        switch (obs) {
            case ROCK:    return createRock(x, y, col, row, biome);
            case BUSH:    return createBush(x, y, col, row, biome);
            case MUD_PIT: return createMudPit(x, y, col, row, biome);
            case FENCE:   return createFence(x, y, col, row);
            default:      return new Group();
        }
    }

    // --- ROCK -----------------------------------------------------------------

    private Group createRock(double x, double y, int col, int row, Biome biome) {
        double[] j = jitter(col, row, 16, biome);

        double cx = x + TILE / 2.0;
        double cy = y + TILE / 2.0;
        double r1 = 14;
        double r2 = 11;

        // Soft shadow under the rock
        Ellipse shadow = new Ellipse(cx, cy + 12, 15, 3.5);
        shadow.setFill(Color.rgb(0, 0, 0, 0.20));

        // Base 8-vertex jagged polygon
        Double[] points = new Double[16];
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(-90 + i * 45);
            double radius = (i % 2 == 0) ? r1 : r2;
            radius += j[i] * 4 - 2;
            points[i * 2]     = cx + Math.cos(angle) * radius;
            points[i * 2 + 1] = cy + Math.sin(angle) * radius;
        }

        Polygon rock = new Polygon();
        rock.getPoints().addAll(points);
        rock.setFill(Color.rgb(130, 130, 135));
        rock.setStroke(Color.rgb(80, 80, 85));
        rock.setStrokeWidth(1.5);

        // Two top-lit facet triangles
        Polygon facet1 = new Polygon(
            cx - 7, cy - 3,
            cx + 1, cy - 9,
            cx + 4, cy - 1
        );
        facet1.setFill(biome.rockHighlight());
        Polygon facet2 = new Polygon(
            cx - 4, cy + 1,
            cx + 2, cy - 4,
            cx + 5, cy + 3
        );
        facet2.setFill(biome.rockHighlight().deriveColor(0, 1, 0.85, 0.7));

        Group g = new Group(shadow, rock, facet1, facet2);

        // 3 short cracks at varied angles
        for (int c = 0; c < 3; c++) {
            double crackAngle = Math.toRadians(j[8 + c] * 360);
            double cstart = j[11 + c] * 3;
            double clen = 4 + j[(13 + c) % 16] * 4;
            Line crack = new Line(
                cx + Math.cos(crackAngle) * cstart,
                cy + Math.sin(crackAngle) * cstart,
                cx + Math.cos(crackAngle) * (cstart + clen),
                cy + Math.sin(crackAngle) * (cstart + clen)
            );
            crack.setStroke(Color.rgb(60, 60, 65, 0.85));
            crack.setStrokeWidth(0.7);
            g.getChildren().add(crack);
        }

        // 2 small pebble chips around the base
        Circle pebble1 = new Circle(cx - 13 + j[0] * 4, cy + 9, 1.6, Color.rgb(120, 120, 125));
        pebble1.setStroke(Color.rgb(80, 80, 85));
        pebble1.setStrokeWidth(0.4);
        Circle pebble2 = new Circle(cx + 11 + j[1] * 3, cy + 10, 1.3, Color.rgb(140, 140, 145));
        pebble2.setStroke(Color.rgb(80, 80, 85));
        pebble2.setStrokeWidth(0.4);
        g.getChildren().addAll(pebble1, pebble2);

        // 4 mineral speck dots scattered on the rock face
        Color speckColor = biome.rockHighlight().deriveColor(0, 1, 1.2, 1.0);
        for (int s = 0; s < 4; s++) {
            double sx = cx + (j[(2 + s) % 16] - 0.5) * 14;
            double sy = cy + (j[(6 + s) % 16] - 0.5) * 12;
            Circle speck = new Circle(sx, sy, 0.7, speckColor);
            g.getChildren().add(speck);
        }

        // Forest rocks get 2 moss patches
        if (biome == Biome.FOREST) {
            Ellipse moss1 = new Ellipse(cx - 4 + j[7] * 6, cy + 7, 4.5, 2.2);
            moss1.setFill(Color.rgb(60, 110, 50, 0.85));
            Ellipse moss2 = new Ellipse(cx + 5, cy + 4, 3, 1.5);
            moss2.setFill(Color.rgb(80, 130, 60, 0.75));
            g.getChildren().addAll(moss1, moss2);
        }

        return g;
    }

    // --- BUSH -----------------------------------------------------------------

    private Group createBush(double x, double y, int col, int row, Biome biome) {
        double cx = x + TILE / 2.0;
        double cy = y + TILE / 2.0;

        double[] j = jitter(col, row, 16, biome);

        // Stem at the base, behind the leaves
        Group g = new Group();
        Line stem = new Line(cx + 1, cy + 12, cx + 1, cy + 6);
        stem.setStroke(Color.rgb(60, 40, 20, 0.8));
        stem.setStrokeWidth(1.2);
        Line stemThorn = new Line(cx + 4, cy + 10, cx + 6, cy + 8);
        stemThorn.setStroke(Color.rgb(60, 40, 20, 0.7));
        stemThorn.setStrokeWidth(0.6);
        Line root = new Line(cx - 3, cy + 13, cx + 4, cy + 13);
        root.setStroke(Color.rgb(60, 40, 20, 0.6));
        root.setStrokeWidth(0.7);
        g.getChildren().addAll(stem, stemThorn, root);

        // 6 leaf circles in varied greens
        Color[] leaves = {
            Color.rgb(34, 120, 34, 0.92),
            Color.rgb(45, 140, 45, 0.88),
            Color.rgb(60, 160, 60, 0.85),
            Color.rgb(50, 150, 50, 0.85),
            Color.rgb(70, 170, 70, 0.80),
            Color.rgb(40, 130, 40, 0.90)
        };
        double[][] leafPos = {
            {cx - 6 + j[0] * 4, cy + 4,  9},
            {cx + 6 + j[1] * 3, cy + 4,  8},
            {cx     + j[2] * 3, cy - 5,  8},
            {cx - 9 + j[3] * 3, cy - 1,  6.5},
            {cx + 9 + j[4] * 3, cy - 1,  6.5},
            {cx     + j[5] * 2, cy + 1,  7}
        };
        for (int i = 0; i < 6; i++) {
            Circle c = new Circle(leafPos[i][0], leafPos[i][1], leafPos[i][2], leaves[i]);
            c.setStroke(Color.rgb(20, 80, 20));
            c.setStrokeWidth(0.9);
            g.getChildren().add(c);
        }

        // 2 highlight ellipses for a 3D feel
        Ellipse hi1 = new Ellipse(cx - 2, cy - 8, 2.5, 1.2);
        hi1.setFill(Color.rgb(255, 255, 255, 0.35));
        Ellipse hi2 = new Ellipse(cx + 4, cy - 4, 1.8, 1.0);
        hi2.setFill(Color.rgb(255, 255, 255, 0.25));
        g.getChildren().addAll(hi1, hi2);

        // 3 frondy leaf-outline polygons (small triangular leaves around the edges)
        Polygon frond1 = new Polygon(cx - 11, cy + 2, cx - 8, cy - 2, cx - 7, cy + 3);
        Polygon frond2 = new Polygon(cx + 11, cy + 2, cx + 8, cy - 2, cx + 7, cy + 3);
        Polygon frond3 = new Polygon(cx, cy - 11, cx - 3, cy - 8, cx + 3, cy - 8);
        for (Polygon f : new Polygon[]{frond1, frond2, frond3}) {
            f.setFill(Color.rgb(50, 140, 50, 0.85));
            f.setStroke(Color.rgb(20, 80, 20));
            f.setStrokeWidth(0.5);
            g.getChildren().add(f);
        }

        // 5–7 berries scattered through the bush
        Color berry = biome.bushAccent();
        int berryCount = 5 + (int) (j[6] * 3); // 5..7
        for (int i = 0; i < berryCount; i++) {
            double bx = cx + (j[(7 + i) % 16] - 0.5) * 16;
            double by = cy + (j[(10 + i) % 16] - 0.5) * 14;
            Circle berryDot = new Circle(bx, by, 1.6, berry);
            berryDot.setStroke(berry.darker());
            berryDot.setStrokeWidth(0.4);
            // Tiny highlight on each berry
            Circle berryHi = new Circle(bx - 0.5, by - 0.5, 0.5, Color.rgb(255, 255, 255, 0.8));
            g.getChildren().addAll(berryDot, berryHi);
        }

        return g;
    }

    // --- MUD PIT --------------------------------------------------------------

    private Group createMudPit(double x, double y, int col, int row, Biome biome) {
        double cx = x + TILE / 2.0;
        double cy = y + TILE / 2.0;

        double[] j = jitter(col, row, 16, biome);

        // Outer ripple ring (transparent stroke)
        Ellipse ringOuter = new Ellipse(cx, cy, 18, 12);
        ringOuter.setFill(Color.TRANSPARENT);
        ringOuter.setStroke(Color.rgb(101, 67, 33, 0.30));
        ringOuter.setStrokeWidth(0.8);

        // Main mud puddle (outer + darker inner ellipse)
        Ellipse mud = new Ellipse(cx, cy, 16, 11);
        mud.setFill(Color.rgb(101, 67, 33, 0.55));
        mud.setStroke(Color.rgb(70, 40, 10, 0.70));
        mud.setStrokeWidth(1.5);
        Ellipse mudInner = new Ellipse(cx - 1, cy - 0.5, 11, 7);
        mudInner.setFill(Color.rgb(60, 35, 10, 0.55));

        Group g = new Group(ringOuter, mud, mudInner);

        // Curved ripple arc inside the puddle
        Arc ripple1 = new Arc(cx, cy, 9, 5, 200, 140);
        ripple1.setType(ArcType.OPEN);
        ripple1.setFill(null);
        ripple1.setStroke(Color.rgb(60, 35, 10, 0.55));
        ripple1.setStrokeWidth(1.0);
        Arc ripple2 = new Arc(cx, cy, 6, 3, 30, 120);
        ripple2.setType(ArcType.OPEN);
        ripple2.setFill(null);
        ripple2.setStroke(Color.rgb(80, 50, 20, 0.45));
        ripple2.setStrokeWidth(0.8);
        g.getChildren().addAll(ripple1, ripple2);

        // 5–6 bubbles biome-tinted, each with a small highlight
        Color accent = biome.mudAccent();
        int bubbles = 5 + (int) (j[0] * 2); // 5..6
        for (int i = 0; i < bubbles; i++) {
            double bx = cx + (j[(1 + i) % 16] - 0.5) * 18;
            double by = cy + (j[(7 + i) % 16] - 0.5) * 12;
            double br = 1.0 + j[(4 + i) % 16] * 1.5;
            Circle bubble = new Circle(bx, by, br, accent);
            Circle bubbleHi = new Circle(bx - br * 0.3, by - br * 0.3, br * 0.30, Color.rgb(255, 255, 255, 0.65));
            g.getChildren().addAll(bubble, bubbleHi);
        }

        // 6 splatter circles around the rim
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(60 * i + j[(2 + i) % 16] * 30);
            double dist = 14 + j[(8 + i) % 16] * 4;
            double sx = cx + Math.cos(ang) * dist;
            double sy = cy + Math.sin(ang) * (dist * 0.7);
            Circle splat = new Circle(sx, sy, 0.9 + j[(11 + i) % 16] * 0.6, Color.rgb(101, 67, 33, 0.55));
            g.getChildren().add(splat);
        }

        // Swamp gets 2 reeds + 1 lily pad
        if (biome == Biome.SWAMP) {
            Line reed1 = new Line(cx - 4, cy - 5, cx - 4, cy - 14);
            Line reed2 = new Line(cx + 6, cy - 4, cx + 6, cy - 12);
            for (Line r : new Line[]{reed1, reed2}) {
                r.setStroke(Color.rgb(50, 110, 50, 0.9));
                r.setStrokeWidth(1.0);
                g.getChildren().add(r);
            }
            Ellipse lily = new Ellipse(cx + 3, cy + 1, 4, 2.2);
            lily.setFill(Color.rgb(60, 130, 60, 0.85));
            lily.setStroke(Color.rgb(30, 90, 30));
            lily.setStrokeWidth(0.5);
            g.getChildren().add(lily);
        }

        return g;
    }

    // --- FENCE ----------------------------------------------------------------

    private Group createFence(double x, double y, int col, int row) {
        double[] j = jitter(col, row, 12, null);

        Color woodLight = Color.rgb(110, 65, 30);
        Color woodDark  = Color.rgb(50, 25, 5);

        // Two vertical posts spaced apart for a proper crisscross
        double postW = 4;
        double postH = 32;
        double postPadY = (TILE - postH) / 2.0;
        double postLX = x + 8;
        double postRX = x + TILE - 8 - postW;
        double postY  = y + postPadY;

        Rectangle postL = new Rectangle(postLX, postY, postW, postH);
        Rectangle postR = new Rectangle(postRX, postY, postW, postH);
        for (Rectangle p : new Rectangle[]{postL, postR}) {
            p.setFill(Color.rgb(92, 51, 23));
            p.setStroke(woodDark);
            p.setStrokeWidth(1.0);
        }

        // Two horizontal rails
        double railH = 4;
        double railW = TILE - 4;
        double railX = x + 2;
        double railY1 = y + 10;
        double railY2 = y + TILE - 10 - railH;

        Rectangle rail1 = new Rectangle(railX, railY1, railW, railH);
        Rectangle rail2 = new Rectangle(railX, railY2, railW, railH);
        for (Rectangle r : new Rectangle[]{rail1, rail2}) {
            r.setFill(woodLight);
            r.setStroke(woodDark);
            r.setStrokeWidth(1.0);
        }

        Group g = new Group(postL, postR, rail1, rail2);

        // Wood grain on each post and rail (4 lines total)
        Line postGrainL = new Line(postLX + 1, postY + 4, postLX + 1, postY + postH - 4);
        Line postGrainR = new Line(postRX + 1, postY + 4, postRX + 1, postY + postH - 4);
        for (Line l : new Line[]{postGrainL, postGrainR}) {
            l.setStroke(Color.rgb(50, 25, 5, 0.55));
            l.setStrokeWidth(0.6);
            g.getChildren().add(l);
        }
        Line railGrain1 = new Line(railX + 3, railY1 + 1, railX + railW - 3, railY1 + 1);
        Line railGrain2 = new Line(railX + 3, railY2 + 1, railX + railW - 3, railY2 + 1);
        for (Line l : new Line[]{railGrain1, railGrain2}) {
            l.setStroke(Color.rgb(50, 25, 5, 0.45));
            l.setStrokeWidth(0.6);
            g.getChildren().add(l);
        }

        // 3 nail-head circles where rails meet posts and at the rail center
        Circle nail1 = new Circle(postLX + postW / 2.0, railY1 + railH / 2.0, 0.9, Color.rgb(70, 70, 75));
        Circle nail2 = new Circle(postRX + postW / 2.0, railY1 + railH / 2.0, 0.9, Color.rgb(70, 70, 75));
        Circle nail3 = new Circle(postLX + postW / 2.0, railY2 + railH / 2.0, 0.9, Color.rgb(70, 70, 75));
        g.getChildren().addAll(nail1, nail2, nail3);

        // Knot on the upper rail at a deterministic offset
        double knotX = railX + 6 + j[0] * (railW - 12);
        Circle knot = new Circle(knotX, railY1 + railH / 2.0, 1.1, Color.rgb(60, 30, 5));
        g.getChildren().add(knot);

        // Two triangular post caps
        Polygon capL = new Polygon(
            postLX - 1, postY + 1,
            postLX + postW + 1, postY + 1,
            postLX + postW / 2.0, postY - 3
        );
        Polygon capR = new Polygon(
            postRX - 1, postY + 1,
            postRX + postW + 1, postY + 1,
            postRX + postW / 2.0, postY - 3
        );
        for (Polygon cap : new Polygon[]{capL, capR}) {
            cap.setFill(Color.rgb(70, 40, 15));
            cap.setStroke(Color.rgb(40, 20, 5));
            cap.setStrokeWidth(0.8);
            g.getChildren().add(cap);
        }

        // Weathering streak on each post
        Line weatherL = new Line(postLX + 2.5, postY + 8, postLX + 2.5, postY + postH - 6);
        Line weatherR = new Line(postRX + 2.5, postY + 8, postRX + 2.5, postY + postH - 6);
        for (Line w : new Line[]{weatherL, weatherR}) {
            w.setStroke(Color.rgb(30, 15, 5, 0.45));
            w.setStrokeWidth(0.5);
            g.getChildren().add(w);
        }

        return g;
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    /**
     * Returns an array of {@code count} deterministic pseudo-random doubles in
     * [0, 1) derived from the cell coordinates and (optionally) the biome.
     * Uses a 2-prime hash to avoid the seed collisions a plain {@code col*31+row}
     * pattern can produce at higher detail densities.
     */
    private double[] jitter(int col, int row, int count, Biome biome) {
        long seed = (long) col * 73856093L + (long) row * 19349663L;
        if (biome != null) seed += (long) biome.ordinal() * 83492791L;
        java.util.Random rng = new java.util.Random(seed);
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            result[i] = rng.nextDouble();
        }
        return result;
    }
}
