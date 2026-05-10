package com.example.demo.render;

import com.example.demo.entity.Wolf;
import com.example.demo.world.GameMap;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * Renders a wolf as a layered top-down silhouette: shadow, tail, four legs,
 * elongated body, chest highlight, head with snout and fangs, ears with inner
 * pink fill, and yellow eyes with vertical slit pupils.
 *
 * <p>The wolf always points "up" (toward decreasing y) — matches the previous
 * fixed-orientation diamond and keeps the renderer free of facing state.
 */
public class WolfRenderer {

    private static final int TILE = GameMap.TILE_SIZE;
    private final Group group = new Group();

    public Group getGroup() { return group; }

    public void update(Wolf wolf) {
        group.getChildren().clear();
        if (wolf == null || !wolf.isActive()) return;

        double cx = wolf.getCol() * TILE + TILE / 2.0;
        double cy = wolf.getRow() * TILE + TILE / 2.0;

        Color furBase = Color.rgb(100, 100, 110);
        Color furDark = Color.rgb(60, 60, 70);
        Color furLight = Color.rgb(140, 140, 150);
        Color innerEar = Color.rgb(220, 140, 150);
        Color claw = Color.rgb(40, 40, 45);

        // Soft shadow under feet
        Ellipse shadow = new Ellipse(cx, cy + 14, 14, 3.5);
        shadow.setFill(Color.rgb(0, 0, 0, 0.22));

        // Tail (curved back, rendered below body)
        Polygon tail = new Polygon(
            cx - 1, cy + 10,
            cx + 1, cy + 10,
            cx + 5, cy + 18,
            cx + 2, cy + 22,
            cx - 2, cy + 22,
            cx - 5, cy + 18
        );
        tail.setFill(furDark);
        tail.setStroke(furDark.darker());
        tail.setStrokeWidth(0.6);

        // Four legs (rectangles at the corners of the body)
        Rectangle frontLeftLeg  = new Rectangle(cx - 9, cy - 6, 3.5, 8);
        Rectangle frontRightLeg = new Rectangle(cx + 5.5, cy - 6, 3.5, 8);
        Rectangle backLeftLeg   = new Rectangle(cx - 9, cy + 4, 3.5, 8);
        Rectangle backRightLeg  = new Rectangle(cx + 5.5, cy + 4, 3.5, 8);
        for (Rectangle r : new Rectangle[]{frontLeftLeg, frontRightLeg, backLeftLeg, backRightLeg}) {
            r.setFill(furDark);
            r.setStroke(furDark.darker());
            r.setStrokeWidth(0.4);
        }
        // Tiny claws at the bottom of each leg
        Circle clawFL = new Circle(cx - 7.25, cy + 2.5, 1.0, claw);
        Circle clawFR = new Circle(cx + 7.25, cy + 2.5, 1.0, claw);
        Circle clawBL = new Circle(cx - 7.25, cy + 12.5, 1.0, claw);
        Circle clawBR = new Circle(cx + 7.25, cy + 12.5, 1.0, claw);

        // Elongated body (8-vertex polygon)
        Polygon body = new Polygon(
            cx - 8, cy - 8,
            cx - 9, cy + 0,
            cx - 8, cy + 8,
            cx - 3, cy + 11,
            cx + 3, cy + 11,
            cx + 8, cy + 8,
            cx + 9, cy + 0,
            cx + 8, cy - 8
        );
        body.setFill(furBase);
        body.setStroke(furDark);
        body.setStrokeWidth(1.4);

        // Chest highlight (lighter ellipse on the upper body)
        Ellipse chest = new Ellipse(cx, cy - 1, 5, 4);
        chest.setFill(furLight);
        chest.setOpacity(0.55);

        // Two shoulder fur tuft lines
        Line tuftL = new Line(cx - 7, cy - 4, cx - 4, cy - 7);
        Line tuftR = new Line(cx + 7, cy - 4, cx + 4, cy - 7);
        for (Line l : new Line[]{tuftL, tuftR}) {
            l.setStroke(furDark.darker());
            l.setStrokeWidth(0.8);
        }

        // Head (separate polygon, sits above the body)
        Polygon head = new Polygon(
            cx - 6, cy - 8,
            cx - 7, cy - 14,
            cx - 3, cy - 17,
            cx + 3, cy - 17,
            cx + 7, cy - 14,
            cx + 6, cy - 8
        );
        head.setFill(furBase);
        head.setStroke(furDark);
        head.setStrokeWidth(1.2);

        // Snout (small triangle pointing up from the head)
        Polygon snout = new Polygon(
            cx - 3, cy - 16,
            cx + 3, cy - 16,
            cx + 1.5, cy - 21,
            cx - 1.5, cy - 21
        );
        snout.setFill(furDark);
        snout.setStroke(furDark.darker());
        snout.setStrokeWidth(0.6);

        // Two fangs (tiny white triangles below the snout)
        Polygon fangL = new Polygon(
            cx - 1.8, cy - 16,
            cx - 0.6, cy - 16,
            cx - 1.2, cy - 13.5
        );
        Polygon fangR = new Polygon(
            cx + 0.6, cy - 16,
            cx + 1.8, cy - 16,
            cx + 1.2, cy - 13.5
        );
        for (Polygon p : new Polygon[]{fangL, fangR}) {
            p.setFill(Color.WHITE);
            p.setStroke(Color.rgb(180, 180, 180));
            p.setStrokeWidth(0.3);
        }

        // Ears (two pointy triangles at the top of the head)
        Polygon leftEar = new Polygon(
            cx - 7, cy - 12,
            cx - 9, cy - 19,
            cx - 4, cy - 14
        );
        Polygon rightEar = new Polygon(
            cx + 7, cy - 12,
            cx + 9, cy - 19,
            cx + 4, cy - 14
        );
        for (Polygon ear : new Polygon[]{leftEar, rightEar}) {
            ear.setFill(furDark);
            ear.setStroke(furDark.darker());
            ear.setStrokeWidth(0.5);
        }
        // Inner-ear pink fill
        Polygon innerL = new Polygon(
            cx - 6.5, cy - 13,
            cx - 7.5, cy - 17,
            cx - 5, cy - 14
        );
        Polygon innerR = new Polygon(
            cx + 6.5, cy - 13,
            cx + 7.5, cy - 17,
            cx + 5, cy - 14
        );
        for (Polygon p : new Polygon[]{innerL, innerR}) p.setFill(innerEar);

        // Eyes: yellow sclera + vertical slit pupils
        Circle eyeL = new Circle(cx - 3, cy - 12, 2.0, Color.rgb(240, 200, 60));
        Circle eyeR = new Circle(cx + 3, cy - 12, 2.0, Color.rgb(240, 200, 60));
        for (Circle e : new Circle[]{eyeL, eyeR}) {
            e.setStroke(Color.rgb(120, 80, 20));
            e.setStrokeWidth(0.4);
        }
        Polygon pupilL = new Polygon(
            cx - 3.4, cy - 13.4,
            cx - 2.6, cy - 13.4,
            cx - 2.6, cy - 10.6,
            cx - 3.4, cy - 10.6
        );
        Polygon pupilR = new Polygon(
            cx + 2.6, cy - 13.4,
            cx + 3.4, cy - 13.4,
            cx + 3.4, cy - 10.6,
            cx + 2.6, cy - 10.6
        );
        for (Polygon p : new Polygon[]{pupilL, pupilR}) p.setFill(Color.rgb(15, 15, 15));

        group.getChildren().addAll(
            shadow,
            tail,
            frontLeftLeg, frontRightLeg, backLeftLeg, backRightLeg,
            clawFL, clawFR, clawBL, clawBR,
            body,
            chest,
            tuftL, tuftR,
            head,
            snout, fangL, fangR,
            leftEar, rightEar, innerL, innerR,
            eyeL, eyeR, pupilL, pupilR
        );
    }
}
