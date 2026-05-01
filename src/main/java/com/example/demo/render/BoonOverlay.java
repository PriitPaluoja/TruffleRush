package com.example.demo.render;

import com.example.demo.core.Boon;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Shown after each completed level: pick 1 of 3 boons to apply for the rest of the run.
 * Boons stack across levels.
 */
public class BoonOverlay {

    private final Group group = new Group();
    private final int width;
    private final int height;
    private final Random rng = new Random();
    private Consumer<Boon> onPicked;
    private final List<BoonCard> cards = new ArrayList<>(3);

    public BoonOverlay(int width, int height) {
        this.width = width;
        this.height = height;

        Rectangle backdrop = new Rectangle(0, 0, width, height);
        backdrop.setFill(Color.rgb(0, 0, 0, 0.85));
        group.getChildren().add(backdrop);

        Text title = new Text("Pick a Boon");
        title.setFont(Font.font("System", FontWeight.BOLD, 36));
        title.setFill(Color.rgb(255, 215, 0));
        centerText(title, 80);
        group.getChildren().add(title);

        Text hint = new Text("Each boon trades a benefit for a cost. They last for the whole run.");
        hint.setFont(Font.font("System", FontWeight.NORMAL, 14));
        hint.setFill(Color.rgb(180, 190, 200));
        centerText(hint, 110);
        group.getChildren().add(hint);

        // Three card slots, evenly spaced
        double cardW = 240, cardH = 220;
        double gap = 40;
        double totalW = cardW * 3 + gap * 2;
        double startX = (width - totalW) / 2.0;
        double cardY = (height - cardH) / 2.0 + 20;
        for (int i = 0; i < 3; i++) {
            BoonCard card = new BoonCard(startX + i * (cardW + gap), cardY, cardW, cardH);
            cards.add(card);
            group.getChildren().addAll(card.nodes());
        }

        group.setVisible(false);
    }

    public Group getGroup() { return group; }
    public void setOnPicked(Consumer<Boon> handler) { this.onPicked = handler; }

    /** Show the overlay with three random boons drawn from the given pool of remaining choices. */
    public void show(List<Boon> remaining) {
        List<Boon> pool = new ArrayList<>(remaining);
        Collections.shuffle(pool, rng);
        for (int i = 0; i < cards.size(); i++) {
            Boon b = i < pool.size() ? pool.get(i) : null;
            cards.get(i).bind(b);
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

    private final class BoonCard {
        final Rectangle bg;
        final Text nameText;
        final Text descText;
        final Text flavorText;
        final double x;
        final double y;
        final double w;
        Boon boon;

        BoonCard(double x, double y, double w, double h) {
            this.x = x; this.y = y; this.w = w;
            bg = new Rectangle(x, y, w, h);
            bg.setFill(Color.rgb(30, 35, 50));
            bg.setStroke(Color.rgb(120, 140, 80));
            bg.setStrokeWidth(2);
            bg.setArcWidth(12);
            bg.setArcHeight(12);

            nameText = new Text();
            nameText.setFont(Font.font("System", FontWeight.BOLD, 22));
            nameText.setFill(Color.rgb(255, 215, 0));
            nameText.setY(y + 50);

            descText = new Text();
            descText.setFont(Font.font("System", FontWeight.NORMAL, 14));
            descText.setFill(Color.WHITE);
            descText.setWrappingWidth(w - 24);
            descText.setX(x + 12);
            descText.setY(y + 100);

            flavorText = new Text();
            flavorText.setFont(Font.font("System", FontWeight.NORMAL, 12));
            flavorText.setFill(Color.rgb(160, 170, 200));
            flavorText.setWrappingWidth(w - 24);
            flavorText.setX(x + 12);
            flavorText.setY(y + 170);

            bg.setOnMouseEntered(e -> { if (boon != null) bg.setFill(Color.rgb(50, 60, 80)); });
            bg.setOnMouseExited(e -> bg.setFill(Color.rgb(30, 35, 50)));
            bg.setOnMouseClicked(e -> pick());
            nameText.setOnMouseClicked(e -> pick());
            descText.setOnMouseClicked(e -> pick());
            flavorText.setOnMouseClicked(e -> pick());
        }

        java.util.List<javafx.scene.Node> nodes() {
            return java.util.List.of(bg, nameText, descText, flavorText);
        }

        void bind(Boon b) {
            this.boon = b;
            if (b == null) {
                nameText.setText("(empty)");
                descText.setText("No more boons available.");
                flavorText.setText("");
                bg.setOpacity(0.4);
                bg.setDisable(true);
                return;
            }
            nameText.setText(b.displayName);
            double charW = nameText.getFont().getSize() * 0.55;
            double textW = b.displayName.length() * charW;
            nameText.setX(x + (w - textW) / 2.0);
            descText.setText(b.shortDesc);
            flavorText.setText(b.flavor);
            bg.setOpacity(1.0);
            bg.setDisable(false);
        }

        void pick() {
            if (boon != null && onPicked != null) onPicked.accept(boon);
        }
    }
}
