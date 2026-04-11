package com.example.demo.render;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Full-screen game over overlay showing death reason, final score, level reached,
 * and a "Main Menu" button.
 */
public class GameOverOverlay {

    private final Group group = new Group();
    private final Text reasonText;
    private final Text scoreText;
    private final Text levelText;
    private Runnable onMainMenu;

    private final int width;
    private final int height;

    public GameOverOverlay(int width, int height) {
        this.width = width;
        this.height = height;

        Rectangle backdrop = new Rectangle(0, 0, width, height);
        backdrop.setFill(Color.rgb(30, 0, 0, 0.85));
        group.getChildren().add(backdrop);

        // "GAME OVER" title
        Text title = new Text("GAME OVER");
        title.setFont(Font.font("System", FontWeight.BOLD, 56));
        title.setFill(Color.rgb(255, 60, 60));
        centerText(title, height * 0.28);
        group.getChildren().add(title);

        // Death reason
        reasonText = new Text("");
        reasonText.setFont(Font.font("System", FontWeight.BOLD, 22));
        reasonText.setFill(Color.rgb(255, 200, 150));
        group.getChildren().add(reasonText);

        // Level reached
        levelText = new Text("");
        levelText.setFont(Font.font("System", FontWeight.BOLD, 18));
        levelText.setFill(Color.rgb(200, 200, 220));
        group.getChildren().add(levelText);

        // Final score
        scoreText = new Text("");
        scoreText.setFont(Font.font("System", FontWeight.BOLD, 24));
        scoreText.setFill(Color.rgb(255, 215, 0));
        group.getChildren().add(scoreText);

        // Main Menu button
        double btnW = 200, btnH = 46;
        double btnX = (width - btnW) / 2.0;
        double btnY = height * 0.70;

        Rectangle btnBg = new Rectangle(btnX, btnY, btnW, btnH);
        btnBg.setFill(Color.rgb(140, 60, 60));
        btnBg.setStroke(Color.rgb(100, 30, 30));
        btnBg.setStrokeWidth(2);
        btnBg.setArcWidth(12);
        btnBg.setArcHeight(12);

        Text btnText = new Text("Main Menu");
        btnText.setFont(Font.font("System", FontWeight.BOLD, 20));
        btnText.setFill(Color.WHITE);
        centerText(btnText, btnY + 30);

        btnBg.setOnMouseEntered(e -> btnBg.setFill(Color.rgb(170, 80, 80)));
        btnBg.setOnMouseExited(e -> btnBg.setFill(Color.rgb(140, 60, 60)));
        btnBg.setOnMouseClicked(e -> { if (onMainMenu != null) onMainMenu.run(); });
        btnText.setOnMouseClicked(e -> { if (onMainMenu != null) onMainMenu.run(); });

        group.getChildren().addAll(btnBg, btnText);
        group.setVisible(false);
    }

    public Group getGroup() { return group; }

    public void show(String reason, int level, int score) {
        reasonText.setText(reason);
        centerText(reasonText, height * 0.40);

        levelText.setText("Level Reached: " + level);
        centerText(levelText, height * 0.50);

        scoreText.setText("Final Score: " + score);
        centerText(scoreText, height * 0.58);

        group.setVisible(true);
    }

    public void hide() {
        group.setVisible(false);
    }

    public void setOnMainMenu(Runnable action) {
        this.onMainMenu = action;
    }

    private void centerText(Text text, double y) {
        double charW = text.getFont().getSize() * 0.55;
        double textW = text.getText().length() * charW;
        text.setX((width - textW) / 2.0);
        text.setY(y);
    }
}
