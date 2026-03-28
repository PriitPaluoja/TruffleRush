package com.example.demo.render;

import com.example.demo.entity.Pig;
import com.example.demo.item.Item;
import com.example.demo.item.ItemType;
import com.example.demo.world.GameMap;
import com.example.demo.world.Obstacle;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Renders all visible (uncollected) items onto a shared JavaFX {@link Group}.
 *
 * <p>Each item is drawn as a JavaFX primitive shape sized to fit comfortably
 * inside a {@value GameMap#TILE_SIZE}-pixel tile.  Item shapes are roughly
 * 12–18 px in their largest dimension and are centred within their tile.
 *
 * <p>Usage:
 * <pre>
 *   ItemRenderer renderer = new ItemRenderer();
 *   sceneRoot.getChildren().add(renderer.getGroup());
 *
 *   // In the game loop:
 *   renderer.update(spawner.getItems());
 * </pre>
 */
public class ItemRenderer {

    private static final int TILE = GameMap.TILE_SIZE; // 40 px

    private final Group group = new Group();

    /** Returns the parent group that should be added to the scene graph. */
    public Group getGroup() {
        return group;
    }

    /**
     * Clears the group and redraws visible uncollected items at their grid positions.
     *
     * <p>Items on BUSH cells are hidden unless at least one pig is adjacent
     * (Manhattan distance &lt;= 1), OR the player's sniff is active and the item
     * is within 2 cells of the player.
     *
     * @param items       the full item list from {@code ItemSpawner.getItems()}
     * @param map         the game map used to check cell obstacles
     * @param pigs        all pigs whose proximity reveals bush-hidden items
     * @param sniffActive whether the player's sniff ability is currently active
     * @param sniffCol    player column (used for sniff reveal radius)
     * @param sniffRow    player row (used for sniff reveal radius)
     */
    public void update(List<Item> items, GameMap map, List<Pig> pigs,
                       boolean sniffActive, int sniffCol, int sniffRow) {
        group.getChildren().clear();
        for (Item item : items) {
            if (item.isCollected()) continue;

            int itemCol = item.getCol();
            int itemRow = item.getRow();

            // Check whether this item sits on a BUSH cell
            Obstacle obs = map.getCell(itemCol, itemRow).getObstacle();
            if (obs == Obstacle.BUSH) {
                // Reveal if any pig is adjacent (distance <= 1)
                boolean revealed = false;
                for (Pig pig : pigs) {
                    int dist = Math.abs(pig.getCol() - itemCol) + Math.abs(pig.getRow() - itemRow);
                    if (dist <= 1) { revealed = true; break; }
                }
                // Also reveal if player sniff is active and item is within 2 cells
                if (!revealed && sniffActive) {
                    int sniffDist = Math.abs(sniffCol - itemCol) + Math.abs(sniffRow - itemRow);
                    if (sniffDist <= 2) revealed = true;
                }
                if (!revealed) continue;
            }

            Group shape = buildShape(item.getType());
            // Translate so the shape centre lands at the tile centre
            double cx = itemCol * TILE + TILE / 2.0;
            double cy = itemRow * TILE + TILE / 2.0;
            shape.setTranslateX(cx);
            shape.setTranslateY(cy);
            group.getChildren().add(shape);
        }
    }

    // -------------------------------------------------------------------------
    // Shape builders — one per ItemType
    // -------------------------------------------------------------------------

    /**
     * Builds and returns the JavaFX shape group for the given item type.
     * All shapes are centred at (0, 0); the caller applies the translation.
     */
    private static Group buildShape(ItemType type) {
        switch (type) {
            case BLACK_TRUFFLE:   return buildBlackTruffle();
            case WHITE_TRUFFLE:   return buildWhiteTruffle();
            case COMMON_MUSHROOM: return buildCommonMushroom();
            case ACORN:           return buildAcorn();
            case CELERY:          return buildCelery();
            case DIET_PILL:       return buildDietPill();
            case MUD_SPLASH:      return buildMudSplash();
            case GOLDEN_TRUFFLE:  return buildGoldenTruffle();
            default:
                // Fallback: generic small grey circle
                Circle fallback = new Circle(0, 0, 6, Color.GREY);
                return new Group(fallback);
        }
    }

    // ------------------------------------------------------------------
    // BLACK_TRUFFLE — 6-sided polygon, dark brown
    // ------------------------------------------------------------------
    private static Group buildBlackTruffle() {
        Polygon hex = regularHexagon(9, Color.rgb(80, 50, 20));
        hex.setStroke(Color.rgb(50, 30, 10));
        hex.setStrokeWidth(1.0);
        return new Group(hex);
    }

    // ------------------------------------------------------------------
    // WHITE_TRUFFLE — 6-sided polygon, white/cream
    // ------------------------------------------------------------------
    private static Group buildWhiteTruffle() {
        Polygon hex = regularHexagon(9, Color.rgb(240, 235, 210));
        hex.setStroke(Color.rgb(180, 170, 140));
        hex.setStrokeWidth(1.0);
        return new Group(hex);
    }

    // ------------------------------------------------------------------
    // COMMON_MUSHROOM — tan circle (cap) + small brown stem rectangle
    // ------------------------------------------------------------------
    private static Group buildCommonMushroom() {
        // Cap
        Circle cap = new Circle(0, -3, 8, Color.rgb(210, 180, 140));
        cap.setStroke(Color.rgb(160, 120, 80));
        cap.setStrokeWidth(1.0);

        // Stem — centred below the cap
        Rectangle stem = new Rectangle(-3, 3, 6, 6);
        stem.setFill(Color.rgb(230, 200, 160));
        stem.setStroke(Color.rgb(160, 120, 80));
        stem.setStrokeWidth(0.8);

        return new Group(stem, cap);
    }

    // ------------------------------------------------------------------
    // ACORN — small brown circle with a tiny dark cap
    // ------------------------------------------------------------------
    private static Group buildAcorn() {
        // Nut
        Circle nut = new Circle(0, 2, 6, Color.rgb(139, 90, 43));
        nut.setStroke(Color.rgb(100, 60, 20));
        nut.setStrokeWidth(0.8);

        // Cap
        Ellipse cap = new Ellipse(0, -3, 5, 3);
        cap.setFill(Color.rgb(80, 50, 20));
        cap.setStroke(Color.rgb(50, 30, 10));
        cap.setStrokeWidth(0.8);

        return new Group(nut, cap);
    }

    // ------------------------------------------------------------------
    // CELERY — thin tall rectangle, green
    // ------------------------------------------------------------------
    private static Group buildCelery() {
        Rectangle stalk = new Rectangle(-4, -9, 8, 18);
        stalk.setFill(Color.rgb(60, 179, 60));
        stalk.setStroke(Color.rgb(30, 120, 30));
        stalk.setStrokeWidth(1.0);

        // Small leaf bump at top
        Ellipse leaf = new Ellipse(0, -10, 5, 3);
        leaf.setFill(Color.rgb(50, 160, 50));

        return new Group(stalk, leaf);
    }

    // ------------------------------------------------------------------
    // DIET_PILL — small rectangle, white with a red cross
    // ------------------------------------------------------------------
    private static Group buildDietPill() {
        // Pill body
        Rectangle body = new Rectangle(-7, -4, 14, 8);
        body.setFill(Color.WHITE);
        body.setStroke(Color.rgb(200, 200, 200));
        body.setStrokeWidth(0.8);
        body.setArcWidth(6);
        body.setArcHeight(6);

        // Red cross — horizontal bar
        Rectangle crossH = new Rectangle(-4, -1.5, 8, 3);
        crossH.setFill(Color.rgb(220, 30, 30));

        // Red cross — vertical bar
        Rectangle crossV = new Rectangle(-1.5, -4, 3, 8);
        crossV.setFill(Color.rgb(220, 30, 30));

        return new Group(body, crossH, crossV);
    }

    // ------------------------------------------------------------------
    // MUD_SPLASH — semi-transparent muddy brown ellipse
    // ------------------------------------------------------------------
    private static Group buildMudSplash() {
        Ellipse mud = new Ellipse(0, 0, 11, 7);
        mud.setFill(Color.rgb(101, 67, 33, 0.65));
        mud.setStroke(Color.rgb(70, 40, 10, 0.8));
        mud.setStrokeWidth(1.0);

        // Two small splat dots
        Circle dot1 = new Circle(-8, -4, 2.5, Color.rgb(101, 67, 33, 0.55));
        Circle dot2 = new Circle( 9,  3, 2.0, Color.rgb(101, 67, 33, 0.50));

        return new Group(mud, dot1, dot2);
    }

    // ------------------------------------------------------------------
    // GOLDEN_TRUFFLE — 6-sided polygon, gold fill
    // ------------------------------------------------------------------
    private static Group buildGoldenTruffle() {
        Polygon hex = regularHexagon(11, Color.rgb(255, 200, 0));
        hex.setStroke(Color.rgb(200, 140, 0));
        hex.setStrokeWidth(1.5);
        return new Group(hex);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Creates a regular hexagon centred at (0, 0) with the given radius and fill.
     *
     * <p>The hexagon is flat-top oriented: the first vertex is at angle 0° (right).
     *
     * @param radius pixel distance from centre to each vertex
     * @param fill   fill colour
     * @return a configured {@link Polygon}
     */
    private static Polygon regularHexagon(double radius, Color fill) {
        Polygon hex = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60.0 * i); // flat-top: start at 0°
            hex.getPoints().add(radius * Math.cos(angle));
            hex.getPoints().add(radius * Math.sin(angle));
        }
        hex.setFill(fill);
        return hex;
    }
}
