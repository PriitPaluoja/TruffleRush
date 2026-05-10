package com.example.demo.render;

import com.example.demo.entity.Farmer;
import com.example.demo.world.GameMap;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * Renders the farmer as a layered humanoid silhouette: hat (brim, top, red
 * band), head, beard and mustache, eyes with pupils, denim torso with
 * suspenders and plaid cross-stitching, two arms, two legs in boots, and a
 * pitchfork held in the right hand. The escape hole still renders as the
 * existing green circles below the body group.
 */
public class FarmerRenderer {

    private static final int TILE = GameMap.TILE_SIZE;
    private final Group group = new Group();

    public Group getGroup() { return group; }

    public void update(Farmer farmer) {
        group.getChildren().clear();
        if (farmer == null || !farmer.isActive()) return;

        double cx = farmer.getCol() * TILE + TILE / 2.0;
        double cy = farmer.getRow() * TILE + TILE / 2.0;

        Color skin     = Color.rgb(232, 195, 158);
        Color hatBrown = Color.rgb(120, 75, 30);
        Color hatDark  = Color.rgb(80, 50, 15);
        Color hatBand  = Color.rgb(170, 50, 40);
        Color denim    = Color.rgb(60, 90, 150);
        Color denimDk  = Color.rgb(35, 55, 100);
        Color suspender= Color.rgb(70, 50, 25);
        Color bootDk   = Color.rgb(40, 25, 15);
        Color forkSh   = Color.rgb(150, 110, 60);
        Color forkTine = Color.rgb(180, 180, 190);

        // Escape hole (rendered first so it sits below the farmer)
        double ehx = farmer.getEscapeCol() * TILE + TILE / 2.0;
        double ehy = farmer.getEscapeRow() * TILE + TILE / 2.0;
        Circle hole = new Circle(ehx, ehy, 12, Color.rgb(0, 200, 0, 0.3));
        hole.setStroke(Color.rgb(0, 180, 0, 0.8));
        hole.setStrokeWidth(2);
        Circle holeInner = new Circle(ehx, ehy, 6, Color.rgb(0, 150, 0, 0.5));
        group.getChildren().addAll(hole, holeInner);

        // Foot shadow
        Ellipse footShadow = new Ellipse(cx, cy + 12, 11, 2.8);
        footShadow.setFill(Color.rgb(0, 0, 0, 0.22));

        // Legs (two denim rectangles below the torso)
        Rectangle legL = new Rectangle(cx - 5.5, cy + 4, 4, 8);
        Rectangle legR = new Rectangle(cx + 1.5, cy + 4, 4, 8);
        for (Rectangle r : new Rectangle[]{legL, legR}) {
            r.setFill(denim);
            r.setStroke(denimDk);
            r.setStrokeWidth(0.5);
        }
        // Boots (wider darker rectangles at the bottom)
        Rectangle bootL = new Rectangle(cx - 6.5, cy + 11, 6, 3.5);
        Rectangle bootR = new Rectangle(cx + 0.5, cy + 11, 6, 3.5);
        for (Rectangle r : new Rectangle[]{bootL, bootR}) {
            r.setFill(bootDk);
            r.setStroke(bootDk.darker());
            r.setStrokeWidth(0.5);
        }

        // Torso (denim shirt rectangle)
        Rectangle torso = new Rectangle(cx - 7, cy - 3, 14, 9);
        torso.setFill(denim);
        torso.setStroke(denimDk);
        torso.setStrokeWidth(1.0);

        // Suspenders (two darker vertical lines)
        Line suspL = new Line(cx - 3, cy - 3, cx - 3, cy + 6);
        Line suspR = new Line(cx + 3, cy - 3, cx + 3, cy + 6);
        for (Line s : new Line[]{suspL, suspR}) {
            s.setStroke(suspender);
            s.setStrokeWidth(1.4);
        }
        // Plaid cross-stitches on the torso
        Line plaidH = new Line(cx - 7, cy + 1.5, cx + 7, cy + 1.5);
        plaidH.setStroke(denimDk);
        plaidH.setStrokeWidth(0.5);

        // Arms (two skin-colored rectangles flanking the torso)
        Rectangle armL = new Rectangle(cx - 10, cy - 2, 3, 8);
        Rectangle armR = new Rectangle(cx + 7, cy - 2, 3, 8);
        for (Rectangle r : new Rectangle[]{armL, armR}) {
            r.setFill(skin);
            r.setStroke(skin.darker());
            r.setStrokeWidth(0.5);
        }

        // Pitchfork (held in the right hand, pointing up-right)
        Rectangle forkShaft = new Rectangle(cx + 9.5, cy - 14, 1.6, 16);
        forkShaft.setFill(forkSh);
        forkShaft.setStroke(forkSh.darker());
        forkShaft.setStrokeWidth(0.4);
        Polygon forkBase = new Polygon(
            cx + 8.5,  cy - 13.5,
            cx + 11.6, cy - 13.5,
            cx + 11.0, cy - 11.5,
            cx + 9.0,  cy - 11.5
        );
        forkBase.setFill(forkTine.darker());
        forkBase.setStroke(forkSh.darker());
        forkBase.setStrokeWidth(0.4);
        Line tine1 = new Line(cx + 9.0,  cy - 13.5, cx + 8.0,  cy - 18);
        Line tine2 = new Line(cx + 10.3, cy - 13.5, cx + 10.3, cy - 18);
        Line tine3 = new Line(cx + 11.5, cy - 13.5, cx + 12.5, cy - 18);
        for (Line t : new Line[]{tine1, tine2, tine3}) {
            t.setStroke(forkTine);
            t.setStrokeWidth(1.2);
        }

        // Head (skin circle)
        Circle head = new Circle(cx, cy - 9, 6, skin);
        head.setStroke(skin.darker());
        head.setStrokeWidth(0.7);

        // Beard (grey wedge below the chin)
        Polygon beard = new Polygon(
            cx - 5, cy - 7,
            cx + 5, cy - 7,
            cx + 4, cy - 4,
            cx + 1.5, cy - 3,
            cx - 1.5, cy - 3,
            cx - 4, cy - 4
        );
        beard.setFill(Color.rgb(200, 200, 200));
        beard.setStroke(Color.rgb(160, 160, 160));
        beard.setStrokeWidth(0.5);

        // Mustache
        Line mustache = new Line(cx - 3, cy - 8, cx + 3, cy - 8);
        mustache.setStroke(Color.rgb(170, 170, 170));
        mustache.setStrokeWidth(1.6);

        // Mouth (small dark line under mustache)
        Line mouth = new Line(cx - 1.5, cy - 6.5, cx + 1.5, cy - 6.5);
        mouth.setStroke(Color.rgb(100, 60, 50));
        mouth.setStrokeWidth(0.7);

        // Eyes: white sclera + black pupils
        Circle eyeLW = new Circle(cx - 2.4, cy - 10.5, 1.4, Color.WHITE);
        Circle eyeRW = new Circle(cx + 2.4, cy - 10.5, 1.4, Color.WHITE);
        for (Circle e : new Circle[]{eyeLW, eyeRW}) {
            e.setStroke(Color.rgb(70, 50, 40));
            e.setStrokeWidth(0.4);
        }
        Circle pupilL = new Circle(cx - 2.4, cy - 10.5, 0.7, Color.rgb(20, 20, 20));
        Circle pupilR = new Circle(cx + 2.4, cy - 10.5, 0.7, Color.rgb(20, 20, 20));

        // Hat brim (over the head)
        Rectangle brim = new Rectangle(cx - 12, cy - 16, 24, 4);
        brim.setFill(hatDark);
        brim.setStroke(hatDark.darker());
        brim.setStrokeWidth(0.5);
        // Hat top
        Rectangle hatTop = new Rectangle(cx - 7, cy - 24, 14, 10);
        hatTop.setFill(hatBrown);
        hatTop.setStroke(hatDark);
        hatTop.setStrokeWidth(0.8);
        // Hat band (red stripe across the base of the top)
        Rectangle band = new Rectangle(cx - 7, cy - 16.5, 14, 2);
        band.setFill(hatBand);

        group.getChildren().addAll(
            footShadow,
            legL, legR, bootL, bootR,
            torso, suspL, suspR, plaidH,
            armL, armR,
            forkShaft, forkBase, tine1, tine2, tine3,
            head, beard, mustache, mouth,
            eyeLW, eyeRW, pupilL, pupilR,
            brim, hatTop, band
        );
    }
}
