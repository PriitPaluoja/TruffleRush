package com.example.demo.render;

import com.example.demo.entity.Pig;
import com.example.demo.entity.PlayerPig;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a heads-up display (HUD) overlaid on the top edge of the game grid.
 *
 * <p>Caches last-rendered values per element so {@code setText} is only called
 * when the displayed string actually changes, avoiding per-frame string
 * formatting in the steady state.
 */
public class HudRenderer {

    private static final int    HUD_HEIGHT   = 40;
    private static final double TEXT_Y       = 26.0;
    private static final double FONT_SIZE    = 13.0;
    private static final Color  TEXT_COLOR   = Color.WHITE;
    private static final Color  READY_COLOR  = Color.rgb(100, 255, 100);
    private static final Color  COOL_COLOR   = Color.rgb(255, 180, 80);
    private static final Color  STREAK_COLOR = Color.rgb(255, 140, 60);

    private final Group     group     = new Group();
    private final Rectangle background;
    private final List<Text> pigTexts = new ArrayList<>();
    private final List<String> lastPigStrings = new ArrayList<>();
    private final Text weatherText;
    private final Text timerText;
    private final Text sniffText;
    private final Text powerUpText;
    private final Text streakText;

    private String lastWeather = "";
    private int lastTimerTicks = -1;
    private String lastSniffString = "";
    private int lastSniffStateKey = -1; // 0=active, 1=ready, 2=cooling
    private String lastPowerUpString = "";
    private boolean lastPowerUpSuper;
    private int lastStreakSeconds = -1;

    public HudRenderer(int mapWidth) {
        background = new Rectangle(0, 0, mapWidth, HUD_HEIGHT);
        background.setFill(Color.rgb(10, 10, 10, 0.70));

        weatherText = makeText("", Color.rgb(200, 230, 255));
        timerText   = makeText("", TEXT_COLOR);
        sniffText   = makeText("", READY_COLOR);
        powerUpText = makeText("", Color.rgb(255, 215, 0));
        streakText  = makeText("", STREAK_COLOR);

        group.getChildren().addAll(background, weatherText, timerText, sniffText, powerUpText, streakText);
    }

    public Group getGroup() {
        return group;
    }

    public void update(int roundTicksRemaining,
                       PlayerPig player,
                       List<Pig> pigs,
                       String weatherName,
                       boolean sniffReady,
                       double sniffCooldownSeconds,
                       boolean sniffActive,
                       int streakSeconds,
                       double streakMultiplier) {

        while (pigTexts.size() < pigs.size()) {
            Text t = makeText("", TEXT_COLOR);
            pigTexts.add(t);
            lastPigStrings.add("");
            group.getChildren().add(t);
        }
        while (pigTexts.size() > pigs.size()) {
            Text removed = pigTexts.remove(pigTexts.size() - 1);
            lastPigStrings.remove(lastPigStrings.size() - 1);
            group.getChildren().remove(removed);
        }

        double x = 8.0;
        for (int i = 0; i < pigs.size(); i++) {
            Pig pig = pigs.get(i);
            Text t  = pigTexts.get(i);
            // Round weight to one decimal; only update if the displayed value would change.
            int tenths = (int) Math.round(pig.getWeight() * 10);
            String displayed = pig.getName() + ": " + (tenths / 10) + "." + Math.abs(tenths % 10) + "kg";
            if (!displayed.equals(lastPigStrings.get(i))) {
                t.setText(displayed);
                lastPigStrings.set(i, displayed);
            }
            t.setFill(pig.getColor());
            t.setX(x);
            t.setY(TEXT_Y);
            x += 110.0;
        }

        if (!weatherName.equals(lastWeather)) {
            weatherText.setText(weatherName);
            lastWeather = weatherName;
        }
        weatherText.setX(560.0);
        weatherText.setY(TEXT_Y);

        if (roundTicksRemaining != lastTimerTicks) {
            int totalSeconds = roundTicksRemaining / 60;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            // Avoid String.format for the per-tick path.
            StringBuilder sb = new StringBuilder(12);
            sb.append("Time: ");
            if (minutes < 10) sb.append('0');
            sb.append(minutes).append(':');
            if (seconds < 10) sb.append('0');
            sb.append(seconds);
            timerText.setText(sb.toString());
            lastTimerTicks = roundTicksRemaining;
        }
        timerText.setX(700.0);
        timerText.setY(TEXT_Y);

        int sniffKey = sniffActive ? 0 : (sniffReady ? 1 : 2);
        if (sniffKey == 2) {
            int secs = (int) Math.ceil(sniffCooldownSeconds);
            String s = "Sniff: " + secs + "s";
            if (sniffKey != lastSniffStateKey || !s.equals(lastSniffString)) {
                sniffText.setText(s);
                sniffText.setFill(COOL_COLOR);
                lastSniffString = s;
            }
        } else if (sniffKey != lastSniffStateKey) {
            if (sniffKey == 0) {
                sniffText.setText("Sniff: ACTIVE");
                sniffText.setFill(Color.rgb(100, 255, 200));
                lastSniffString = "Sniff: ACTIVE";
            } else {
                sniffText.setText("Sniff: READY");
                sniffText.setFill(READY_COLOR);
                lastSniffString = "Sniff: READY";
            }
        }
        lastSniffStateKey = sniffKey;
        sniffText.setX(850.0);
        sniffText.setY(TEXT_Y);

        // Streak badge — only render when active (>=1s of streak)
        if (streakSeconds != lastStreakSeconds) {
            if (streakSeconds <= 0) {
                streakText.setText("");
            } else {
                int x10 = (int) Math.round(streakMultiplier * 10);
                streakText.setText("STREAK x" + (x10 / 10) + "." + Math.abs(x10 % 10));
            }
            lastStreakSeconds = streakSeconds;
        }
        streakText.setX(8.0);
        streakText.setY(TEXT_Y + 14);

        // Power-ups — build once then compare
        StringBuilder pb = new StringBuilder();
        if (player.isSuperPig())    appendPower(pb, "*SUPER", player.getSuperPigTicks() / 60);
        if (player.hasSpeedBoost()) appendPower(pb, "SPD",    player.getSpeedBoostTicks() / 60);
        if (player.hasShield())     { if (pb.length() > 0) pb.append(' '); pb.append("SHLD"); }
        if (player.hasMagnet())     appendPower(pb, "MAG",    player.getMagnetTicks() / 60);
        String powers = pb.toString();
        boolean superNow = player.isSuperPig();
        if (!powers.equals(lastPowerUpString)) {
            powerUpText.setText(powers);
            lastPowerUpString = powers;
        }
        if (superNow != lastPowerUpSuper) {
            powerUpText.setFill(superNow ? Color.rgb(255, 215, 0) : Color.rgb(100, 200, 255));
            lastPowerUpSuper = superNow;
        }
        powerUpText.setX(955.0);
        powerUpText.setY(TEXT_Y);
    }

    private static void appendPower(StringBuilder sb, String label, int seconds) {
        if (sb.length() > 0) sb.append(' ');
        sb.append(label).append('(').append(seconds).append("s)");
    }

    private static Text makeText(String content, Color fill) {
        Text t = new Text(content);
        t.setFont(Font.font("System", FontWeight.BOLD, FONT_SIZE));
        t.setFill(fill);
        return t;
    }
}
