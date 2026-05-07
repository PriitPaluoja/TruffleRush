package com.example.demo.render;

import com.example.demo.core.MetaProgression;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Mid-level pause overlay. Resume / Quit-to-Menu buttons + a settings cluster
 * (master volume slider, screen-shake toggle, hit-stop toggle). Toggles persist
 * to disk via {@link MetaProgression} immediately on change so the next run
 * picks them up.
 */
public class PauseOverlay {

    private final Group group = new Group();
    private final int width;
    private final int height;
    private MetaProgression meta;

    private Runnable onResume;
    private Runnable onQuit;
    private java.util.function.DoubleConsumer onVolume;
    private java.util.function.Consumer<Boolean> onShakeToggle;
    private java.util.function.Consumer<Boolean> onHitStopToggle;

    private final Rectangle volumeFill;
    private final Rectangle volumeTrack;
    private final Text volumeValue;
    private final Text shakeBtnText;
    private final Rectangle shakeBtnBg;
    private final Text hitStopBtnText;
    private final Rectangle hitStopBtnBg;

    private static final double SLIDER_W = 220;
    private static final double SLIDER_H = 12;

    public PauseOverlay(int width, int height) {
        this.width = width;
        this.height = height;

        Rectangle backdrop = new Rectangle(0, 0, width, height);
        backdrop.setFill(Color.rgb(0, 0, 0, 0.78));
        // Swallow clicks on backdrop so they don't fall through to the world.
        backdrop.setOnMouseClicked(e -> e.consume());
        group.getChildren().add(backdrop);

        Text title = new Text("PAUSED");
        title.setFont(Font.font("System", FontWeight.BOLD, 56));
        title.setFill(Color.rgb(255, 215, 0));
        centerText(title, height * 0.22);
        group.getChildren().add(title);

        // --- Settings panel ---
        double panelW = 360;
        double panelH = 220;
        double panelX = (width - panelW) / 2.0;
        double panelY = height * 0.32;

        Rectangle panel = new Rectangle(panelX, panelY, panelW, panelH);
        panel.setFill(Color.rgb(20, 25, 40, 0.92));
        panel.setStroke(Color.rgb(60, 75, 110));
        panel.setStrokeWidth(2);
        panel.setArcWidth(12);
        panel.setArcHeight(12);
        group.getChildren().add(panel);

        Text settingsLabel = new Text("Settings");
        settingsLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        settingsLabel.setFill(Color.rgb(180, 200, 230));
        settingsLabel.setX(panelX + 18);
        settingsLabel.setY(panelY + 28);
        group.getChildren().add(settingsLabel);

        // Volume row
        double rowY = panelY + 60;
        Text volLabel = new Text("Volume");
        volLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        volLabel.setFill(Color.rgb(220, 220, 220));
        volLabel.setX(panelX + 18);
        volLabel.setY(rowY + 4);
        group.getChildren().add(volLabel);

        double sliderX = panelX + panelW - SLIDER_W - 18;
        double sliderY = rowY - 6;
        volumeTrack = new Rectangle(sliderX, sliderY, SLIDER_W, SLIDER_H);
        volumeTrack.setFill(Color.rgb(55, 60, 75));
        volumeTrack.setArcWidth(8);
        volumeTrack.setArcHeight(8);
        volumeFill = new Rectangle(sliderX, sliderY, SLIDER_W, SLIDER_H);
        volumeFill.setFill(Color.rgb(120, 200, 255));
        volumeFill.setArcWidth(8);
        volumeFill.setArcHeight(8);
        volumeValue = new Text("100%");
        volumeValue.setFont(Font.font("System", FontWeight.NORMAL, 12));
        volumeValue.setFill(Color.rgb(180, 200, 220));
        volumeValue.setX(sliderX + SLIDER_W + 8);
        volumeValue.setY(sliderY + SLIDER_H);

        // Click + drag on the track sets the volume.
        java.util.function.DoubleConsumer setFromX = px -> {
            double frac = Math.max(0.0, Math.min(1.0, (px - sliderX) / SLIDER_W));
            applyVolume(frac);
            if (onVolume != null) onVolume.accept(frac);
        };
        volumeTrack.setOnMousePressed(e -> setFromX.accept(e.getSceneX()));
        volumeTrack.setOnMouseDragged(e -> setFromX.accept(e.getSceneX()));
        volumeFill.setOnMousePressed(e -> setFromX.accept(e.getSceneX()));
        volumeFill.setOnMouseDragged(e -> setFromX.accept(e.getSceneX()));

        group.getChildren().addAll(volumeTrack, volumeFill, volumeValue);

        // Screen-shake toggle row
        rowY += 44;
        Text shakeLabel = new Text("Screen shake");
        shakeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        shakeLabel.setFill(Color.rgb(220, 220, 220));
        shakeLabel.setX(panelX + 18);
        shakeLabel.setY(rowY + 4);
        group.getChildren().add(shakeLabel);

        double tBtnW = 80, tBtnH = 26;
        double tBtnX = panelX + panelW - tBtnW - 18;
        double tBtnY = rowY - 16;
        shakeBtnBg = new Rectangle(tBtnX, tBtnY, tBtnW, tBtnH);
        shakeBtnBg.setArcWidth(8);
        shakeBtnBg.setArcHeight(8);
        shakeBtnText = new Text("ON");
        shakeBtnText.setFont(Font.font("System", FontWeight.BOLD, 13));
        shakeBtnText.setFill(Color.WHITE);
        shakeBtnBg.setOnMouseClicked(e -> toggleShake());
        shakeBtnText.setOnMouseClicked(e -> toggleShake());
        positionToggle(shakeBtnBg, shakeBtnText, tBtnX, tBtnY, tBtnW, tBtnH);
        group.getChildren().addAll(shakeBtnBg, shakeBtnText);

        // Hit-stop toggle row
        rowY += 44;
        Text hsLabel = new Text("Hit-stop");
        hsLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        hsLabel.setFill(Color.rgb(220, 220, 220));
        hsLabel.setX(panelX + 18);
        hsLabel.setY(rowY + 4);
        group.getChildren().add(hsLabel);

        double tBtn2Y = rowY - 16;
        hitStopBtnBg = new Rectangle(tBtnX, tBtn2Y, tBtnW, tBtnH);
        hitStopBtnBg.setArcWidth(8);
        hitStopBtnBg.setArcHeight(8);
        hitStopBtnText = new Text("ON");
        hitStopBtnText.setFont(Font.font("System", FontWeight.BOLD, 13));
        hitStopBtnText.setFill(Color.WHITE);
        hitStopBtnBg.setOnMouseClicked(e -> toggleHitStop());
        hitStopBtnText.setOnMouseClicked(e -> toggleHitStop());
        positionToggle(hitStopBtnBg, hitStopBtnText, tBtnX, tBtn2Y, tBtnW, tBtnH);
        group.getChildren().addAll(hitStopBtnBg, hitStopBtnText);

        // Resume button
        double btnW = 200, btnH = 44;
        double btnX = (width - btnW) / 2.0;
        double btnY = panelY + panelH + 24;

        Rectangle resumeBg = new Rectangle(btnX, btnY, btnW, btnH);
        resumeBg.setFill(Color.rgb(60, 140, 60));
        resumeBg.setStroke(Color.rgb(30, 100, 30));
        resumeBg.setStrokeWidth(2);
        resumeBg.setArcWidth(12);
        resumeBg.setArcHeight(12);
        Text resumeText = new Text("Resume");
        resumeText.setFont(Font.font("System", FontWeight.BOLD, 20));
        resumeText.setFill(Color.WHITE);
        centerText(resumeText, btnY + 29);
        resumeBg.setOnMouseEntered(e -> resumeBg.setFill(Color.rgb(80, 170, 80)));
        resumeBg.setOnMouseExited(e -> resumeBg.setFill(Color.rgb(60, 140, 60)));
        resumeBg.setOnMouseClicked(e -> { if (onResume != null) onResume.run(); });
        resumeText.setOnMouseClicked(e -> { if (onResume != null) onResume.run(); });
        group.getChildren().addAll(resumeBg, resumeText);

        // Quit button
        double qBtnY = btnY + btnH + 12;
        Rectangle quitBg = new Rectangle(btnX, qBtnY, btnW, btnH);
        quitBg.setFill(Color.rgb(140, 60, 60));
        quitBg.setStroke(Color.rgb(100, 30, 30));
        quitBg.setStrokeWidth(2);
        quitBg.setArcWidth(12);
        quitBg.setArcHeight(12);
        Text quitText = new Text("Quit to Menu");
        quitText.setFont(Font.font("System", FontWeight.BOLD, 18));
        quitText.setFill(Color.WHITE);
        centerText(quitText, qBtnY + 28);
        quitBg.setOnMouseEntered(e -> quitBg.setFill(Color.rgb(170, 80, 80)));
        quitBg.setOnMouseExited(e -> quitBg.setFill(Color.rgb(140, 60, 60)));
        quitBg.setOnMouseClicked(e -> { if (onQuit != null) onQuit.run(); });
        quitText.setOnMouseClicked(e -> { if (onQuit != null) onQuit.run(); });
        group.getChildren().addAll(quitBg, quitText);

        Text hint = new Text("Press Esc to resume");
        hint.setFont(Font.font("System", FontWeight.NORMAL, 12));
        hint.setFill(Color.rgb(140, 150, 170));
        centerText(hint, qBtnY + btnH + 28);
        group.getChildren().add(hint);

        group.setVisible(false);
    }

    public Group getGroup() { return group; }

    public void show(MetaProgression meta) {
        this.meta = meta;
        applyVolume(meta.getMasterVolume());
        refreshShakeButton(meta.isShakeEnabled());
        refreshHitStopButton(meta.isHitStopEnabled());
        group.setVisible(true);
    }

    public void hide() { group.setVisible(false); }

    public boolean isVisible() { return group.isVisible(); }

    public void setOnResume(Runnable r) { this.onResume = r; }
    public void setOnQuit(Runnable r) { this.onQuit = r; }
    public void setOnVolumeChanged(java.util.function.DoubleConsumer c) { this.onVolume = c; }
    public void setOnShakeToggled(java.util.function.Consumer<Boolean> c) { this.onShakeToggle = c; }
    public void setOnHitStopToggled(java.util.function.Consumer<Boolean> c) { this.onHitStopToggle = c; }

    private void toggleShake() {
        if (meta == null) return;
        boolean next = !meta.isShakeEnabled();
        refreshShakeButton(next);
        if (onShakeToggle != null) onShakeToggle.accept(next);
    }

    private void toggleHitStop() {
        if (meta == null) return;
        boolean next = !meta.isHitStopEnabled();
        refreshHitStopButton(next);
        if (onHitStopToggle != null) onHitStopToggle.accept(next);
    }

    private void refreshShakeButton(boolean on) {
        shakeBtnText.setText(on ? "ON" : "OFF");
        shakeBtnBg.setFill(on ? Color.rgb(60, 140, 60) : Color.rgb(110, 50, 50));
        positionToggle(shakeBtnBg, shakeBtnText,
            shakeBtnBg.getX(), shakeBtnBg.getY(),
            shakeBtnBg.getWidth(), shakeBtnBg.getHeight());
    }

    private void refreshHitStopButton(boolean on) {
        hitStopBtnText.setText(on ? "ON" : "OFF");
        hitStopBtnBg.setFill(on ? Color.rgb(60, 140, 60) : Color.rgb(110, 50, 50));
        positionToggle(hitStopBtnBg, hitStopBtnText,
            hitStopBtnBg.getX(), hitStopBtnBg.getY(),
            hitStopBtnBg.getWidth(), hitStopBtnBg.getHeight());
    }

    private void applyVolume(double frac) {
        volumeFill.setWidth(Math.max(2, SLIDER_W * frac));
        int pct = (int) Math.round(frac * 100);
        volumeValue.setText(pct + "%");
    }

    private static void positionToggle(Rectangle bg, Text text,
                                       double x, double y, double w, double h) {
        double charW = text.getFont().getSize() * 0.55;
        double textW = text.getText().length() * charW;
        text.setX(x + (w - textW) / 2.0);
        text.setY(y + h / 2 + 5);
    }

    private void centerText(Text text, double y) {
        double charW = text.getFont().getSize() * 0.55;
        double textW = text.getText().length() * charW;
        text.setX((width - textW) / 2.0);
        text.setY(y);
    }
}
