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
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Renders all visible (uncollected) items onto a shared JavaFX {@link Group}.
 *
 * <p>Each item is drawn as a JavaFX primitive shape sized to fit comfortably
 * inside a {@value GameMap#TILE_SIZE}-pixel tile.  Item shapes are roughly
 * 12–18 px in their largest dimension and are centred within their tile.
 *
 * <p>Flagship items (golden truffle, super acorn, magnet crown, greater speed)
 * bob gently using the {@code animTick} parameter passed to {@link #update}.
 */
public class ItemRenderer {

    private static final int TILE = GameMap.TILE_SIZE; // 40 px

    private final Group group = new Group();

    public Group getGroup() {
        return group;
    }

    /**
     * Clears the group and redraws visible uncollected items at their grid positions.
     *
     * <p>Items on BUSH cells are hidden unless at least one pig is adjacent
     * (Manhattan distance &lt;= 1), OR the player's sniff is active and the item
     * is within 2 cells of the player.
     */
    public void update(List<Item> items, GameMap map, List<Pig> pigs,
                       boolean sniffActive, int sniffCol, int sniffRow,
                       long animTick) {
        group.getChildren().clear();
        for (Item item : items) {
            if (item.isCollected()) continue;

            int itemCol = item.getCol();
            int itemRow = item.getRow();

            Obstacle obs = map.getCell(itemCol, itemRow).getObstacle();
            if (obs == Obstacle.BUSH) {
                boolean revealed = false;
                for (Pig pig : pigs) {
                    int dist = Math.abs(pig.getCol() - itemCol) + Math.abs(pig.getRow() - itemRow);
                    if (dist <= 1) { revealed = true; break; }
                }
                if (!revealed && sniffActive) {
                    int sniffDist = Math.abs(sniffCol - itemCol) + Math.abs(sniffRow - itemRow);
                    if (sniffDist <= 2) revealed = true;
                }
                if (!revealed) continue;
            }

            Group shape = buildShape(item.getType());
            double cx = itemCol * TILE + TILE / 2.0;
            double cy = itemRow * TILE + TILE / 2.0;

            if (isFlagship(item.getType())) {
                cy += Math.sin(animTick * 0.08 + (itemCol + itemRow) * 0.7) * 1.5;
            }

            shape.setTranslateX(cx);
            shape.setTranslateY(cy);
            group.getChildren().add(shape);
        }
    }

    private static boolean isFlagship(ItemType type) {
        return type == ItemType.GOLDEN_TRUFFLE
            || type == ItemType.SUPER_ACORN
            || type == ItemType.MAGNET_CROWN
            || type == ItemType.GREATER_SPEED;
    }

    // -------------------------------------------------------------------------
    // Shape builders — one per ItemType
    // -------------------------------------------------------------------------

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
            case SPEED_MUSHROOM:  return buildSpeedMushroom();
            case GREATER_SPEED:   return buildGreaterSpeed();
            case SHIELD_ACORN:    return buildShieldAcorn();
            case MAGNET_TRUFFLE:  return buildMagnetTruffle();
            case MAGNET_CROWN:    return buildMagnetCrown();
            case DECOY_MUSHROOM:  return buildDecoyMushroom();
            case SUPER_ACORN:     return buildSuperAcorn();
            default:
                Circle fallback = new Circle(0, 0, 6, Color.GREY);
                return new Group(fallback);
        }
    }

    // ------------------------------------------------------------------
    // BLACK_TRUFFLE — 6-sided polygon with pock-marks + highlight
    // ------------------------------------------------------------------
    private static Group buildBlackTruffle() {
        Polygon hex = regularHexagon(9, Color.rgb(80, 50, 20));
        hex.setStroke(Color.rgb(50, 30, 10));
        hex.setStrokeWidth(1.0);

        Ellipse highlight = new Ellipse(-3, -3, 2.5, 1.5);
        highlight.setFill(Color.rgb(255, 255, 255, 0.18));

        Color pock = Color.rgb(40, 25, 5);
        Circle p1 = new Circle(-2, 1, 1.0, pock);
        Circle p2 = new Circle(3, -2, 1.2, pock);
        Circle p3 = new Circle(1, 3, 0.8, pock);

        return new Group(hex, highlight, p1, p2, p3);
    }

    // ------------------------------------------------------------------
    // WHITE_TRUFFLE — cream hexagon with darker pock-marks + highlight
    // ------------------------------------------------------------------
    private static Group buildWhiteTruffle() {
        Polygon hex = regularHexagon(9, Color.rgb(240, 235, 210));
        hex.setStroke(Color.rgb(180, 170, 140));
        hex.setStrokeWidth(1.0);

        Ellipse highlight = new Ellipse(-3, -3, 3, 1.6);
        highlight.setFill(Color.rgb(255, 255, 255, 0.5));

        Color pock = Color.rgb(170, 160, 130);
        Circle p1 = new Circle(-2, 1, 1.0, pock);
        Circle p2 = new Circle(3, -1, 1.2, pock);
        Circle p3 = new Circle(0, 3, 0.8, pock);

        return new Group(hex, highlight, p1, p2, p3);
    }

    // ------------------------------------------------------------------
    // COMMON_MUSHROOM — tan cap with white spots + gill line
    // ------------------------------------------------------------------
    private static Group buildCommonMushroom() {
        Circle cap = new Circle(0, -3, 8, Color.rgb(210, 180, 140));
        cap.setStroke(Color.rgb(160, 120, 80));
        cap.setStrokeWidth(1.0);

        Rectangle stem = new Rectangle(-3, 3, 6, 6);
        stem.setFill(Color.rgb(230, 200, 160));
        stem.setStroke(Color.rgb(160, 120, 80));
        stem.setStrokeWidth(0.8);

        Color spot = Color.rgb(250, 240, 220);
        Circle s1 = new Circle(-3, -5, 1.2, spot);
        Circle s2 = new Circle(2, -2, 1.4, spot);
        Circle s3 = new Circle(-1, 0, 1.0, spot);

        Line gill = new Line(-5, 3, 5, 3);
        gill.setStroke(Color.rgb(160, 120, 80, 0.7));
        gill.setStrokeWidth(0.6);

        return new Group(stem, cap, s1, s2, s3, gill);
    }

    // ------------------------------------------------------------------
    // ACORN — nut + cap + thin stem + body highlight
    // ------------------------------------------------------------------
    private static Group buildAcorn() {
        Circle nut = new Circle(0, 2, 6, Color.rgb(139, 90, 43));
        nut.setStroke(Color.rgb(100, 60, 20));
        nut.setStrokeWidth(0.8);

        Ellipse cap = new Ellipse(0, -3, 5, 3);
        cap.setFill(Color.rgb(80, 50, 20));
        cap.setStroke(Color.rgb(50, 30, 10));
        cap.setStrokeWidth(0.8);

        Line stem = new Line(0, -7, 0, -5);
        stem.setStroke(Color.rgb(60, 35, 10));
        stem.setStrokeWidth(1.0);

        Ellipse highlight = new Ellipse(-2, 1, 1.4, 2.4);
        highlight.setFill(Color.rgb(255, 230, 180, 0.45));

        return new Group(nut, highlight, cap, stem);
    }

    // ------------------------------------------------------------------
    // CELERY — stalk with fiber lines + extra leaf
    // ------------------------------------------------------------------
    private static Group buildCelery() {
        Rectangle stalk = new Rectangle(-4, -9, 8, 18);
        stalk.setFill(Color.rgb(60, 179, 60));
        stalk.setStroke(Color.rgb(30, 120, 30));
        stalk.setStrokeWidth(1.0);

        Line fiber1 = new Line(-1.5, -7, -1.5, 7);
        fiber1.setStroke(Color.rgb(30, 120, 30, 0.7));
        fiber1.setStrokeWidth(0.6);
        Line fiber2 = new Line(1.5, -7, 1.5, 7);
        fiber2.setStroke(Color.rgb(30, 120, 30, 0.7));
        fiber2.setStrokeWidth(0.6);

        Ellipse leaf = new Ellipse(0, -10, 5, 3);
        leaf.setFill(Color.rgb(50, 160, 50));
        Ellipse leaf2 = new Ellipse(-3, -11, 3, 2);
        leaf2.setFill(Color.rgb(70, 180, 70));

        return new Group(stalk, fiber1, fiber2, leaf, leaf2);
    }

    // ------------------------------------------------------------------
    // DIET_PILL — already detailed, leave as-is
    // ------------------------------------------------------------------
    private static Group buildDietPill() {
        Rectangle body = new Rectangle(-7, -4, 14, 8);
        body.setFill(Color.WHITE);
        body.setStroke(Color.rgb(200, 200, 200));
        body.setStrokeWidth(0.8);
        body.setArcWidth(6);
        body.setArcHeight(6);

        Rectangle crossH = new Rectangle(-4, -1.5, 8, 3);
        crossH.setFill(Color.rgb(220, 30, 30));

        Rectangle crossV = new Rectangle(-1.5, -4, 3, 8);
        crossV.setFill(Color.rgb(220, 30, 30));

        return new Group(body, crossH, crossV);
    }

    // ------------------------------------------------------------------
    // MUD_SPLASH — splat plus extra asymmetric droplet
    // ------------------------------------------------------------------
    private static Group buildMudSplash() {
        Ellipse mud = new Ellipse(0, 0, 11, 7);
        mud.setFill(Color.rgb(101, 67, 33, 0.65));
        mud.setStroke(Color.rgb(70, 40, 10, 0.8));
        mud.setStrokeWidth(1.0);

        Circle dot1 = new Circle(-8, -4, 2.5, Color.rgb(101, 67, 33, 0.55));
        Circle dot2 = new Circle( 9,  3, 2.0, Color.rgb(101, 67, 33, 0.50));
        Ellipse drop = new Ellipse(-5, 5, 3, 1.5);
        drop.setFill(Color.rgb(101, 67, 33, 0.55));

        return new Group(mud, dot1, dot2, drop);
    }

    // ------------------------------------------------------------------
    // GOLDEN_TRUFFLE — gold hex with pock-marks + 2 sparkle stars
    // ------------------------------------------------------------------
    private static Group buildGoldenTruffle() {
        Polygon hex = regularHexagon(11, Color.rgb(255, 200, 0));
        hex.setStroke(Color.rgb(200, 140, 0));
        hex.setStrokeWidth(1.5);

        Color pock = Color.rgb(220, 160, 0);
        Circle p1 = new Circle(-3, 0, 1.2, pock);
        Circle p2 = new Circle(3, -2, 1.0, pock);
        Circle p3 = new Circle(0, 4, 1.3, pock);

        Polygon sparkle1 = sparkle(-7, -7, 2.5);
        Polygon sparkle2 = sparkle(8, 6, 2.0);

        return new Group(hex, p1, p2, p3, sparkle1, sparkle2);
    }

    // ------------------------------------------------------------------
    // SPEED_MUSHROOM — blue cap + white spots
    // ------------------------------------------------------------------
    private static Group buildSpeedMushroom() {
        Circle cap = new Circle(0, -3, 8, Color.rgb(60, 120, 255));
        cap.setStroke(Color.rgb(30, 70, 200));
        cap.setStrokeWidth(1.0);
        Rectangle stem = new Rectangle(-3, 3, 6, 6);
        stem.setFill(Color.rgb(180, 200, 255));
        stem.setStroke(Color.rgb(30, 70, 200));
        stem.setStrokeWidth(0.8);

        Color spot = Color.rgb(240, 245, 255);
        Circle s1 = new Circle(-3, -5, 1.2, spot);
        Circle s2 = new Circle(2, -2, 1.4, spot);

        return new Group(stem, cap, s1, s2);
    }

    // ------------------------------------------------------------------
    // GREATER_SPEED — deep blue mushroom with halo + 3 white spots
    // ------------------------------------------------------------------
    private static Group buildGreaterSpeed() {
        Circle halo = new Circle(0, -2, 12, Color.rgb(40, 90, 220, 0.25));
        Circle cap = new Circle(0, -3, 9, Color.rgb(40, 90, 220));
        cap.setStroke(Color.rgb(20, 50, 160));
        cap.setStrokeWidth(1.5);
        Rectangle stem = new Rectangle(-3, 3, 6, 6);
        stem.setFill(Color.rgb(160, 180, 240));
        stem.setStroke(Color.rgb(20, 50, 160));
        stem.setStrokeWidth(0.8);

        Color spot = Color.rgb(240, 245, 255);
        Circle s1 = new Circle(-4, -5, 1.3, spot);
        Circle s2 = new Circle(3, -2, 1.5, spot);
        Circle s3 = new Circle(0, -7, 1.1, spot);

        return new Group(halo, stem, cap, s1, s2, s3);
    }

    // ------------------------------------------------------------------
    // MAGNET_CROWN — magenta hex with crown spikes + white U glyph
    // ------------------------------------------------------------------
    private static Group buildMagnetCrown() {
        Polygon hex = regularHexagon(10, Color.rgb(220, 60, 240));
        hex.setStroke(Color.rgb(160, 30, 180));
        hex.setStrokeWidth(1.5);
        Polygon spike1 = new Polygon(-6.0, -8.0, -4.0, -14.0, -2.0, -8.0);
        Polygon spike2 = new Polygon(-2.0, -10.0, 0.0, -16.0, 2.0, -10.0);
        Polygon spike3 = new Polygon(2.0, -8.0, 4.0, -14.0, 6.0, -8.0);
        for (Polygon s : new Polygon[]{spike1, spike2, spike3}) {
            s.setFill(Color.rgb(255, 220, 80));
            s.setStroke(Color.rgb(180, 140, 0));
            s.setStrokeWidth(0.8);
        }

        Group g = new Group(hex, spike1, spike2, spike3);
        addMagnetGlyph(g, Color.WHITE);
        return g;
    }

    // ------------------------------------------------------------------
    // SHIELD_ACORN — gold disc with green shield ring + white tick
    // ------------------------------------------------------------------
    private static Group buildShieldAcorn() {
        Circle outer = new Circle(0, 0, 9, Color.TRANSPARENT);
        outer.setStroke(Color.rgb(100, 200, 100));
        outer.setStrokeWidth(2.0);
        Circle inner = new Circle(0, 0, 6, Color.rgb(255, 215, 0));
        inner.setStroke(Color.rgb(200, 160, 0));
        inner.setStrokeWidth(1.0);

        Polyline tick = new Polyline(-2.5, 0.0, -0.5, 2.5, 3.0, -2.5);
        tick.setStroke(Color.WHITE);
        tick.setStrokeWidth(1.5);
        tick.setFill(null);

        return new Group(outer, inner, tick);
    }

    // ------------------------------------------------------------------
    // MAGNET_TRUFFLE — purple hex with white magnet-U glyph
    // ------------------------------------------------------------------
    private static Group buildMagnetTruffle() {
        Polygon hex = regularHexagon(9, Color.rgb(160, 50, 220));
        hex.setStroke(Color.rgb(100, 20, 160));
        hex.setStrokeWidth(1.0);
        Group g = new Group(hex);
        addMagnetGlyph(g, Color.WHITE);
        return g;
    }

    // ------------------------------------------------------------------
    // DECOY_MUSHROOM — orange cap with question-mark dot pattern
    // ------------------------------------------------------------------
    private static Group buildDecoyMushroom() {
        Circle cap = new Circle(0, -3, 8, Color.rgb(255, 160, 40));
        cap.setStroke(Color.rgb(200, 100, 0));
        cap.setStrokeWidth(1.0);
        Rectangle stem = new Rectangle(-3, 3, 6, 6);
        stem.setFill(Color.rgb(255, 200, 130));
        stem.setStroke(Color.rgb(200, 100, 0));
        stem.setStrokeWidth(0.8);

        Color mark = Color.WHITE;
        Circle d1 = new Circle(-1, -7, 1.0, mark);
        Circle d2 = new Circle( 2, -5, 1.0, mark);
        Circle d3 = new Circle( 1, -2, 1.0, mark);
        Circle dotBig = new Circle(0,  1, 1.4, mark);

        return new Group(stem, cap, d1, d2, d3, dotBig);
    }

    // ------------------------------------------------------------------
    // SUPER_ACORN — golden glow + nut + 4-point star burst
    // ------------------------------------------------------------------
    private static Group buildSuperAcorn() {
        Circle glow = new Circle(0, 0, 12, Color.rgb(255, 215, 0, 0.3));
        Circle nut = new Circle(0, 0, 7, Color.rgb(255, 200, 0));
        nut.setStroke(Color.rgb(200, 140, 0));
        nut.setStrokeWidth(1.5);
        Ellipse cap = new Ellipse(0, -5, 5, 3);
        cap.setFill(Color.rgb(200, 140, 0));

        Polygon star = new Polygon(
            0.0, -10.0,
            2.0, -2.0,
            10.0, 0.0,
            2.0, 2.0,
            0.0, 10.0,
            -2.0, 2.0,
            -10.0, 0.0,
            -2.0, -2.0
        );
        star.setFill(Color.rgb(255, 255, 255, 0.55));

        return new Group(glow, star, nut, cap);
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /** Adds a small "U" magnet glyph centred on (0, 0) to the given group. */
    private static void addMagnetGlyph(Group g, Color stroke) {
        Line left  = new Line(-3, -2, -3, 3);
        Line right = new Line( 3, -2,  3, 3);
        Line bottom = new Line(-3, 3, 3, 3);
        for (Line l : new Line[]{left, right, bottom}) {
            l.setStroke(stroke);
            l.setStrokeWidth(1.6);
            g.getChildren().add(l);
        }
    }

    /** Tiny 4-point sparkle star at (cx, cy). */
    private static Polygon sparkle(double cx, double cy, double size) {
        Polygon s = new Polygon(
            cx,         cy - size,
            cx + size * 0.35, cy - size * 0.35,
            cx + size,  cy,
            cx + size * 0.35, cy + size * 0.35,
            cx,         cy + size,
            cx - size * 0.35, cy + size * 0.35,
            cx - size,  cy,
            cx - size * 0.35, cy - size * 0.35
        );
        s.setFill(Color.rgb(255, 255, 255, 0.85));
        return s;
    }

    /**
     * Creates a regular hexagon centred at (0, 0) with the given radius and fill.
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
