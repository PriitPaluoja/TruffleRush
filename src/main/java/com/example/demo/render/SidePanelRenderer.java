package com.example.demo.render;

import com.example.demo.core.GameSession;
import com.example.demo.core.RandomEventManager;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a side panel (120 px wide) to the right of the game grid showing
 * level, score, high score, and a scrolling event log.
 */
public class SidePanelRenderer {

    private static final int PANEL_WIDTH = 120;
    private static final double PADDING = 8.0;
    private static final Color BG_COLOR = Color.rgb(15, 15, 25, 0.85);
    private static final Color LABEL_COLOR = Color.rgb(180, 180, 200);
    private static final Color VALUE_COLOR = Color.WHITE;
    private static final Color EVENT_COLOR = Color.rgb(255, 220, 100);

    private final Group group = new Group();
    private final int panelX;

    private final Text levelLabel;
    private final Text levelValue;
    private final Text scoreLabel;
    private final Text scoreValue;
    private final Text highScoreLabel;
    private final Text highScoreValue;
    private final Text activeEventText;
    private final Text eventTitle;
    private final List<Text> eventTexts = new ArrayList<>();
    private final List<String> eventLog = new ArrayList<>();

    private static final int MAX_EVENTS = 6;

    public SidePanelRenderer(int mapWidth, int mapHeight) {
        this.panelX = mapWidth;

        Rectangle bg = new Rectangle(panelX, 0, PANEL_WIDTH, mapHeight);
        bg.setFill(BG_COLOR);
        group.getChildren().add(bg);

        double y = 16.0;
        double x = panelX + PADDING;

        levelLabel = makeLabel("LEVEL", x, y);
        y += 16;
        levelValue = makeValue("1", x, y);
        y += 28;

        scoreLabel = makeLabel("SCORE", x, y);
        y += 16;
        scoreValue = makeValue("0", x, y);
        y += 28;

        highScoreLabel = makeLabel("HIGH SCORE", x, y);
        y += 16;
        highScoreValue = makeValue("0", x, y);
        y += 28;

        activeEventText = new Text("");
        activeEventText.setFont(Font.font("System", FontWeight.BOLD, 10));
        activeEventText.setFill(Color.rgb(255, 100, 100));
        activeEventText.setX(x);
        activeEventText.setY(y);
        activeEventText.setWrappingWidth(PANEL_WIDTH - PADDING * 2);
        y += 18;
        group.getChildren().add(activeEventText);

        eventTitle = makeLabel("LOG", x, y);
        y += 18;

        for (int i = 0; i < MAX_EVENTS; i++) {
            Text t = new Text("");
            t.setFont(Font.font("System", FontWeight.NORMAL, 10));
            t.setFill(EVENT_COLOR);
            t.setX(x);
            t.setY(y + i * 14);
            t.setWrappingWidth(PANEL_WIDTH - PADDING * 2);
            eventTexts.add(t);
            group.getChildren().add(t);
        }

        group.getChildren().addAll(levelLabel, levelValue, scoreLabel, scoreValue,
                highScoreLabel, highScoreValue, eventTitle);
    }

    public Group getGroup() { return group; }

    public void update(GameSession session) {
        update(session, null);
    }

    public void update(GameSession session, RandomEventManager events) {
        levelValue.setText(String.valueOf(session.getLevel()));
        scoreValue.setText(String.valueOf(session.getScore()));
        highScoreValue.setText(String.valueOf(GameSession.getHighScore()));
        activeEventText.setText(events != null ? events.getActiveEventLabel() : "");

        // Update event log display
        int start = Math.max(0, eventLog.size() - MAX_EVENTS);
        for (int i = 0; i < MAX_EVENTS; i++) {
            int idx = start + i;
            if (idx < eventLog.size()) {
                eventTexts.get(i).setText(eventLog.get(idx));
            } else {
                eventTexts.get(i).setText("");
            }
        }
    }

    public void addEvent(String message) {
        eventLog.add(message);
        if (eventLog.size() > 50) {
            eventLog.remove(0);
        }
    }

    private Text makeLabel(String text, double x, double y) {
        Text t = new Text(text);
        t.setFont(Font.font("System", FontWeight.BOLD, 10));
        t.setFill(LABEL_COLOR);
        t.setX(x);
        t.setY(y);
        return t;
    }

    private Text makeValue(String text, double x, double y) {
        Text t = new Text(text);
        t.setFont(Font.font("System", FontWeight.BOLD, 16));
        t.setFill(VALUE_COLOR);
        t.setX(x);
        t.setY(y);
        return t;
    }
}
