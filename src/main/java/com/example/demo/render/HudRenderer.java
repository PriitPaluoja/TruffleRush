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
 * <p>The HUD is a semi-transparent dark bar (height 40 px) drawn at y = 0,
 * spanning the full map width.  It displays:
 * <ul>
 *   <li>Each pig's name and current weight (coloured per pig).</li>
 *   <li>The current weather name.</li>
 *   <li>Round time remaining in MM:SS format.</li>
 *   <li>Sniff ability status: "Sniff: READY" or "Sniff: Xs".</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   HudRenderer hud = new HudRenderer(800);
 *   sceneRoot.getChildren().add(hud.getGroup());
 *
 *   // In the game loop:
 *   hud.update(ticksLeft, playerPig, allPigs, weatherName, sniffReady, sniffCooldownSecs);
 * </pre>
 */
public class HudRenderer {

    private static final int    HUD_HEIGHT   = 40;
    private static final double TEXT_Y       = 26.0;   // baseline within 40-px bar
    private static final double FONT_SIZE    = 13.0;
    private static final Color  TEXT_COLOR   = Color.WHITE;
    private static final Color  READY_COLOR  = Color.rgb(100, 255, 100);
    private static final Color  COOL_COLOR   = Color.rgb(255, 180, 80);

    // -------------------------------------------------------------------------
    // Scene-graph nodes
    // -------------------------------------------------------------------------

    private final Group     group     = new Group();
    private final Rectangle background;

    /** One Text node per pig for weight display.  Rebuilt on first update if pig count changes. */
    private final List<Text> pigTexts = new ArrayList<>();

    private final Text weatherText;
    private final Text timerText;
    private final Text sniffText;

    private final int mapWidth;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a HUD renderer sized to the given map pixel width.
     *
     * @param mapWidth total pixel width of the game area
     */
    public HudRenderer(int mapWidth) {
        this.mapWidth = mapWidth;

        // Semi-transparent background bar
        background = new Rectangle(0, 0, mapWidth, HUD_HEIGHT);
        background.setFill(Color.rgb(10, 10, 10, 0.70));

        // Weather label
        weatherText = makeText("", Color.rgb(200, 230, 255));

        // Round timer
        timerText = makeText("", TEXT_COLOR);

        // Sniff status
        sniffText = makeText("", READY_COLOR);

        group.getChildren().addAll(background, weatherText, timerText, sniffText);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the scene-graph group to be added to the scene root.
     *
     * @return the HUD group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Refreshes all HUD elements with the latest game state.
     *
     * @param roundTicksRemaining  ticks remaining in the current round
     * @param player               the player pig (used for sniff state)
     * @param pigs                 all pigs in display order
     * @param weatherName          human-readable current weather name
     * @param sniffReady           {@code true} when the sniff cooldown has expired
     * @param sniffCooldownSeconds seconds remaining on the sniff cooldown (0 if ready)
     */
    public void update(int roundTicksRemaining,
                       PlayerPig player,
                       List<Pig> pigs,
                       String weatherName,
                       boolean sniffReady,
                       double sniffCooldownSeconds) {

        // --- Ensure we have one Text node per pig ---
        while (pigTexts.size() < pigs.size()) {
            Text t = makeText("", TEXT_COLOR);
            pigTexts.add(t);
            group.getChildren().add(t);
        }
        while (pigTexts.size() > pigs.size()) {
            Text removed = pigTexts.remove(pigTexts.size() - 1);
            group.getChildren().remove(removed);
        }

        // --- Layout: pig weights on the left, weather + timer + sniff on the right ---
        //
        // We'll lay out pig texts starting at x = 8, each ~130 px apart.
        // Weather, timer, and sniff are right-aligned with fixed positions.

        double x = 8.0;
        for (int i = 0; i < pigs.size(); i++) {
            Pig pig = pigs.get(i);
            Text t  = pigTexts.get(i);
            t.setText(pig.getName() + ": " + String.format("%.1f", pig.getWeight()) + "kg");
            t.setFill(pig.getColor());
            t.setX(x);
            t.setY(TEXT_Y);
            x += 135.0;
        }

        // Weather — placed after pig texts with a small gap
        weatherText.setText(weatherName);
        weatherText.setX(x + 10.0);
        weatherText.setY(TEXT_Y);

        // Timer — right of weather
        int totalSeconds = roundTicksRemaining / 60;
        int minutes      = totalSeconds / 60;
        int seconds      = totalSeconds % 60;
        timerText.setText(String.format("Time: %02d:%02d", minutes, seconds));
        timerText.setX(mapWidth - 260.0);
        timerText.setY(TEXT_Y);

        // Sniff status — far right
        if (sniffReady) {
            sniffText.setText("Sniff: READY");
            sniffText.setFill(READY_COLOR);
        } else {
            sniffText.setText(String.format("Sniff: %.0fs", sniffCooldownSeconds));
            sniffText.setFill(COOL_COLOR);
        }
        sniffText.setX(mapWidth - 130.0);
        sniffText.setY(TEXT_Y);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a bold Text node with the given initial string and fill colour. */
    private static Text makeText(String content, Color fill) {
        Text t = new Text(content);
        t.setFont(Font.font("System", FontWeight.BOLD, FONT_SIZE));
        t.setFill(fill);
        return t;
    }
}
