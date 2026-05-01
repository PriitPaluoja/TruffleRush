package com.example.demo.render;

import com.example.demo.core.MetaProgression;
import com.example.demo.core.Perk;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Shop screen between the main menu and a run. Lets the player spend banked
 * truffles on permanent perks, or skip straight to the run.
 */
public class ShopOverlay {

    private final Group group = new Group();
    private final int width;
    private final int height;
    private MetaProgression meta;

    private final Text bankText;
    private final List<PerkRow> rows = new ArrayList<>();
    private Runnable onStart;
    private Runnable onDailyRun;

    public ShopOverlay(int width, int height) {
        this.width = width;
        this.height = height;

        Rectangle backdrop = new Rectangle(0, 0, width, height);
        backdrop.setFill(Color.rgb(10, 15, 25, 0.97));
        group.getChildren().add(backdrop);

        Text title = new Text("Truffle Cellar");
        title.setFont(Font.font("System", FontWeight.BOLD, 40));
        title.setFill(Color.rgb(255, 215, 0));
        centerText(title, 64);
        group.getChildren().add(title);

        bankText = new Text("Bank: 0 truffles");
        bankText.setFont(Font.font("System", FontWeight.BOLD, 22));
        bankText.setFill(Color.rgb(255, 240, 100));
        centerText(bankText, 100);
        group.getChildren().add(bankText);

        Text hint = new Text("Spend banked truffles on permanent perks. They persist between runs.");
        hint.setFont(Font.font("System", FontWeight.NORMAL, 13));
        hint.setFill(Color.rgb(160, 170, 180));
        centerText(hint, 124);
        group.getChildren().add(hint);

        // Perk rows
        Perk[] perks = Perk.values();
        double rowY = 160;
        double rowH = 56;
        double rowW = 580;
        double rowX = (width - rowW) / 2.0;
        for (Perk p : perks) {
            PerkRow row = new PerkRow(p, rowX, rowY, rowW, rowH);
            rows.add(row);
            group.getChildren().addAll(row.nodes());
            rowY += rowH + 8;
        }

        // Start button
        double btnW = 200, btnH = 44;
        double btnX = (width - btnW) / 2.0;
        double btnY = height - 100;
        Rectangle startBg = new Rectangle(btnX, btnY, btnW, btnH);
        startBg.setFill(Color.rgb(60, 140, 60));
        startBg.setStroke(Color.rgb(30, 100, 30));
        startBg.setStrokeWidth(2);
        startBg.setArcWidth(12);
        startBg.setArcHeight(12);
        Text startText = new Text("Begin Run");
        startText.setFont(Font.font("System", FontWeight.BOLD, 20));
        startText.setFill(Color.WHITE);
        centerText(startText, btnY + 29);
        startBg.setOnMouseEntered(e -> startBg.setFill(Color.rgb(80, 170, 80)));
        startBg.setOnMouseExited(e -> startBg.setFill(Color.rgb(60, 140, 60)));
        startBg.setOnMouseClicked(e -> { if (onStart != null) onStart.run(); });
        startText.setOnMouseClicked(e -> { if (onStart != null) onStart.run(); });
        group.getChildren().addAll(startBg, startText);

        // Daily-run button
        double dailyW = 200, dailyH = 36;
        double dailyX = (width - dailyW) / 2.0;
        double dailyY = btnY + btnH + 12;
        Rectangle dailyBg = new Rectangle(dailyX, dailyY, dailyW, dailyH);
        dailyBg.setFill(Color.rgb(60, 70, 140));
        dailyBg.setStroke(Color.rgb(30, 40, 100));
        dailyBg.setStrokeWidth(2);
        dailyBg.setArcWidth(10);
        dailyBg.setArcHeight(10);
        Text dailyText = new Text("Today's Daily Run");
        dailyText.setFont(Font.font("System", FontWeight.BOLD, 16));
        dailyText.setFill(Color.WHITE);
        centerText(dailyText, dailyY + 24);
        dailyBg.setOnMouseEntered(e -> dailyBg.setFill(Color.rgb(80, 90, 170)));
        dailyBg.setOnMouseExited(e -> dailyBg.setFill(Color.rgb(60, 70, 140)));
        dailyBg.setOnMouseClicked(e -> { if (onDailyRun != null) onDailyRun.run(); });
        dailyText.setOnMouseClicked(e -> { if (onDailyRun != null) onDailyRun.run(); });
        group.getChildren().addAll(dailyBg, dailyText);

        group.setVisible(false);
    }

    public Group getGroup() { return group; }

    public void show(MetaProgression meta) {
        this.meta = meta;
        refresh();
        group.setVisible(true);
    }

    public void hide() { group.setVisible(false); }

    public void setOnStart(Runnable r) { this.onStart = r; }
    public void setOnDailyRun(Runnable r) { this.onDailyRun = r; }

    private void refresh() {
        bankText.setText("Bank: " + meta.getTruffleBank() + " truffles");
        centerText(bankText, 100);
        for (PerkRow row : rows) row.refresh(meta);
    }

    private void centerText(Text text, double y) {
        double charW = text.getFont().getSize() * 0.55;
        double textW = text.getText().length() * charW;
        text.setX((width - textW) / 2.0);
        text.setY(y);
    }

    private final class PerkRow {
        final Perk perk;
        final Rectangle bg;
        final Text nameText;
        final Text descText;
        final Text levelText;
        final Rectangle buyBg;
        final Text buyText;

        PerkRow(Perk perk, double x, double y, double w, double h) {
            this.perk = perk;
            bg = new Rectangle(x, y, w, h);
            bg.setFill(Color.rgb(30, 35, 50));
            bg.setStroke(Color.rgb(60, 70, 90));
            bg.setStrokeWidth(1);
            bg.setArcWidth(8);
            bg.setArcHeight(8);

            nameText = new Text(perk.displayName);
            nameText.setFont(Font.font("System", FontWeight.BOLD, 18));
            nameText.setFill(Color.rgb(255, 215, 0));
            nameText.setX(x + 14);
            nameText.setY(y + 22);

            descText = new Text(perk.description);
            descText.setFont(Font.font("System", FontWeight.NORMAL, 13));
            descText.setFill(Color.rgb(180, 190, 200));
            descText.setX(x + 14);
            descText.setY(y + 42);

            levelText = new Text("0/" + perk.maxLevel);
            levelText.setFont(Font.font("System", FontWeight.NORMAL, 14));
            levelText.setFill(Color.rgb(160, 170, 180));
            levelText.setX(x + w - 200);
            levelText.setY(y + h / 2 + 5);

            double bw = 90, bh = 30;
            buyBg = new Rectangle(x + w - bw - 10, y + (h - bh) / 2, bw, bh);
            buyBg.setArcWidth(8);
            buyBg.setArcHeight(8);

            buyText = new Text("");
            buyText.setFont(Font.font("System", FontWeight.BOLD, 14));
            buyText.setFill(Color.WHITE);
            buyText.setX(buyBg.getX() + 18);
            buyText.setY(buyBg.getY() + bh / 2 + 5);

            buyBg.setOnMouseClicked(e -> attemptBuy());
            buyText.setOnMouseClicked(e -> attemptBuy());
        }

        java.util.List<javafx.scene.Node> nodes() {
            return java.util.List.of(bg, nameText, descText, levelText, buyBg, buyText);
        }

        void refresh(MetaProgression meta) {
            int level = meta.getPerkLevel(perk);
            levelText.setText("Lv " + level + "/" + perk.maxLevel);
            int cost = perk.costForLevel(level);
            if (cost < 0) {
                buyText.setText("MAX");
                buyBg.setFill(Color.rgb(60, 60, 60));
            } else if (meta.getTruffleBank() < cost) {
                buyText.setText(cost + " T");
                buyBg.setFill(Color.rgb(80, 50, 50));
            } else {
                buyText.setText(cost + " T");
                buyBg.setFill(Color.rgb(60, 120, 60));
            }
        }

        void attemptBuy() {
            if (meta.tryBuy(perk)) {
                meta.save();
                ShopOverlay.this.refresh();
            }
        }
    }
}
