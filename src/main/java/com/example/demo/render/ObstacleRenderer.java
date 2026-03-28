package com.example.demo.render;

import com.example.demo.world.Cell;
import com.example.demo.world.GameMap;
import com.example.demo.world.Obstacle;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
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
 * <p>Usage:
 * <pre>{@code
 *   ObstacleRenderer renderer = new ObstacleRenderer();
 *   renderer.render(map);
 *   root.getChildren().add(renderer.getGroup());
 * }</pre>
 */
public class ObstacleRenderer {

    private static final int TILE = GameMap.TILE_SIZE; // 40

    private final Group group = new Group();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link Group} that contains all rendered obstacle shapes.
     * Add this group to the scene graph once and call {@link #render(GameMap)}
     * whenever the map layout changes.
     *
     * @return the parent group for all obstacle shapes
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Iterates all cells in {@code map} and builds JavaFX shapes for every
     * cell that has an obstacle.  Existing shapes in the group are replaced.
     *
     * @param map the game map to render
     */
    public void render(GameMap map) {
        group.getChildren().clear();

        for (int col = 0; col < map.getColumns(); col++) {
            for (int row = 0; row < map.getRows(); row++) {
                Cell cell = map.getCell(col, row);
                Obstacle obs = cell.getObstacle();
                if (obs == null) continue;

                double x = col * TILE;
                double y = row * TILE;

                Group shape = createShape(obs, x, y, col, row);
                group.getChildren().add(shape);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shape factories
    // -------------------------------------------------------------------------

    /** Dispatches to the correct factory method for the given obstacle type. */
    private Group createShape(Obstacle obs, double x, double y, int col, int row) {
        switch (obs) {
            case ROCK:    return createRock(x, y, col, row);
            case BUSH:    return createBush(x, y, col, row);
            case MUD_PIT: return createMudPit(x, y);
            case FENCE:   return createFence(x, y, col, row);
            default:      return new Group();
        }
    }

    // --- ROCK -----------------------------------------------------------------

    /**
     * Irregular 5-vertex polygon in grey tones.  Vertices are offset by small
     * amounts derived from the cell coordinates to give each rock a unique look.
     */
    private Group createRock(double x, double y, int col, int row) {
        // Deterministic per-cell pseudo-random offsets based on position
        double[] jitter = jitter(col, row, 5);

        double cx = x + TILE / 2.0;
        double cy = y + TILE / 2.0;
        double r1 = 14;
        double r2 = 11;

        // 5-vertex polygon alternating between two radii, with a small jitter
        Double[] points = new Double[10];
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-90 + i * 72);
            double radius = (i % 2 == 0) ? r1 : r2;
            radius += jitter[i] * 4 - 2;   // ±2 px jitter
            points[i * 2]     = cx + Math.cos(angle) * radius;
            points[i * 2 + 1] = cy + Math.sin(angle) * radius;
        }

        Polygon rock = new Polygon();
        rock.getPoints().addAll(points);
        rock.setFill(Color.rgb(130, 130, 135));
        rock.setStroke(Color.rgb(80, 80, 85));
        rock.setStrokeWidth(1.5);

        return new Group(rock);
    }

    // --- BUSH -----------------------------------------------------------------

    /**
     * Three overlapping circles in shades of green,
     * arranged in a small triangle formation.
     */
    private Group createBush(double x, double y, int col, int row) {
        double cx = x + TILE / 2.0;
        double cy = y + TILE / 2.0;

        double[] jitter = jitter(col, row, 3);

        Circle c1 = circle(cx - 6 + jitter[0] * 4, cy + 4,  9, Color.rgb(34, 120, 34, 0.90));
        Circle c2 = circle(cx + 6 + jitter[1] * 3, cy + 4,  8, Color.rgb(45, 140, 45, 0.85));
        Circle c3 = circle(cx     + jitter[2] * 3, cy - 5,  8, Color.rgb(60, 160, 60, 0.80));

        strokeCircle(c1, Color.rgb(20, 80, 20), 1.0);
        strokeCircle(c2, Color.rgb(20, 80, 20), 1.0);
        strokeCircle(c3, Color.rgb(20, 80, 20), 1.0);

        return new Group(c1, c2, c3);
    }

    // --- MUD PIT --------------------------------------------------------------

    /** Semi-transparent brown ellipse centred in the tile. */
    private Group createMudPit(double x, double y) {
        double cx = x + TILE / 2.0;
        double cy = y + TILE / 2.0;

        Ellipse mud = new Ellipse(cx, cy, 16, 11);
        mud.setFill(Color.rgb(101, 67, 33, 0.55));   // dark brown, ~55 % opacity
        mud.setStroke(Color.rgb(70, 40, 10, 0.70));
        mud.setStrokeWidth(1.5);

        return new Group(mud);
    }

    // --- FENCE ----------------------------------------------------------------

    /**
     * A thin dark-wood rectangle.  Oriented vertically (4×36) to represent a
     * fence post; the post is centred in the tile.
     */
    private Group createFence(double x, double y, int col, int row) {
        // Thin vertical post centred in tile
        double postW = 4;
        double postH = 36;
        double px = x + (TILE - postW) / 2.0;
        double py = y + (TILE - postH) / 2.0;

        Rectangle post = new Rectangle(px, py, postW, postH);
        post.setFill(Color.rgb(92, 51, 23));     // dark wood brown
        post.setStroke(Color.rgb(50, 25, 5));
        post.setStrokeWidth(1.0);

        // Horizontal rail across the middle to suggest a fence panel
        double railW = 32;
        double railH = 4;
        double rx = x + (TILE - railW) / 2.0;
        double ry = y + (TILE - railH) / 2.0;

        Rectangle rail = new Rectangle(rx, ry, railW, railH);
        rail.setFill(Color.rgb(110, 65, 30));
        rail.setStroke(Color.rgb(50, 25, 5));
        rail.setStrokeWidth(1.0);

        return new Group(post, rail);
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
     * [0, 1) derived from the cell coordinates.  Using the coordinates as the
     * seed ensures the same cell always gets the same visual offsets across
     * renders.
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
