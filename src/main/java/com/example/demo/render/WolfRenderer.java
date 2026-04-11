package com.example.demo.render;

import com.example.demo.entity.Wolf;
import com.example.demo.world.GameMap;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * Renders a wolf as a gray diamond body with pointy ear triangles.
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

        // Diamond body
        Polygon body = new Polygon(
            cx, cy - 14,
            cx + 10, cy,
            cx, cy + 14,
            cx - 10, cy
        );
        body.setFill(Color.rgb(100, 100, 110));
        body.setStroke(Color.rgb(60, 60, 70));
        body.setStrokeWidth(1.5);

        // Left ear
        Polygon leftEar = new Polygon(
            cx - 8, cy - 10,
            cx - 12, cy - 20,
            cx - 2, cy - 12
        );
        leftEar.setFill(Color.rgb(80, 80, 90));

        // Right ear
        Polygon rightEar = new Polygon(
            cx + 8, cy - 10,
            cx + 12, cy - 20,
            cx + 2, cy - 12
        );
        rightEar.setFill(Color.rgb(80, 80, 90));

        // Eyes
        javafx.scene.shape.Circle leftEye = new javafx.scene.shape.Circle(cx - 4, cy - 3, 2, Color.rgb(255, 50, 50));
        javafx.scene.shape.Circle rightEye = new javafx.scene.shape.Circle(cx + 4, cy - 3, 2, Color.rgb(255, 50, 50));

        group.getChildren().addAll(body, leftEar, rightEar, leftEye, rightEye);
    }
}
