package com.example.demo.render;

import com.example.demo.entity.Direction;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
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

        // --- Tail (curl + tuft tip, opposite facing) ---
        double tailX = -snoutDir.dc * radius * 0.95;
        double tailY = -snoutDir.dr * radius * 0.95;
        Circle tailBase = new Circle(tailX, tailY, radius * 0.20);
        tailBase.setFill(bodyColor);
        tailBase.setStroke(bodyColor.darker());
        tailBase.setStrokeWidth(1.0);
        // Curly arc that extends a little farther from the body
        double curlX = tailX - snoutDir.dc * radius * 0.18;
        double curlY = tailY - snoutDir.dr * radius * 0.18;
        Arc tailCurl = new Arc(curlX, curlY, radius * 0.18, radius * 0.18, 0, 280);
        tailCurl.setType(ArcType.OPEN);
        tailCurl.setFill(Color.TRANSPARENT);
        tailCurl.setStroke(bodyColor.darker());
        tailCurl.setStrokeWidth(1.4);
        // Tiny tuft on the tip of the curl
        double tuftX = curlX + perpX * radius * 0.18;
        double tuftY = curlY + perpY * radius * 0.18;
        Circle tailTuft = new Circle(tuftX, tuftY, radius * 0.07);
        tailTuft.setFill(bodyColor.darker());
        tailTuft.setStroke(bodyColor.darker().darker());
        tailTuft.setStrokeWidth(0.5);
        group.getChildren().addAll(tailBase, tailCurl, tailTuft);

        // --- Legs: 4 small ovals + dark hoof band at the outer edge of each ---
        Color legColor = bodyColor.darker();
        Color hoofColor = Color.rgb(40, 25, 15);
        double legD = radius * 0.85;
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(45 + i * 90);
            double lx = Math.cos(a) * legD;
            double ly = Math.sin(a) * legD;
            Ellipse leg = new Ellipse(lx, ly, radius * 0.18, radius * 0.14);
            leg.setFill(legColor);
            leg.setStroke(legColor.darker());
            leg.setStrokeWidth(0.6);
            // Hoof band: smaller ellipse at the outer (away-from-body) end
            double hoofX = Math.cos(a) * (legD + radius * 0.10);
            double hoofY = Math.sin(a) * (legD + radius * 0.10);
            Ellipse hoof = new Ellipse(hoofX, hoofY, radius * 0.10, radius * 0.07);
            hoof.setFill(hoofColor);
            hoof.setStroke(hoofColor.darker());
            hoof.setStrokeWidth(0.4);
            group.getChildren().addAll(leg, hoof);
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
        // Inner-ear pink fill (smaller circle inside each ear)
        Color innerEar = Color.rgb(240, 170, 180);
        Circle innerEarLeft  = new Circle(earLeft.getCenterX(),  earLeft.getCenterY(),  earRadius * 0.55);
        Circle innerEarRight = new Circle(earRight.getCenterX(), earRight.getCenterY(), earRadius * 0.55);
        innerEarLeft.setFill(innerEar);
        innerEarRight.setFill(innerEar);
        group.getChildren().addAll(earLeft, earRight, innerEarLeft, innerEarRight);

        // --- Body ---
        Circle body = new Circle(0, 0, radius);
        body.setFill(bodyColor);
        body.setStroke(bodyColor.darker());
        body.setStrokeWidth(1.5);
        group.getChildren().add(body);

        // --- Belly highlight (lighter shade, slightly forward of center) ---
        Color belly = lighten(bodyColor, 0.18);
        double bellyX = snoutDir.dc * radius * 0.18;
        double bellyY = snoutDir.dr * radius * 0.18;
        Ellipse bellyHi = new Ellipse(bellyX, bellyY, radius * 0.62, radius * 0.45);
        bellyHi.setFill(belly);
        bellyHi.setOpacity(0.55);
        group.getChildren().add(bellyHi);

        // --- Back-fur tuft lines (2 short strokes opposite the facing direction) ---
        Color tuftColor = bodyColor.darker().darker();
        for (int t = -1; t <= 1; t += 2) {
            double rootX = -snoutDir.dc * radius * 0.55 + perpX * radius * 0.18 * t;
            double rootY = -snoutDir.dr * radius * 0.55 + perpY * radius * 0.18 * t;
            double tipX  = rootX - snoutDir.dc * radius * 0.14;
            double tipY  = rootY - snoutDir.dr * radius * 0.14;
            Line tuft = new Line(rootX, rootY, tipX, tipY);
            tuft.setStroke(tuftColor);
            tuft.setStrokeWidth(0.8);
            group.getChildren().add(tuft);
        }

        // --- Snout ---
        Circle snout = new Circle(snoutX, snoutY, snoutRadius);
        snout.setFill(bodyColor.darker());
        snout.setStroke(bodyColor.darker().darker());
        snout.setStrokeWidth(1.0);
        group.getChildren().add(snout);

        // --- Snout-top highlight (small lighter ellipse on the back side of the snout) ---
        double snoutHiX = snoutX - snoutDir.dc * snoutRadius * 0.25;
        double snoutHiY = snoutY - snoutDir.dr * snoutRadius * 0.25;
        Ellipse snoutHi = new Ellipse(snoutHiX, snoutHiY, snoutRadius * 0.55, snoutRadius * 0.30);
        snoutHi.setFill(lighten(bodyColor, 0.15));
        snoutHi.setOpacity(0.55);
        group.getChildren().add(snoutHi);

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

        // --- Tongue: tiny pink ellipse peeking under the mouth line ---
        double tongueCX = mouthCX + snoutDir.dc * snoutRadius * 0.10;
        double tongueCY = mouthCY + snoutDir.dr * snoutRadius * 0.10;
        Ellipse tongue = new Ellipse(tongueCX, tongueCY, snoutRadius * 0.20, snoutRadius * 0.12);
        tongue.setFill(Color.rgb(220, 100, 110));
        tongue.setStroke(Color.rgb(160, 60, 70));
        tongue.setStrokeWidth(0.4);
        group.getChildren().add(tongue);

        // --- Nostrils ---
        Circle nostrilLeft  = buildNostril(perpX, perpY, snoutX, snoutY, snoutRadius, -1);
        Circle nostrilRight = buildNostril(perpX, perpY, snoutX, snoutY, snoutRadius,  1);
        group.getChildren().addAll(nostrilLeft, nostrilRight);

        // --- Eyes (sclera + iris + pupil + highlight per side) ---
        double eyeR = Math.max(1.5, radius * 0.13);
        double eyeForward = radius * 0.25;
        double eyeSide = radius * 0.32;
        double eyeBaseX = snoutDir.dc * eyeForward;
        double eyeBaseY = snoutDir.dr * eyeForward;
        double eyeLX = eyeBaseX - perpX * eyeSide;
        double eyeLY = eyeBaseY - perpY * eyeSide;
        double eyeRX = eyeBaseX + perpX * eyeSide;
        double eyeRY = eyeBaseY + perpY * eyeSide;

        // --- Cheek blush (rendered before eyes so eyes overlay) ---
        Color blush = Color.rgb(255, 140, 160, 0.55);
        double blushOffX = -perpX * radius * 0.10;
        double blushOffY = -perpY * radius * 0.10;
        Circle cheekL = new Circle(eyeLX + blushOffX, eyeLY + blushOffY, radius * 0.10, blush);
        Circle cheekR = new Circle(eyeRX - blushOffX, eyeRY - blushOffY, radius * 0.10, blush);
        group.getChildren().addAll(cheekL, cheekR);

        Color irisColor = Color.rgb(80, 130, 70);
        double pupilOff = eyeR * 0.35;
        for (int side = 0; side < 2; side++) {
            double ex = (side == 0) ? eyeLX : eyeRX;
            double ey = (side == 0) ? eyeLY : eyeRY;
            // Sclera
            Circle sclera = new Circle(ex, ey, eyeR, Color.WHITE);
            sclera.setStroke(Color.rgb(40, 20, 10));
            sclera.setStrokeWidth(0.6);
            // Iris (colored ring) — slightly nudged toward facing
            Circle iris = new Circle(ex + snoutDir.dc * pupilOff * 0.6,
                                     ey + snoutDir.dr * pupilOff * 0.6,
                                     eyeR * 0.75, irisColor);
            // Pupil (black, fully nudged toward facing)
            Circle pupil = new Circle(ex + snoutDir.dc * pupilOff,
                                      ey + snoutDir.dr * pupilOff,
                                      eyeR * 0.42, Color.rgb(15, 15, 15));
            // Highlight (white, opposite the facing direction so it sits on the upper-back of pupil)
            Circle hi = new Circle(ex + snoutDir.dc * pupilOff - perpX * eyeR * 0.20,
                                   ey + snoutDir.dr * pupilOff - perpY * eyeR * 0.20,
                                   eyeR * 0.18, Color.WHITE);
            group.getChildren().addAll(sclera, iris, pupil, hi);
        }

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

    /** Player: 8-petal layered flower with stem above the head — friendly protagonist read. */
    private static void addPlayerFlower(Group group, double radius,
                                        Direction snoutDir, double perpX, double perpY) {
        double cx = -snoutDir.dc * radius * 0.95;
        double cy = -snoutDir.dr * radius * 0.95;
        double pr = Math.max(1.4, radius * 0.13);
        Color petal = Color.rgb(255, 220, 120);
        Color innerPetal = Color.rgb(230, 175, 80);
        Color core  = Color.rgb(255, 80, 100);
        Color coreHi = Color.rgb(255, 200, 60);

        // Stem from head to flower base
        double stemX = cx + snoutDir.dc * pr * 1.0;
        double stemY = cy + snoutDir.dr * pr * 1.0;
        Line stem = new Line(stemX, stemY,
                             stemX + snoutDir.dc * pr * 1.2,
                             stemY + snoutDir.dr * pr * 1.2);
        stem.setStroke(Color.rgb(60, 130, 60));
        stem.setStrokeWidth(0.9);
        group.getChildren().add(stem);

        // Outer 4 petals (axis-aligned)
        Circle pF = new Circle(cx + snoutDir.dc * pr * 1.4, cy + snoutDir.dr * pr * 1.4, pr, petal);
        Circle pB = new Circle(cx - snoutDir.dc * pr * 1.4, cy - snoutDir.dr * pr * 1.4, pr, petal);
        Circle pL = new Circle(cx - perpX * pr * 1.4, cy - perpY * pr * 1.4, pr, petal);
        Circle pR = new Circle(cx + perpX * pr * 1.4, cy + perpY * pr * 1.4, pr, petal);
        for (Circle c : new Circle[]{pF, pB, pL, pR}) {
            c.setStroke(petal.darker());
            c.setStrokeWidth(0.5);
        }
        group.getChildren().addAll(pF, pB, pL, pR);

        // Inner 4 petals at 45° offsets, smaller and darker for depth
        double dpr = pr * 0.78;
        double dOff = pr * 1.1;
        double[] dx = { snoutDir.dc + perpX,  snoutDir.dc - perpX, -snoutDir.dc + perpX, -snoutDir.dc - perpX };
        double[] dy = { snoutDir.dr + perpY,  snoutDir.dr - perpY, -snoutDir.dr + perpY, -snoutDir.dr - perpY };
        for (int i = 0; i < 4; i++) {
            double n = Math.sqrt(dx[i] * dx[i] + dy[i] * dy[i]);
            if (n < 0.001) continue;
            double ux = dx[i] / n, uy = dy[i] / n;
            Circle inner = new Circle(cx + ux * dOff, cy + uy * dOff, dpr, innerPetal);
            inner.setStroke(innerPetal.darker());
            inner.setStrokeWidth(0.4);
            group.getChildren().add(inner);
        }

        // Layered core (outer red + inner gold dot)
        Circle center = new Circle(cx, cy, pr * 0.75, core);
        Circle centerHi = new Circle(cx, cy, pr * 0.32, coreHi);
        group.getChildren().addAll(center, centerHi);
    }

    /** Hoggart: leaf "antenna" with midrib + side veins + dewdrop — derpy-but-eager read. */
    private static void addHoggartLeaf(Group group, double radius, Direction snoutDir) {
        double baseX = -snoutDir.dc * radius * 0.85;
        double baseY = -snoutDir.dr * radius * 0.85;
        double tipX  = -snoutDir.dc * radius * 1.45;
        double tipY  = -snoutDir.dr * radius * 1.45;
        Line stem = new Line(baseX, baseY, tipX, tipY);
        stem.setStroke(Color.rgb(70, 50, 20));
        stem.setStrokeWidth(1.2);
        // Leaf body
        Ellipse leaf = new Ellipse(tipX, tipY, radius * 0.22, radius * 0.13);
        double angDeg = Math.toDegrees(Math.atan2(snoutDir.dr, snoutDir.dc));
        leaf.setRotate(angDeg + 90);
        leaf.setFill(Color.rgb(80, 170, 70));
        leaf.setStroke(Color.rgb(40, 110, 40));
        leaf.setStrokeWidth(0.8);
        // Midrib (along the leaf's long axis)
        double leafLen = radius * 0.22;
        double midRX = -snoutDir.dc * leafLen * 0.85;
        double midRY = -snoutDir.dr * leafLen * 0.85;
        Line midrib = new Line(tipX - midRX, tipY - midRY, tipX + midRX, tipY + midRY);
        midrib.setStroke(Color.rgb(40, 110, 40));
        midrib.setStrokeWidth(0.6);
        // Two side veins crossing the midrib (perpendicular to the snout axis)
        double pX, pY;
        if (snoutDir == Direction.UP || snoutDir == Direction.DOWN) { pX = 1; pY = 0; }
        else { pX = 0; pY = 1; }
        double veinSpan = radius * 0.10;
        Line vein1 = new Line(
            tipX - snoutDir.dc * leafLen * 0.30 - pX * veinSpan,
            tipY - snoutDir.dr * leafLen * 0.30 - pY * veinSpan,
            tipX - snoutDir.dc * leafLen * 0.30 + pX * veinSpan,
            tipY - snoutDir.dr * leafLen * 0.30 + pY * veinSpan
        );
        Line vein2 = new Line(
            tipX + snoutDir.dc * leafLen * 0.30 - pX * veinSpan,
            tipY + snoutDir.dr * leafLen * 0.30 - pY * veinSpan,
            tipX + snoutDir.dc * leafLen * 0.30 + pX * veinSpan,
            tipY + snoutDir.dr * leafLen * 0.30 + pY * veinSpan
        );
        for (Line v : new Line[]{vein1, vein2}) {
            v.setStroke(Color.rgb(40, 110, 40, 0.8));
            v.setStrokeWidth(0.5);
        }
        // Tiny dewdrop highlight near the leaf tip
        Circle dew = new Circle(
            tipX + pX * radius * 0.04 - snoutDir.dc * leafLen * 0.40,
            tipY + pY * radius * 0.04 - snoutDir.dr * leafLen * 0.40,
            radius * 0.045, Color.rgb(220, 240, 230, 0.85));
        group.getChildren().addAll(stem, leaf, midrib, vein1, vein2, dew);
    }

    /** Whiskers: 5 fine whiskers per side + small eyebrows — cunning trickster read. */
    private static void addWhiskerLines(Group group, double radius,
                                        Direction snoutDir, double perpX, double perpY) {
        double snoutR = radius * 0.35;
        double snoutCX = snoutDir.dc * radius * 0.65;
        double snoutCY = snoutDir.dr * radius * 0.65;
        double whiskerLen = radius * 0.62;
        Color whisker = Color.rgb(40, 30, 20, 0.8);
        // 5 whiskers per side: j = -2..+2
        for (int side = -1; side <= 1; side += 2) {
            double rootX = snoutCX + perpX * snoutR * 0.7 * side;
            double rootY = snoutCY + perpY * snoutR * 0.7 * side;
            for (int j = -2; j <= 2; j++) {
                double lenScale = 1.0 - Math.abs(j) * 0.10; // outer whiskers shorter
                double tipX = rootX + perpX * whiskerLen * lenScale * side
                            + snoutDir.dc * j * radius * 0.13;
                double tipY = rootY + perpY * whiskerLen * lenScale * side
                            + snoutDir.dr * j * radius * 0.13;
                Line w = new Line(rootX, rootY, tipX, tipY);
                w.setStroke(whisker);
                w.setStrokeWidth(0.6);
                group.getChildren().add(w);
            }
        }
        // Two small eyebrow strokes above the eyes
        double eyeForward = radius * 0.25;
        double eyeSide = radius * 0.32;
        double eyeBaseX = snoutDir.dc * eyeForward;
        double eyeBaseY = snoutDir.dr * eyeForward;
        double browBack = -radius * 0.20;
        for (int s = -1; s <= 1; s += 2) {
            double bx = eyeBaseX + perpX * eyeSide * s + snoutDir.dc * browBack;
            double by = eyeBaseY + perpY * eyeSide * s + snoutDir.dr * browBack;
            Line brow = new Line(bx - perpX * radius * 0.08 * s, by - perpY * radius * 0.08 * s,
                                 bx + perpX * radius * 0.08 * s, by + perpY * radius * 0.08 * s);
            brow.setStroke(Color.rgb(60, 40, 20));
            brow.setStrokeWidth(1.0);
            group.getChildren().add(brow);
        }
    }

    /** Bramble: V-shaped angry brow + cheek scar + nicked ear — bully read. */
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

        // Scar across the right cheek (a short angled line near the snout)
        double scarCX = snoutDir.dc * radius * 0.45 + perpX * radius * 0.30;
        double scarCY = snoutDir.dr * radius * 0.45 + perpY * radius * 0.30;
        Line scar = new Line(
            scarCX - snoutDir.dc * radius * 0.08 - perpX * radius * 0.04,
            scarCY - snoutDir.dr * radius * 0.08 - perpY * radius * 0.04,
            scarCX + snoutDir.dc * radius * 0.08 + perpX * radius * 0.04,
            scarCY + snoutDir.dr * radius * 0.08 + perpY * radius * 0.04
        );
        scar.setStroke(Color.rgb(200, 60, 60));
        scar.setStrokeWidth(1.2);
        group.getChildren().add(scar);

        // Nick on the left ear (a small dark V notch)
        double earBackX = -snoutDir.dc * radius * 0.15;
        double earBackY = -snoutDir.dr * radius * 0.15;
        double earLX = earBackX - perpX * radius * 0.55;
        double earLY = earBackY - perpY * radius * 0.55;
        Line nick1 = new Line(earLX - perpX * radius * 0.06, earLY - perpY * radius * 0.06,
                              earLX, earLY + 0.001);
        Line nick2 = new Line(earLX, earLY + 0.001,
                              earLX - perpX * radius * 0.04 - snoutDir.dc * radius * 0.04,
                              earLY - perpY * radius * 0.04 - snoutDir.dr * radius * 0.04);
        for (Line n : new Line[]{nick1, nick2}) {
            n.setStroke(Color.rgb(20, 20, 20));
            n.setStrokeWidth(1.4);
        }
        group.getChildren().addAll(nick1, nick2);
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

    /** Mix toward white by {@code t} (0..1). */
    private static Color lighten(Color c, double t) {
        double r = c.getRed()   + (1.0 - c.getRed())   * t;
        double g = c.getGreen() + (1.0 - c.getGreen()) * t;
        double b = c.getBlue()  + (1.0 - c.getBlue())  * t;
        return new Color(clamp01(r), clamp01(g), clamp01(b), c.getOpacity());
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
