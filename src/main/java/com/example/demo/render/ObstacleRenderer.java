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
 *   <li><b>ROCK</b>     – irregular grey polygon (5 vertices), slightly randomised</li>
 *   <li><b>BUSH</b>     – cluster of 3 overlapping green circles</li>
 *   <li><b>MUD_PIT</b>  – semi-transparent brown ellipse</li>
 *   <li><b>FENCE</b>    – narrow dark-wood rectangle oriented to fill the tile</li>
 * </ul>
 *
 * <p>Biome accents (berries, moss, mud bubbles, etc.) are drawn on top of the
 * base shapes using {@link Biome#bushAccent()} / {@link Biome#rockHighlight()} /
 * {@link Biome#mudAccent()} so each level reads a little differently.
 */
public class ObstacleRenderer {

    private static final int TILE = GameMap.TILE_SIZE; // 40

    private final Group group = new Group();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public Group getGroup() {
        return group;
    }

    /**
     * Iterates all cells in {@code map} and builds JavaFX shapes for every
     * cell that has an obstacle.  Existing shapes in the group are replaced.
     *
     * @param map   the game map to render
     * @param biome the active biome (used for accent colors)
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

    // -------------------------------------------------------------------------
    // Shape factories
    // -------------------------------------------------------------------------

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
        double[] jitter = jitter(col, row, 8);

        double cx = x + TILE / 2.0;
        double cy = y + TILE / 2.0;
        double r1 = 14;
        double r2 = 11;

        // Base 5-vertex polygon
        Double[] points = new Double[10];
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-90 + i * 72);
            double radius = (i % 2 == 0) ? r1 : r2;
            radius += jitter[i] * 4 - 2;
            points[i * 2]     = cx + Math.cos(angle) * radius;
            points[i * 2 + 1] = cy + Math.sin(angle) * radius;
        }

        Polygon rock = new Polygon();
        rock.getPoints().addAll(points);
        rock.setFill(Color.rgb(130, 130, 135));
        rock.setStroke(Color.rgb(80, 80, 85));
        rock.setStrokeWidth(1.5);

        // Soft shadow under the rock so it doesn't float
        Ellipse shadow = new Ellipse(cx, cy + 11, 14, 3);
        shadow.setFill(Color.rgb(0, 0, 0, 0.18));

        // Top-lit facet — small triangle in the upper-left half
        Polygon facet = new Polygon(
            cx - 6, cy - 3,
            cx + 2, cy - 9,
            cx + 4, cy - 1
        );
        facet.setFill(biome.rockHighlight());

        Group g = new Group(shadow, rock, facet);

        // 1–2 short cracks
        double crackAngle = Math.toRadians(jitter[5] * 360);
        double clen = 4 + jitter[6] * 3;
        Line crack = new Line(
            cx, cy,
            cx + Math.cos(crackAngle) * clen,
            cy + Math.sin(crackAngle) * clen
        );
        crack.setStroke(Color.rgb(60, 60, 65, 0.85));
        crack.setStrokeWidth(0.8);
        g.getChildren().add(crack);

        // Forest rocks get a small moss patch on the lower edge
        if (biome == Biome.FOREST) {
            Ellipse moss = new Ellipse(cx - 3 + jitter[7] * 6, cy + 7, 4, 2);
            moss.setFill(Color.rgb(60, 110, 50, 0.85));
            g.getChildren().add(moss);
        }

        return g;
    }

    // --- BUSH -----------------------------------------------------------------

    private Group createBush(double x, double y, int col, int row, Biome biome) {
        double cx = x + TILE / 2.0;
        double cy = y + TILE / 2.0;

        double[] jitter = jitter(col, row, 10);

        Circle c1 = circle(cx - 6 + jitter[0] * 4, cy + 4,  9, Color.rgb(34, 120, 34, 0.90));
        Circle c2 = circle(cx + 6 + jitter[1] * 3, cy + 4,  8, Color.rgb(45, 140, 45, 0.85));
        Circle c3 = circle(cx     + jitter[2] * 3, cy - 5,  8, Color.rgb(60, 160, 60, 0.80));

        strokeCircle(c1, Color.rgb(20, 80, 20), 1.0);
        strokeCircle(c2, Color.rgb(20, 80, 20), 1.0);
        strokeCircle(c3, Color.rgb(20, 80, 20), 1.0);

        Group g = new Group(c1, c2, c3);

        // Off-center highlight on the top circle for a 3D feel
        Ellipse highlight = new Ellipse(cx - 2, cy - 8, 2.5, 1.2);
        highlight.setFill(Color.rgb(255, 255, 255, 0.35));
        g.getChildren().add(highlight);

        // 2–4 berries scattered through the bush
        Color berry = biome.bushAccent();
        int berryCount = 2 + (int) (jitter[3] * 3); // 2..4
        for (int i = 0; i < berryCount; i++) {
            double bx = cx + (jitter[4 + i] - 0.5) * 16;
            double by = cy + (jitter[(7 + i) % 10] - 0.5) * 14;
            Circle berryDot = new Circle(bx, by, 1.5, berry);
            berryDot.setStroke(berry.darker());
            berryDot.setStrokeWidth(0.4);
            g.getChildren().add(berryDot);
        }

        // Thin stem coming out of the base
        Line stem = new Line(cx + 1, cy + 12, cx + 1, cy + 6);
        stem.setStroke(Color.rgb(60, 40, 20, 0.8));
        stem.setStrokeWidth(1.0);
        g.getChildren().add(0, stem); // behind the leaves

        return g;
    }

    // --- MUD PIT --------------------------------------------------------------

    private Group createMudPit(double x, double y, int col, int row, Biome biome) {
        double cx = x + TILE / 2.0;
        double cy = y + TILE / 2.0;

        double[] jitter = jitter(col, row, 6);

        Ellipse mud = new Ellipse(cx, cy, 16, 11);
        mud.setFill(Color.rgb(101, 67, 33, 0.55));
        mud.setStroke(Color.rgb(70, 40, 10, 0.70));
        mud.setStrokeWidth(1.5);

        Group g = new Group(mud);

        // Curved ripple arc inside the puddle
        Arc ripple = new Arc(cx, cy, 9, 5, 200, 140);
        ripple.setType(ArcType.OPEN);
        ripple.setFill(null);
        ripple.setStroke(Color.rgb(60, 35, 10, 0.55));
        ripple.setStrokeWidth(1.0);
        g.getChildren().add(ripple);

        // 2–3 bubbles biome-tinted
        Color accent = biome.mudAccent();
        int bubbles = 2 + (int) (jitter[0] * 2); // 2..3
        for (int i = 0; i < bubbles; i++) {
            double bx = cx + (jitter[1 + i] - 0.5) * 18;
            double by = cy + (jitter[(3 + i) % 6] - 0.5) * 12;
            double br = 1.0 + jitter[(4 + i) % 6] * 1.2;
            Circle bubble = new Circle(bx, by, br, accent);
            g.getChildren().add(bubble);
        }

        // Swamp gets a thin reed sticking up out of the top edge
        if (biome == Biome.SWAMP) {
            Line reed = new Line(cx - 4, cy - 5, cx - 4, cy - 14);
            reed.setStroke(Color.rgb(50, 110, 50, 0.9));
            reed.setStrokeWidth(1.0);
            g.getChildren().add(reed);
        }

        return g;
    }

    // --- FENCE ----------------------------------------------------------------

    private Group createFence(double x, double y, int col, int row) {
        double[] jitter = jitter(col, row, 4);

        // Vertical post
        double postW = 4;
        double postH = 36;
        double px = x + (TILE - postW) / 2.0;
        double py = y + (TILE - postH) / 2.0;

        Rectangle post = new Rectangle(px, py, postW, postH);
        post.setFill(Color.rgb(92, 51, 23));
        post.setStroke(Color.rgb(50, 25, 5));
        post.setStrokeWidth(1.0);

        // Horizontal rail
        double railW = 32;
        double railH = 4;
        double rx = x + (TILE - railW) / 2.0;
        double ry = y + (TILE - railH) / 2.0;

        Rectangle rail = new Rectangle(rx, ry, railW, railH);
        rail.setFill(Color.rgb(110, 65, 30));
        rail.setStroke(Color.rgb(50, 25, 5));
        rail.setStrokeWidth(1.0);

        Group g = new Group(post, rail);

        // Wood grain on post (one thin line)
        Line postGrain = new Line(px + 1, py + 4, px + 1, py + postH - 4);
        postGrain.setStroke(Color.rgb(50, 25, 5, 0.55));
        postGrain.setStrokeWidth(0.6);
        g.getChildren().add(postGrain);

        // Wood grain on rail
        Line railGrain = new Line(rx + 3, ry + 1, rx + railW - 3, ry + 1);
        railGrain.setStroke(Color.rgb(50, 25, 5, 0.45));
        railGrain.setStrokeWidth(0.6);
        g.getChildren().add(railGrain);

        // Knot on the rail at a deterministic offset
        double knotX = rx + 4 + jitter[0] * (railW - 8);
        Circle knot = new Circle(knotX, ry + railH / 2.0, 1.0, Color.rgb(60, 30, 5));
        g.getChildren().add(knot);

        // Triangular post-cap
        Polygon cap = new Polygon(
            px - 1, py + 1,
            px + postW + 1, py + 1,
            px + postW / 2.0, py - 3
        );
        cap.setFill(Color.rgb(70, 40, 15));
        cap.setStroke(Color.rgb(40, 20, 5));
        cap.setStrokeWidth(0.8);
        g.getChildren().add(cap);

        return g;
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private Circle circle(double cx, double cy, double radius, Color fill) {
        Circle c = new Circle(cx, cy, radius);
        c.setFill(fill);
        return c;
    }

    private void strokeCircle(Circle c, Color stroke, double width) {
        c.setStroke(stroke);
        c.setStrokeWidth(width);
    }

    /**
     * Returns an array of {@code count} deterministic pseudo-random doubles in
     * [0, 1) derived from the cell coordinates.
     */
    private double[] jitter(int col, int row, int count) {
        java.util.Random rng = new java.util.Random((long) col * 31 + row);
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            result[i] = rng.nextDouble();
        }
        return result;
    }
}
