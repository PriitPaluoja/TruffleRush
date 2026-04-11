package com.example.demo.render;

import com.example.demo.core.GameSession;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Full-screen main menu overlay with title, high score, controls, and start button.
 */
public class MainMenuOverlay {

    private final Group group = new Group();
    private final Text highScoreText;
    private Runnable onStart;

    private final int width;
    private final int height;

    public MainMenuOverlay(int width, int height) {
        this.width = width;
        this.height = height;

        // Dark background
        Rectangle backdrop = new Rectangle(0, 0, width, height);
        backdrop.setFill(Color.rgb(10, 15, 25, 0.95));
        group.getChildren().add(backdrop);

        // Title
        Text title = new Text("TruffleRush");
        title.setFont(Font.font("System", FontWeight.BOLD, 64));
        title.setFill(Color.rgb(255, 215, 0));
        centerText(title, height * 0.22);
        group.getChildren().add(title);

        // Subtitle
        Text subtitle = new Text("Become the heaviest pig!");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 18));
        subtitle.setFill(Color.rgb(200, 200, 180));
        centerText(subtitle, height * 0.30);
        group.getChildren().add(subtitle);

        // High score
        highScoreText = new Text("High Score: 0");
        highScoreText.setFont(Font.font("System", FontWeight.BOLD, 20));
        highScoreText.setFill(Color.rgb(255, 240, 100));
        centerText(highScoreText, height * 0.40);
        group.getChildren().add(highScoreText);

        // Controls
        String[] controls = {
            "Arrow Keys — Move",
            "Space — Sniff (reveals hidden items)",
            "Collect truffles, avoid hazards, survive!"
        };
        double cy = height * 0.50;
        for (String line : controls) {
            Text t = new Text(line);
            t.setFont(Font.font("System", FontWeight.NORMAL, 14));
            t.setFill(Color.rgb(160, 170, 180));
            centerText(t, cy);
            group.getChildren().add(t);
            cy += 22;
        }

        // Start button
        double btnW = 220, btnH = 50;
        double btnX = (width - btnW) / 2.0;
        double btnY = height * 0.72;

        Rectangle btnBg = new Rectangle(btnX, btnY, btnW, btnH);
        btnBg.setFill(Color.rgb(60, 140, 60));
        btnBg.setStroke(Color.rgb(30, 100, 30));
        btnBg.setStrokeWidth(2);
        btnBg.setArcWidth(14);
        btnBg.setArcHeight(14);

        Text btnText = new Text("Start Game");
        btnText.setFont(Font.font("System", FontWeight.BOLD, 24));
        btnText.setFill(Color.WHITE);
        centerText(btnText, btnY + 34);

        btnBg.setOnMouseEntered(e -> btnBg.setFill(Color.rgb(80, 170, 80)));
        btnBg.setOnMouseExited(e -> btnBg.setFill(Color.rgb(60, 140, 60)));
        btnBg.setOnMouseClicked(e -> { if (onStart != null) onStart.run(); });
        btnText.setOnMouseClicked(e -> { if (onStart != null) onStart.run(); });

        group.getChildren().addAll(btnBg, btnText);

        // Version hint
        Text version = new Text("Levels, wolves, farmers, power-ups & more!");
        version.setFont(Font.font("System", FontWeight.NORMAL, 11));
        version.setFill(Color.rgb(100, 110, 120));
        centerText(version, height * 0.88);
        group.getChildren().add(version);

        group.setVisible(true);
    }

    public Group getGroup() { return group; }

    public void show() {
        highScoreText.setText("High Score: " + GameSession.getHighScore());
        centerText(highScoreText, height * 0.40);
        group.setVisible(true);
    }

    public void hide() {
        group.setVisible(false);
    }

    public void setOnStart(Runnable action) {
        this.onStart = action;
    }

    private void centerText(Text text, double y) {
        double charW = text.getFont().getSize() * 0.55;
        double textW = text.getText().length() * charW;
        text.setX((width - textW) / 2.0);
        text.setY(y);
    }
}
