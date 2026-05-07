package com.example.demo.render;

import com.example.demo.entity.Direction;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;

/**
 * Factory that builds JavaFX shape groups representing a pig.
 *
 * <p>All shapes are pure JavaFX primitives (Circle, Ellipse, Line). The pig
 * silhouette in order from back to front:
 * <pre>
 *   tail → legs → ears → body → snout → mouth → nostrils → eyes → identity marker
 * </pre>
 *
 * <p>Identity markers are switched on the pig's name so each rival reads
 * differently at a glance even when they share a body color shade:
 * <ul>
 *   <li>"Player"   – 4-dot flower above the head</li>
 *   <li>"Hoggart"  – single leaf "antenna" sticking up</li>
 *   <li>"Whiskers" – fine whisker lines fanning from the snout</li>
 *   <li>"Bramble"  – V-shaped angry eyebrow above the eyes</li>
 * </ul>
 */
public final class ShapeFactory {

    private ShapeFactory() {}

    public static Group createPigShape(Color bodyColor, double radius, Direction facing) {
        return createPigShape(bodyColor, radius, facing, "");
    }

    /**
     * Creates a {@link Group} that visually represents a pig, centred at (0, 0).
     *
     * @param bodyColor body fill color
     * @param radius    body radius in pixels
     * @param facing    direction the pig is facing (NONE → defaults to RIGHT)
     * @param name      pig name; drives the identity-marker accessory
     */
    public static Group createPigShape(Color bodyColor, double radius, Direction facing, String name) {

        Direction snoutDir = (facing == Direction.NONE) ? Direction.RIGHT : facing;
        double snoutRadius = radius * 0.35;
        double snoutOffsetFactor = 0.65;
        double snoutX = snoutDir.dc * radius * snoutOffsetFactor;
        double snoutY = snoutDir.dr * radius * snoutOffsetFactor;

        // Perpendicular axis to facing — used for nostrils, eyes, whiskers, mouth
        double perpX, perpY;
        if (snoutDir == Direction.UP || snoutDir == Direction.DOWN) {
            perpX = 1; perpY = 0;
        } else {
            perpX = 0; perpY = 1;
        }

        Group group = new Group();

        // --- Tail (opposite facing) ---
        double tailX = -snoutDir.dc * radius * 0.95;
        double tailY = -snoutDir.dr * radius * 0.95;
        Circle tail = new Circle(tailX, tailY, radius * 0.20);
        tail.setFill(bodyColor);
        tail.setStroke(bodyColor.darker());
        tail.setStrokeWidth(1.0);
        group.getChildren().add(tail);

        // --- Legs: 4 small ovals at the diagonal corners so they don't clash with snout/tail ---
        Color legColor = bodyColor.darker();
        double legD = radius * 0.85;
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(45 + i * 90);
            double lx = Math.cos(a) * legD;
            double ly = Math.sin(a) * legD;
            Ellipse leg = new Ellipse(lx, ly, radius * 0.18, radius * 0.14);
            leg.setFill(legColor);
            leg.setStroke(legColor.darker());
            leg.setStrokeWidth(0.6);
            group.getChildren().add(leg);
        }

        // --- Ears (above body, slightly toward facing for a forward-leaning look) ---
        double earRadius = radius * 0.25;
        double earOffset = radius * 0.55;
        // Ears positioned perpendicular-spread + slight forward offset
        double earForward = 0.15;
        double earBackX = -snoutDir.dc * radius * earForward;
        double earBackY = -snoutDir.dr * radius * earForward;
        Circle earLeft  = new Circle(earBackX - perpX * earOffset, earBackY - perpY * earOffset, earRadius);
        Circle earRight = new Circle(earBackX + perpX * earOffset, earBackY + perpY * earOffset, earRadius);
        earLeft.setFill(bodyColor.darker());
        earLeft.setStroke(bodyColor.darker().darker());
        earLeft.setStrokeWidth(1.0);
        earRight.setFill(bodyColor.darker());
        earRight.setStroke(bodyColor.darker().darker());
        earRight.setStrokeWidth(1.0);
        group.getChildren().addAll(earLeft, earRight);

        // --- Body ---
        Circle body = new Circle(0, 0, radius);
        body.setFill(bodyColor);
        body.setStroke(bodyColor.darker());
        body.setStrokeWidth(1.5);
        group.getChildren().add(body);

        // --- Snout ---
        Circle snout = new Circle(snoutX, snoutY, snoutRadius);
        snout.setFill(bodyColor.darker());
        snout.setStroke(bodyColor.darker().darker());
        snout.setStrokeWidth(1.0);
        group.getChildren().add(snout);

        // --- Mouth (small line on the leading edge of the snout) ---
        double mouthCX = snoutX + snoutDir.dc * snoutRadius * 0.45;
        double mouthCY = snoutY + snoutDir.dr * snoutRadius * 0.45;
        double mouthSpan = snoutRadius * 0.55;
        Line mouth = new Line(
            mouthCX - perpX * mouthSpan, mouthCY - perpY * mouthSpan,
            mouthCX + perpX * mouthSpan, mouthCY + perpY * mouthSpan
        );
        mouth.setStroke(bodyColor.darker().darker().darker());
        mouth.setStrokeWidth(1.0);
        group.getChildren().add(mouth);

        // --- Nostrils ---
        Circle nostrilLeft  = buildNostril(perpX, perpY, snoutX, snoutY, snoutRadius, -1);
        Circle nostrilRight = buildNostril(perpX, perpY, snoutX, snoutY, snoutRadius,  1);
        group.getChildren().addAll(nostrilLeft, nostrilRight);

        // --- Eyes ---
        double eyeR = Math.max(1.5, radius * 0.13);
        double eyeForward = radius * 0.25;
        double eyeSide = radius * 0.32;
        double eyeBaseX = snoutDir.dc * eyeForward;
        double eyeBaseY = snoutDir.dr * eyeForward;
        double eyeLX = eyeBaseX - perpX * eyeSide;
        double eyeLY = eyeBaseY - perpY * eyeSide;
        double eyeRX = eyeBaseX + perpX * eyeSide;
        double eyeRY = eyeBaseY + perpY * eyeSide;
        Circle eyeLW = new Circle(eyeLX, eyeLY, eyeR, Color.WHITE);
        eyeLW.setStroke(Color.rgb(40, 20, 10));
        eyeLW.setStrokeWidth(0.6);
        Circle eyeRW = new Circle(eyeRX, eyeRY, eyeR, Color.WHITE);
        eyeRW.setStroke(Color.rgb(40, 20, 10));
        eyeRW.setStrokeWidth(0.6);
        // Pupils nudged slightly toward facing direction
        double pupilOff = eyeR * 0.35;
        Circle pupilL = new Circle(eyeLX + snoutDir.dc * pupilOff, eyeLY + snoutDir.dr * pupilOff,
                                   eyeR * 0.55, Color.rgb(20, 20, 20));
        Circle pupilR = new Circle(eyeRX + snoutDir.dc * pupilOff, eyeRY + snoutDir.dr * pupilOff,
                                   eyeR * 0.55, Color.rgb(20, 20, 20));
        group.getChildren().addAll(eyeLW, eyeRW, pupilL, pupilR);

        // --- Identity marker (top-most) ---
        addIdentityMarker(group, name, radius, snoutDir, perpX, perpY,
                          eyeLX, eyeLY, eyeRX, eyeRY);

        return group;
    }

    // -------------------------------------------------------------------------
    // Identity markers
    // -------------------------------------------------------------------------

    private static void addIdentityMarker(Group group, String name, double radius,
                                          Direction snoutDir, double perpX, double perpY,
                                          double eyeLX, double eyeLY, double eyeRX, double eyeRY) {
        if (name == null) return;
        switch (name) {
            case "Player":   addPlayerFlower(group, radius, snoutDir, perpX, perpY); break;
            case "Hoggart":  addHoggartLeaf(group, radius, snoutDir);                break;
            case "Whiskers": addWhiskerLines(group, radius, snoutDir, perpX, perpY); break;
            case "Bramble":  addBrambleBrow(group, eyeLX, eyeLY, eyeRX, eyeRY,
                                            snoutDir, perpX, perpY, radius);        break;
            default: /* no marker */                                                 break;
        }
    }

    /** Player: 4-petal flower above the head — friendly protagonist read. */
    private static void addPlayerFlower(Group group, double radius,
                                        Direction snoutDir, double perpX, double perpY) {
        double cx = -snoutDir.dc * radius * 0.95;
        double cy = -snoutDir.dr * radius * 0.95;
        double pr = Math.max(1.4, radius * 0.13);
        Color petal = Color.rgb(255, 220, 120);
        Color core  = Color.rgb(255, 80, 100);
        // Forward / back petals
        Circle pF = new Circle(cx + snoutDir.dc * pr * 1.4, cy + snoutDir.dr * pr * 1.4, pr, petal);
        Circle pB = new Circle(cx - snoutDir.dc * pr * 1.4, cy - snoutDir.dr * pr * 1.4, pr, petal);
        Circle pL = new Circle(cx - perpX * pr * 1.4, cy - perpY * pr * 1.4, pr, petal);
        Circle pR = new Circle(cx + perpX * pr * 1.4, cy + perpY * pr * 1.4, pr, petal);
        Circle center = new Circle(cx, cy, pr * 0.7, core);
        for (Circle c : new Circle[]{pF, pB, pL, pR}) {
            c.setStroke(petal.darker());
            c.setStrokeWidth(0.5);
        }
        group.getChildren().addAll(pF, pB, pL, pR, center);
    }

    /** Hoggart: single leaf "antenna" sticking out above the head — derpy-but-eager read. */
    private static void addHoggartLeaf(Group group, double radius, Direction snoutDir) {
        double baseX = -snoutDir.dc * radius * 0.85;
        double baseY = -snoutDir.dr * radius * 0.85;
        double tipX  = -snoutDir.dc * radius * 1.45;
        double tipY  = -snoutDir.dr * radius * 1.45;
        Line stem = new Line(baseX, baseY, tipX, tipY);
        stem.setStroke(Color.rgb(70, 50, 20));
        stem.setStrokeWidth(1.2);
        Ellipse leaf = new Ellipse(tipX, tipY, radius * 0.22, radius * 0.13);
        // Rotate ellipse to align with the stem direction
        double angDeg = Math.toDegrees(Math.atan2(snoutDir.dr, snoutDir.dc));
        leaf.setRotate(angDeg + 90);
        leaf.setFill(Color.rgb(80, 170, 70));
        leaf.setStroke(Color.rgb(40, 110, 40));
        leaf.setStrokeWidth(0.8);
        group.getChildren().addAll(stem, leaf);
    }

    /** Whiskers: 3 fine whisker lines on each side of the snout — cunning trickster read. */
    private static void addWhiskerLines(Group group, double radius,
                                        Direction snoutDir, double perpX, double perpY) {
        double snoutR = radius * 0.35;
        double snoutCX = snoutDir.dc * radius * 0.65;
        double snoutCY = snoutDir.dr * radius * 0.65;
        double whiskerLen = radius * 0.55;
        for (int side = -1; side <= 1; side += 2) {
            double rootX = snoutCX + perpX * snoutR * 0.7 * side;
            double rootY = snoutCY + perpY * snoutR * 0.7 * side;
            for (int j = -1; j <= 1; j++) {
                double tipX = rootX + perpX * whiskerLen * side
                            + snoutDir.dc * j * radius * 0.18;
                double tipY = rootY + perpY * whiskerLen * side
                            + snoutDir.dr * j * radius * 0.18;
                Line w = new Line(rootX, rootY, tipX, tipY);
                w.setStroke(Color.rgb(40, 30, 20, 0.8));
                w.setStrokeWidth(0.7);
                group.getChildren().add(w);
            }
        }
    }

    /** Bramble: V-shaped angry brow that meets between the eyes — bully read. */
    private static void addBrambleBrow(Group group,
                                       double eyeLX, double eyeLY, double eyeRX, double eyeRY,
                                       Direction snoutDir, double perpX, double perpY, double radius) {
        // Brow sits slightly above the eyes (away from snout direction)
        double back = -radius * 0.20;
        double cx = (eyeLX + eyeRX) / 2.0 + snoutDir.dc * back;
        double cy = (eyeLY + eyeRY) / 2.0 + snoutDir.dr * back;
        // Outer ends are above the eyes (further back from snout)
        double outerBack = -radius * 0.35;
        double outerLX = eyeLX + snoutDir.dc * outerBack - perpX * radius * 0.05;
        double outerLY = eyeLY + snoutDir.dr * outerBack - perpY * radius * 0.05;
        double outerRX = eyeRX + snoutDir.dc * outerBack + perpX * radius * 0.05;
        double outerRY = eyeRY + snoutDir.dr * outerBack + perpY * radius * 0.05;

        Line browL = new Line(outerLX, outerLY, cx, cy);
        Line browR = new Line(outerRX, outerRY, cx, cy);
        for (Line l : new Line[]{browL, browR}) {
            l.setStroke(Color.rgb(20, 20, 20));
            l.setStrokeWidth(1.6);
        }
        group.getChildren().addAll(browL, browR);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Circle buildNostril(double perpX, double perpY,
                                       double snoutCX, double snoutCY,
                                       double snoutR, int side) {
        double nostrilR = snoutR * 0.22;
        double nx = snoutCX + perpX * snoutR * 0.35 * side;
        double ny = snoutCY + perpY * snoutR * 0.35 * side;
        Circle nostril = new Circle(nx, ny, nostrilR);
        nostril.setFill(Color.rgb(80, 40, 40, 0.7));
        return nostril;
    }
}
