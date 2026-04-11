package com.example.demo.render;

import com.example.demo.entity.Farmer;
import com.example.demo.world.GameMap;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Renders a farmer as a brown rectangle body with a hat and escape hole indicator.
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

        // Body
        Rectangle body = new Rectangle(cx - 8, cy - 10, 16, 20);
        body.setFill(Color.rgb(139, 90, 43));
        body.setStroke(Color.rgb(100, 60, 20));
        body.setStrokeWidth(1.0);

        // Hat brim
        Rectangle brim = new Rectangle(cx - 12, cy - 14, 24, 4);
        brim.setFill(Color.rgb(100, 60, 20));

        // Hat top
        Rectangle hatTop = new Rectangle(cx - 7, cy - 22, 14, 10);
        hatTop.setFill(Color.rgb(120, 75, 30));
        hatTop.setStroke(Color.rgb(80, 50, 15));
        hatTop.setStrokeWidth(0.8);

        // Eyes
        Circle leftEye = new Circle(cx - 3, cy - 4, 1.5, Color.BLACK);
        Circle rightEye = new Circle(cx + 3, cy - 4, 1.5, Color.BLACK);

        // Escape hole (pulsing green circle at escape position)
        double ehx = farmer.getEscapeCol() * TILE + TILE / 2.0;
        double ehy = farmer.getEscapeRow() * TILE + TILE / 2.0;
        Circle hole = new Circle(ehx, ehy, 12, Color.rgb(0, 200, 0, 0.3));
        hole.setStroke(Color.rgb(0, 180, 0, 0.8));
        hole.setStrokeWidth(2);
        Circle holeInner = new Circle(ehx, ehy, 6, Color.rgb(0, 150, 0, 0.5));

        group.getChildren().addAll(hole, holeInner, body, brim, hatTop, leftEye, rightEye);
    }
}
