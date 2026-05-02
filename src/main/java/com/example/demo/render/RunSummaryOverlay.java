package com.example.demo.render;

import com.example.demo.core.Boon;
import com.example.demo.core.GameSession;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-run recap screen: final level reached, score, item totals, boons taken,
 * and how many truffles got banked. Shown between the game-over click and the
 * main menu transition so the player has a story for their run.
 */
public class RunSummaryOverlay {

    private final Group group = new Group();
    private final int width;
    private final int height;
    private Runnable onContinue;

    private final Text titleText;
    private final List<Text> dynamicLines = new ArrayList<>();

    public RunSummaryOverlay(int width, int height) {
        this.width = width;
        this.height = height;

        Rectangle backdrop = new Rectangle(0, 0, width, height);
        backdrop.setFill(Color.rgb(8, 12, 20, 0.96));
        group.getChildren().add(backdrop);

        titleText = new Text("Run Summary");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 38));
        titleText.setFill(Color.rgb(255, 215, 0));
        centerText(titleText, height * 0.18);
        group.getChildren().add(titleText);

        // Continue button
        double btnW = 200, btnH = 44;
        double btnX = (width - btnW) / 2.0;
        double btnY = height - 90;
        Rectangle btnBg = new Rectangle(btnX, btnY, btnW, btnH);
        btnBg.setFill(Color.rgb(60, 140, 60));
        btnBg.setStroke(Color.rgb(30, 100, 30));
        btnBg.setStrokeWidth(2);
        btnBg.setArcWidth(12);
        btnBg.setArcHeight(12);
        Text btnText = new Text("Main Menu");
        btnText.setFont(Font.font("System", FontWeight.BOLD, 20));
        btnText.setFill(Color.WHITE);
        centerText(btnText, btnY + 29);
        btnBg.setOnMouseEntered(e -> btnBg.setFill(Color.rgb(80, 170, 80)));
        btnBg.setOnMouseExited(e -> btnBg.setFill(Color.rgb(60, 140, 60)));
        btnBg.setOnMouseClicked(e -> { if (onContinue != null) onContinue.run(); });
        btnText.setOnMouseClicked(e -> { if (onContinue != null) onContinue.run(); });
        group.getChildren().addAll(btnBg, btnText);

        group.setVisible(false);
    }

    public Group getGroup() { return group; }

    public void setOnContinue(Runnable r) { this.onContinue = r; }

    public void show(GameSession session, int truffleBank, int truffleDeposit) {
        for (Text t : dynamicLines) group.getChildren().remove(t);
        dynamicLines.clear();

        List<String> lines = new ArrayList<>();
        lines.add("Reached:  Level " + session.getLevel());
        lines.add("Score:    " + session.getScore());
        lines.add("Items:    " + session.getItemsCollected() + "  truffles+");
        lines.add("Wolves stunned:  " + session.getWolvesStunned());
        lines.add("Farmers escaped: " + session.getFarmersEscaped());
        if (!session.getActiveBoons().isEmpty()) {
            StringBuilder sb = new StringBuilder("Boons:    ");
            boolean first = true;
            for (Boon b : session.getActiveBoons()) {
                if (!first) sb.append(", ");
                sb.append(b.displayName);
                first = false;
            }
            lines.add(sb.toString());
        }
        lines.add("");
        lines.add("Banked:   +" + truffleDeposit + " T   (total: " + truffleBank + ")");

        double y = height * 0.30;
        for (String text : lines) {
            Text t = new Text(text);
            t.setFont(Font.font("Monospaced", FontWeight.NORMAL, 18));
            t.setFill(Color.rgb(220, 230, 240));
            // Left-align dynamic lines around the centre column
            double approxW = text.length() * 18 * 0.6;
            t.setX((width - approxW) / 2.0);
            t.setY(y);
            group.getChildren().add(t);
            dynamicLines.add(t);
            y += 28;
        }
        group.setVisible(true);
    }

    public void hide() { group.setVisible(false); }

    private void centerText(Text text, double y) {
        double charW = text.getFont().getSize() * 0.55;
        double textW = text.getText().length() * charW;
        text.setX((width - textW) / 2.0);
        text.setY(y);
    }
}
