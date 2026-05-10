package com.example.demo.render;

import com.example.demo.entity.Pig;
import com.example.demo.item.Item;
import com.example.demo.item.ItemType;
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
    // BLACK_TRUFFLE — outer hex + inner highlight hex + 5 pocks + tendril
    // ------------------------------------------------------------------
    private static Group buildBlackTruffle() {
        Group g = new Group();
        groundShadow(g, 9, 3);
        Polygon hex = regularHexagon(9, Color.rgb(80, 50, 20));
        hex.setStroke(Color.rgb(50, 30, 10));
        hex.setStrokeWidth(1.0);
        Polygon inner = regularHexagon(5, Color.rgb(110, 75, 40, 0.85));
        inner.setTranslateX(-1.5);
        inner.setTranslateY(-1.5);
        Ellipse highlight = new Ellipse(-3, -3, 2.8, 1.5);
        highlight.setFill(Color.rgb(255, 230, 200, 0.30));
        Color pock = Color.rgb(40, 25, 5);
        Color pockCore = Color.rgb(20, 12, 2);
        g.getChildren().addAll(hex, inner, highlight);
        addPock(g, -2, 1, 1.0, pock, pockCore);
        addPock(g,  3, -2, 1.2, pock, pockCore);
        addPock(g,  1,  3, 0.8, pock, pockCore);
        addPock(g, -4, -1, 0.9, pock, pockCore);
        addPock(g,  4,  2, 0.8, pock, pockCore);
        Line tendril = new Line(0, 8, 0, 11);
        tendril.setStroke(Color.rgb(50, 30, 10));
        tendril.setStrokeWidth(1.2);
        g.getChildren().add(tendril);
        return g;
    }

    // ------------------------------------------------------------------
    // WHITE_TRUFFLE — outer hex + inner highlight + 5 pocks + tendril
    // ------------------------------------------------------------------
    private static Group buildWhiteTruffle() {
        Group g = new Group();
        groundShadow(g, 9, 3);
        Polygon hex = regularHexagon(9, Color.rgb(240, 235, 210));
        hex.setStroke(Color.rgb(180, 170, 140));
        hex.setStrokeWidth(1.0);
        Polygon inner = regularHexagon(5, Color.rgb(255, 250, 230, 0.7));
        inner.setTranslateX(-1.5);
        inner.setTranslateY(-1.5);
        Ellipse highlight = new Ellipse(-3, -3, 3, 1.6);
        highlight.setFill(Color.rgb(255, 255, 255, 0.55));
        Color pock = Color.rgb(170, 160, 130);
        Color pockCore = Color.rgb(120, 110, 80);
        g.getChildren().addAll(hex, inner, highlight);
        addPock(g, -2,  1, 1.0, pock, pockCore);
        addPock(g,  3, -1, 1.2, pock, pockCore);
        addPock(g,  0,  3, 0.8, pock, pockCore);
        addPock(g, -4, -1, 0.9, pock, pockCore);
        addPock(g,  4,  2, 0.8, pock, pockCore);
        Line tendril = new Line(0, 8, 0, 11);
        tendril.setStroke(Color.rgb(180, 170, 140));
        tendril.setStrokeWidth(1.2);
        g.getChildren().add(tendril);
        return g;
    }

    // ------------------------------------------------------------------
    // COMMON_MUSHROOM — layered tan dome + flared stem + spots w/ highlights
    // ------------------------------------------------------------------
    private static Group buildCommonMushroom() {
        Group g = new Group();
        groundShadow(g, 8, 2.5);
        // Stem (rect + flare base)
        Rectangle stem = new Rectangle(-3, 3, 6, 6);
        stem.setFill(Color.rgb(230, 200, 160));
        stem.setStroke(Color.rgb(160, 120, 80));
        stem.setStrokeWidth(0.8);
        Polygon flare = new Polygon(
            -4.5, 9,
             4.5, 9,
             3,   7,
            -3,   7
        );
        flare.setFill(Color.rgb(220, 190, 150));
        flare.setStroke(Color.rgb(160, 120, 80));
        flare.setStrokeWidth(0.5);
        Line gill1 = new Line(-1.5, 3, -1.5, 9);
        Line gill2 = new Line(1.5, 3, 1.5, 9);
        for (Line l : new Line[]{gill1, gill2}) {
            l.setStroke(Color.rgb(160, 120, 80, 0.7));
            l.setStrokeWidth(0.4);
        }
        // Layered cap (rim shadow, mid band, top dome)
        Ellipse rim = new Ellipse(0, -1, 9, 4);
        rim.setFill(Color.rgb(170, 140, 100));
        Circle cap = new Circle(0, -3, 8, Color.rgb(210, 180, 140));
        cap.setStroke(Color.rgb(160, 120, 80));
        cap.setStrokeWidth(1.0);
        Ellipse domeHi = new Ellipse(-1, -5, 4.5, 2.4);
        domeHi.setFill(Color.rgb(240, 215, 180, 0.7));
        // Spots (each = circle + tiny highlight)
        Color spot = Color.rgb(250, 240, 220);
        Color spotHi = Color.rgb(255, 255, 255);
        g.getChildren().addAll(stem, flare, gill1, gill2, rim, cap, domeHi);
        addSpot(g, -3, -5, 1.4, spot, spotHi);
        addSpot(g,  2, -2, 1.6, spot, spotHi);
        addSpot(g, -1,  0, 1.1, spot, spotHi);
        addSpot(g,  4, -5, 1.0, spot, spotHi);
        return g;
    }

    // ------------------------------------------------------------------
    // ACORN — layered nut + cross-hatched cap + leaf + shadow
    // ------------------------------------------------------------------
    private static Group buildAcorn() {
        Group g = new Group();
        groundShadow(g, 7, 2.5);
        // Layered nut (outer + inner highlight ellipse)
        Ellipse nut = new Ellipse(0, 2, 6.5, 6.5);
        nut.setFill(Color.rgb(139, 90, 43));
        nut.setStroke(Color.rgb(100, 60, 20));
        nut.setStrokeWidth(0.8);
        Ellipse nutHi = new Ellipse(-1.6, 0.8, 2.4, 4);
        nutHi.setFill(Color.rgb(200, 150, 90, 0.55));
        // Cap with cross-hatch
        Ellipse cap = new Ellipse(0, -3, 5, 3);
        cap.setFill(Color.rgb(80, 50, 20));
        cap.setStroke(Color.rgb(50, 30, 10));
        cap.setStrokeWidth(0.8);
        Line hatch1 = new Line(-3.5, -3.5, -2.5, -2.0);
        Line hatch2 = new Line(-1.0, -4.0, 0.0, -2.5);
        Line hatch3 = new Line( 1.5, -3.5, 2.5, -2.0);
        for (Line l : new Line[]{hatch1, hatch2, hatch3}) {
            l.setStroke(Color.rgb(50, 30, 10, 0.7));
            l.setStrokeWidth(0.5);
        }
        // Stem
        Line stem = new Line(0, -7, 0, -5);
        stem.setStroke(Color.rgb(60, 35, 10));
        stem.setStrokeWidth(1.0);
        // Tiny leaf on stem
        Polygon leaf = new Polygon(
            0,  -7,
            3,  -8,
            2,  -9.5,
           -0.5,-8.5
        );
        leaf.setFill(Color.rgb(80, 170, 70));
        leaf.setStroke(Color.rgb(40, 110, 40));
        leaf.setStrokeWidth(0.4);
        g.getChildren().addAll(nut, nutHi, cap, hatch1, hatch2, hatch3, stem, leaf);
        return g;
    }

    // ------------------------------------------------------------------
    // CELERY — 2-tone stalk + fibers + 3 leaves with veins
    // ------------------------------------------------------------------
    private static Group buildCelery() {
        Group g = new Group();
        groundShadow(g, 6, 2);
        Rectangle outer = new Rectangle(-4, -9, 8, 18);
        outer.setFill(Color.rgb(60, 179, 60));
        outer.setStroke(Color.rgb(30, 120, 30));
        outer.setStrokeWidth(1.0);
        Rectangle inner = new Rectangle(-2.5, -8, 5, 16);
        inner.setFill(Color.rgb(80, 200, 80));
        inner.setStroke(null);
        Line fiber1 = new Line(-1.5, -7, -1.5, 7);
        Line fiber2 = new Line( 1.5, -7,  1.5, 7);
        Line fiber3 = new Line( 0,   -8,  0,   8);
        for (Line l : new Line[]{fiber1, fiber2, fiber3}) {
            l.setStroke(Color.rgb(30, 120, 30, 0.7));
            l.setStrokeWidth(0.6);
        }
        // 3 leaves with veins
        Ellipse leaf1 = new Ellipse(0, -10, 5, 3);
        leaf1.setFill(Color.rgb(50, 160, 50));
        leaf1.setStroke(Color.rgb(20, 100, 20));
        leaf1.setStrokeWidth(0.4);
        Ellipse leaf2 = new Ellipse(-3, -11, 3, 2);
        leaf2.setFill(Color.rgb(70, 180, 70));
        leaf2.setStroke(Color.rgb(20, 100, 20));
        leaf2.setStrokeWidth(0.3);
        Ellipse leaf3 = new Ellipse( 3, -11, 3, 2);
        leaf3.setFill(Color.rgb(70, 180, 70));
        leaf3.setStroke(Color.rgb(20, 100, 20));
        leaf3.setStrokeWidth(0.3);
        Line vein1 = new Line(-3, -10, 3, -10);
        Line vein2 = new Line(-3, -11, 0, -10);
        Line vein3 = new Line( 3, -11, 0, -10);
        for (Line v : new Line[]{vein1, vein2, vein3}) {
            v.setStroke(Color.rgb(20, 100, 20, 0.7));
            v.setStrokeWidth(0.4);
        }
        g.getChildren().addAll(outer, inner, fiber1, fiber2, fiber3,
                               leaf1, leaf2, leaf3, vein1, vein2, vein3);
        return g;
    }

    // ------------------------------------------------------------------
    // DIET_PILL — split-color capsule + highlight + 2 pellets
    // ------------------------------------------------------------------
    private static Group buildDietPill() {
        Group g = new Group();
        groundShadow(g, 7, 2);
        Rectangle leftHalf = new Rectangle(-7, -4, 7, 8);
        leftHalf.setFill(Color.WHITE);
        leftHalf.setStroke(Color.rgb(200, 200, 200));
        leftHalf.setStrokeWidth(0.8);
        leftHalf.setArcWidth(6);
        leftHalf.setArcHeight(6);
        Rectangle rightHalf = new Rectangle(0, -4, 7, 8);
        rightHalf.setFill(Color.rgb(220, 30, 30));
        rightHalf.setStroke(Color.rgb(160, 10, 10));
        rightHalf.setStrokeWidth(0.8);
        rightHalf.setArcWidth(6);
        rightHalf.setArcHeight(6);
        // Cross overlay
        Rectangle crossH = new Rectangle(-3, -1.2, 6, 2.4);
        crossH.setFill(Color.WHITE);
        Rectangle crossV = new Rectangle(-1.2, -3, 2.4, 6);
        crossV.setFill(Color.WHITE);
        // Top highlight
        Ellipse hi = new Ellipse(0, -2.5, 4.5, 1.0);
        hi.setFill(Color.rgb(255, 255, 255, 0.55));
        // Two pellets next to the capsule
        Circle pellet1 = new Circle(-9, 3, 1.4, Color.rgb(220, 30, 30));
        pellet1.setStroke(Color.rgb(160, 10, 10));
        pellet1.setStrokeWidth(0.4);
        Circle pellet2 = new Circle( 9, 4, 1.2, Color.WHITE);
        pellet2.setStroke(Color.rgb(180, 180, 180));
        pellet2.setStrokeWidth(0.4);
        g.getChildren().addAll(leftHalf, rightHalf, crossH, crossV, hi, pellet1, pellet2);
        return g;
    }

    // ------------------------------------------------------------------
    // MUD_SPLASH — main puddle + 2 ripples + 5 splatters + wet shine
    // ------------------------------------------------------------------
    private static Group buildMudSplash() {
        Group g = new Group();
        Ellipse mud = new Ellipse(0, 0, 11, 7);
        mud.setFill(Color.rgb(101, 67, 33, 0.65));
        mud.setStroke(Color.rgb(70, 40, 10, 0.8));
        mud.setStrokeWidth(1.0);
        Ellipse mudInner = new Ellipse(-1, -0.5, 7, 4);
        mudInner.setFill(Color.rgb(70, 40, 10, 0.55));
        Arc ripple1 = new Arc(0, 0, 12, 8, 30, 120);
        ripple1.setType(ArcType.OPEN);
        ripple1.setFill(Color.TRANSPARENT);
        ripple1.setStroke(Color.rgb(101, 67, 33, 0.45));
        ripple1.setStrokeWidth(0.8);
        Arc ripple2 = new Arc(0, 0, 14, 9, 200, 100);
        ripple2.setType(ArcType.OPEN);
        ripple2.setFill(Color.TRANSPARENT);
        ripple2.setStroke(Color.rgb(70, 40, 10, 0.5));
        ripple2.setStrokeWidth(0.7);
        Color splash = Color.rgb(101, 67, 33, 0.55);
        Circle s1 = new Circle(-9, -4, 1.6, splash);
        Circle s2 = new Circle(10,  3, 1.4, splash);
        Circle s3 = new Circle(-7,  5, 1.2, splash);
        Circle s4 = new Circle( 6, -5, 1.0, splash);
        Circle s5 = new Circle(-2, -7, 0.9, splash);
        Ellipse shine = new Ellipse(-2, -2, 3.5, 1.0);
        shine.setFill(Color.rgb(220, 200, 160, 0.5));
        g.getChildren().addAll(mud, mudInner, ripple1, ripple2, s1, s2, s3, s4, s5, shine);
        return g;
    }

    // ------------------------------------------------------------------
    // GOLDEN_TRUFFLE — 2 hexes + facet triangles + sparkles + light blooms
    // ------------------------------------------------------------------
    private static Group buildGoldenTruffle() {
        Group g = new Group();
        // Outer light bloom
        Circle bloom = new Circle(0, 0, 14, Color.rgb(255, 230, 120, 0.20));
        Polygon hex = regularHexagon(11, Color.rgb(255, 200, 0));
        hex.setStroke(Color.rgb(200, 140, 0));
        hex.setStrokeWidth(1.5);
        // Facet triangles for gem cuts (centered)
        Polygon facet1 = new Polygon(0.0, -10.0, 5.5, -3.0, 0.0, 0.0);
        Polygon facet2 = new Polygon(0.0, -10.0, -5.5, -3.0, 0.0, 0.0);
        Polygon facet3 = new Polygon(0.0, 10.0, 5.5, 3.0, 0.0, 0.0);
        Polygon facet4 = new Polygon(0.0, 10.0, -5.5, 3.0, 0.0, 0.0);
        for (Polygon p : new Polygon[]{facet1, facet3}) {
            p.setFill(Color.rgb(255, 230, 120, 0.5));
        }
        for (Polygon p : new Polygon[]{facet2, facet4}) {
            p.setFill(Color.rgb(220, 160, 0, 0.4));
        }
        // Inner highlight hex
        Polygon innerHex = regularHexagon(5, Color.rgb(255, 245, 180, 0.85));
        innerHex.setTranslateX(-1.5);
        innerHex.setTranslateY(-1.5);
        // Sparkles around the gem
        Polygon sp1 = sparkle(-8, -8, 2.5);
        Polygon sp2 = sparkle( 9,  6, 2.0);
        Polygon sp3 = sparkle(-9,  7, 1.6);
        Polygon sp4 = sparkle( 7, -9, 1.8);
        Polygon sp5 = sparkle( 0, -12, 1.4);
        // Inner shine ellipse
        Ellipse shine = new Ellipse(-2.5, -3, 2.5, 1.4);
        shine.setFill(Color.rgb(255, 255, 255, 0.7));
        g.getChildren().addAll(bloom, hex, facet1, facet2, facet3, facet4,
                               innerHex, shine, sp1, sp2, sp3, sp4, sp5);
        return g;
    }

    // ------------------------------------------------------------------
    // SPEED_MUSHROOM — layered blue cap + flared stem + spots
    // ------------------------------------------------------------------
    private static Group buildSpeedMushroom() {
        Group g = new Group();
        groundShadow(g, 8, 2.5);
        Rectangle stem = new Rectangle(-3, 3, 6, 6);
        stem.setFill(Color.rgb(180, 200, 255));
        stem.setStroke(Color.rgb(30, 70, 200));
        stem.setStrokeWidth(0.8);
        Polygon flare = new Polygon(-4.5, 9, 4.5, 9, 3, 7, -3, 7);
        flare.setFill(Color.rgb(160, 180, 240));
        flare.setStroke(Color.rgb(30, 70, 200));
        flare.setStrokeWidth(0.5);
        Line gill1 = new Line(-1.5, 3, -1.5, 9);
        Line gill2 = new Line(1.5, 3, 1.5, 9);
        for (Line l : new Line[]{gill1, gill2}) {
            l.setStroke(Color.rgb(30, 70, 200, 0.7));
            l.setStrokeWidth(0.4);
        }
        Ellipse rim = new Ellipse(0, -1, 9, 4);
        rim.setFill(Color.rgb(40, 80, 200));
        Circle cap = new Circle(0, -3, 8, Color.rgb(60, 120, 255));
        cap.setStroke(Color.rgb(30, 70, 200));
        cap.setStrokeWidth(1.0);
        Ellipse domeHi = new Ellipse(-1, -5, 4.5, 2.4);
        domeHi.setFill(Color.rgb(160, 200, 255, 0.7));
        Color spot = Color.rgb(240, 245, 255);
        Color spotHi = Color.rgb(255, 255, 255);
        g.getChildren().addAll(stem, flare, gill1, gill2, rim, cap, domeHi);
        addSpot(g, -3, -5, 1.3, spot, spotHi);
        addSpot(g,  2, -2, 1.5, spot, spotHi);
        addSpot(g,  4, -5, 1.0, spot, spotHi);
        return g;
    }

    // ------------------------------------------------------------------
    // GREATER_SPEED — deep blue mushroom with halo + extra inner glow
    // ------------------------------------------------------------------
    private static Group buildGreaterSpeed() {
        Group g = new Group();
        Circle halo = new Circle(0, -2, 13, Color.rgb(40, 90, 220, 0.22));
        Circle haloInner = new Circle(0, -3, 9.5, Color.rgb(80, 130, 240, 0.30));
        groundShadow(g, 8, 2.5);
        Rectangle stem = new Rectangle(-3, 3, 6, 6);
        stem.setFill(Color.rgb(160, 180, 240));
        stem.setStroke(Color.rgb(20, 50, 160));
        stem.setStrokeWidth(0.8);
        Polygon flare = new Polygon(-4.5, 9, 4.5, 9, 3, 7, -3, 7);
        flare.setFill(Color.rgb(140, 160, 220));
        flare.setStroke(Color.rgb(20, 50, 160));
        flare.setStrokeWidth(0.5);
        Ellipse rim = new Ellipse(0, -1, 10, 4.5);
        rim.setFill(Color.rgb(20, 50, 160));
        Circle cap = new Circle(0, -3, 9, Color.rgb(40, 90, 220));
        cap.setStroke(Color.rgb(20, 50, 160));
        cap.setStrokeWidth(1.5);
        Ellipse domeHi = new Ellipse(-1, -5, 5, 2.6);
        domeHi.setFill(Color.rgb(180, 220, 255, 0.7));
        Color spot = Color.rgb(240, 245, 255);
        Color spotHi = Color.rgb(255, 255, 255);
        g.getChildren().addAll(halo, haloInner, stem, flare, rim, cap, domeHi);
        addSpot(g, -4, -5, 1.4, spot, spotHi);
        addSpot(g,  3, -2, 1.6, spot, spotHi);
        addSpot(g,  0, -7, 1.2, spot, spotHi);
        addSpot(g,  5, -5, 1.0, spot, spotHi);
        return g;
    }

    // ------------------------------------------------------------------
    // MAGNET_CROWN — 2-tone hex + crown spikes + 2-pole magnet + sparks
    // ------------------------------------------------------------------
    private static Group buildMagnetCrown() {
        Group g = new Group();
        Polygon hex = regularHexagon(10, Color.rgb(220, 60, 240));
        hex.setStroke(Color.rgb(160, 30, 180));
        hex.setStrokeWidth(1.5);
        Polygon innerHex = regularHexagon(5.5, Color.rgb(255, 140, 255, 0.7));
        innerHex.setTranslateX(-1.5);
        innerHex.setTranslateY(-1.5);
        Polygon spike1 = new Polygon(-6.0, -8.0, -4.0, -14.0, -2.0, -8.0);
        Polygon spike2 = new Polygon(-2.0, -10.0, 0.0, -16.0, 2.0, -10.0);
        Polygon spike3 = new Polygon(2.0, -8.0, 4.0, -14.0, 6.0, -8.0);
        for (Polygon s : new Polygon[]{spike1, spike2, spike3}) {
            s.setFill(Color.rgb(255, 220, 80));
            s.setStroke(Color.rgb(180, 140, 0));
            s.setStrokeWidth(0.8);
        }
        // Tiny gem at the base of each spike
        Circle gem1 = new Circle(-4, -8, 0.9, Color.rgb(255, 80, 80));
        Circle gem2 = new Circle( 0, -10, 1.0, Color.rgb(80, 180, 255));
        Circle gem3 = new Circle( 4, -8, 0.9, Color.rgb(255, 80, 80));
        g.getChildren().addAll(hex, innerHex, spike1, spike2, spike3, gem1, gem2, gem3);
        addMagnetUWithPoles(g, Color.WHITE);
        addSparks(g);
        return g;
    }

    // ------------------------------------------------------------------
    // SHIELD_ACORN — green ring + gold disc + tick + 4 corner studs
    // ------------------------------------------------------------------
    private static Group buildShieldAcorn() {
        Group g = new Group();
        groundShadow(g, 8, 2);
        Circle outer = new Circle(0, 0, 9.5, Color.TRANSPARENT);
        outer.setStroke(Color.rgb(100, 200, 100));
        outer.setStrokeWidth(2.0);
        Circle ringHi = new Circle(0, 0, 9.5, Color.TRANSPARENT);
        ringHi.setStroke(Color.rgb(180, 240, 180, 0.6));
        ringHi.setStrokeWidth(0.6);
        Circle inner = new Circle(0, 0, 6.5, Color.rgb(255, 215, 0));
        inner.setStroke(Color.rgb(200, 160, 0));
        inner.setStrokeWidth(1.0);
        // Inner gold highlight
        Ellipse innerHi = new Ellipse(-1.5, -1.8, 2.6, 1.3);
        innerHi.setFill(Color.rgb(255, 245, 180, 0.7));
        Polyline tick = new Polyline(-2.5, 0.0, -0.5, 2.5, 3.0, -2.5);
        tick.setStroke(Color.WHITE);
        tick.setStrokeWidth(1.7);
        tick.setFill(null);
        // 4 corner studs around the ring
        Circle stud1 = new Circle(-9, 0, 1.0, Color.rgb(80, 180, 80));
        Circle stud2 = new Circle( 9, 0, 1.0, Color.rgb(80, 180, 80));
        Circle stud3 = new Circle(0, -9, 1.0, Color.rgb(80, 180, 80));
        Circle stud4 = new Circle(0,  9, 1.0, Color.rgb(80, 180, 80));
        g.getChildren().addAll(outer, ringHi, inner, innerHi, tick, stud1, stud2, stud3, stud4);
        return g;
    }

    // ------------------------------------------------------------------
    // MAGNET_TRUFFLE — 2-tone purple hex + magnet U with poles + sparks
    // ------------------------------------------------------------------
    private static Group buildMagnetTruffle() {
        Group g = new Group();
        groundShadow(g, 9, 3);
        Polygon hex = regularHexagon(9, Color.rgb(160, 50, 220));
        hex.setStroke(Color.rgb(100, 20, 160));
        hex.setStrokeWidth(1.0);
        Polygon innerHex = regularHexagon(5, Color.rgb(200, 110, 240, 0.7));
        innerHex.setTranslateX(-1.5);
        innerHex.setTranslateY(-1.5);
        g.getChildren().addAll(hex, innerHex);
        addMagnetUWithPoles(g, Color.WHITE);
        addSparks(g);
        return g;
    }

    // ------------------------------------------------------------------
    // DECOY_MUSHROOM — orange layered cap + flared stem + ?-pattern dots
    // ------------------------------------------------------------------
    private static Group buildDecoyMushroom() {
        Group g = new Group();
        groundShadow(g, 8, 2.5);
        Rectangle stem = new Rectangle(-3, 3, 6, 6);
        stem.setFill(Color.rgb(255, 200, 130));
        stem.setStroke(Color.rgb(200, 100, 0));
        stem.setStrokeWidth(0.8);
        Polygon flare = new Polygon(-4.5, 9, 4.5, 9, 3, 7, -3, 7);
        flare.setFill(Color.rgb(240, 180, 110));
        flare.setStroke(Color.rgb(200, 100, 0));
        flare.setStrokeWidth(0.5);
        Line gill1 = new Line(-1.5, 3, -1.5, 9);
        Line gill2 = new Line(1.5, 3, 1.5, 9);
        for (Line l : new Line[]{gill1, gill2}) {
            l.setStroke(Color.rgb(200, 100, 0, 0.7));
            l.setStrokeWidth(0.4);
        }
        Ellipse rim = new Ellipse(0, -1, 9, 4);
        rim.setFill(Color.rgb(220, 130, 30));
        Circle cap = new Circle(0, -3, 8, Color.rgb(255, 160, 40));
        cap.setStroke(Color.rgb(200, 100, 0));
        cap.setStrokeWidth(1.0);
        Ellipse domeHi = new Ellipse(-1, -5, 4.5, 2.4);
        domeHi.setFill(Color.rgb(255, 210, 140, 0.7));
        Color mark = Color.WHITE;
        g.getChildren().addAll(stem, flare, gill1, gill2, rim, cap, domeHi);
        addSpot(g, -1, -7, 1.0, mark, mark);
        addSpot(g,  2, -5, 1.0, mark, mark);
        addSpot(g,  1, -2, 1.0, mark, mark);
        addSpot(g,  0,  1, 1.4, mark, mark);
        return g;
    }

    // ------------------------------------------------------------------
    // SUPER_ACORN — multi-glow + nut + 8-point burst + corner sparkles
    // ------------------------------------------------------------------
    private static Group buildSuperAcorn() {
        Group g = new Group();
        Circle glowOuter = new Circle(0, 0, 14, Color.rgb(255, 215, 0, 0.18));
        Circle glowMid   = new Circle(0, 0, 10, Color.rgb(255, 230, 100, 0.30));
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
        Circle nut = new Circle(0, 0, 7, Color.rgb(255, 200, 0));
        nut.setStroke(Color.rgb(200, 140, 0));
        nut.setStrokeWidth(1.5);
        Ellipse nutHi = new Ellipse(-1.6, -1.8, 2.6, 3.2);
        nutHi.setFill(Color.rgb(255, 245, 180, 0.7));
        Ellipse cap = new Ellipse(0, -5, 5, 3);
        cap.setFill(Color.rgb(200, 140, 0));
        cap.setStroke(Color.rgb(160, 100, 0));
        cap.setStrokeWidth(0.8);
        // Cross-hatch on cap
        Line hatch1 = new Line(-3.5, -5.5, -2.5, -4.0);
        Line hatch2 = new Line(-1.0, -6.0, 0.0, -4.5);
        Line hatch3 = new Line( 1.5, -5.5, 2.5, -4.0);
        for (Line l : new Line[]{hatch1, hatch2, hatch3}) {
            l.setStroke(Color.rgb(160, 100, 0, 0.7));
            l.setStrokeWidth(0.4);
        }
        // Corner sparkles
        Polygon sp1 = sparkle(-9, -9, 2.0);
        Polygon sp2 = sparkle( 9,  9, 1.6);
        Polygon sp3 = sparkle( 9, -9, 1.4);
        Polygon sp4 = sparkle(-9,  9, 1.4);
        g.getChildren().addAll(glowOuter, glowMid, star, nut, nutHi, cap,
                               hatch1, hatch2, hatch3, sp1, sp2, sp3, sp4);
        return g;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /** Adds a soft elliptical ground shadow centred at (0, 0). */
    private static void groundShadow(Group g, double rx, double ry) {
        Ellipse shadow = new Ellipse(0, 11, rx, ry);
        shadow.setFill(Color.rgb(0, 0, 0, 0.18));
        g.getChildren().add(shadow);
    }

    /** Adds a pock mark (outer dim circle + darker centre dot). */
    private static void addPock(Group g, double x, double y, double r, Color outer, Color core) {
        Circle o = new Circle(x, y, r, outer);
        Circle c = new Circle(x, y, r * 0.45, core);
        g.getChildren().addAll(o, c);
    }

    /** Adds a spot (filled circle + tiny white highlight). */
    private static void addSpot(Group g, double x, double y, double r, Color fill, Color hi) {
        Circle c = new Circle(x, y, r, fill);
        Circle h = new Circle(x - r * 0.35, y - r * 0.35, r * 0.35, hi);
        h.setOpacity(0.85);
        g.getChildren().addAll(c, h);
    }

    /** Adds a U-shaped magnet glyph with red/blue pole rectangles. */
    private static void addMagnetUWithPoles(Group g, Color stroke) {
        Line left  = new Line(-3, -2, -3, 3);
        Line right = new Line( 3, -2,  3, 3);
        Line bottom = new Line(-3, 3, 3, 3);
        for (Line l : new Line[]{left, right, bottom}) {
            l.setStroke(stroke);
            l.setStrokeWidth(1.6);
            g.getChildren().add(l);
        }
        // Pole tips: red on left, blue on right
        Rectangle redPole  = new Rectangle(-4, -3.5, 2, 1.6);
        redPole.setFill(Color.rgb(220, 40, 40));
        Rectangle bluePole = new Rectangle( 2, -3.5, 2, 1.6);
        bluePole.setFill(Color.rgb(40, 80, 220));
        g.getChildren().addAll(redPole, bluePole);
    }

    /** Adds 4 small attraction-spark circles around the magnet. */
    private static void addSparks(Group g) {
        Color spark = Color.rgb(255, 255, 200, 0.85);
        Circle s1 = new Circle(-7, -1, 0.9, spark);
        Circle s2 = new Circle( 7, -1, 0.9, spark);
        Circle s3 = new Circle(-5,  6, 0.7, spark);
        Circle s4 = new Circle( 5,  6, 0.7, spark);
        g.getChildren().addAll(s1, s2, s3, s4);
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
