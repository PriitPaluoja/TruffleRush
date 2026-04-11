package com.example.demo.render;

import com.example.demo.entity.Pig;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a full-screen round-end results overlay.
 *
 * <p>The overlay consists of:
 * <ul>
 *   <li>A semi-transparent dark backdrop covering the entire map.</li>
 *   <li>A large "ROUND OVER" title.</li>
 *   <li>Each pig ranked by final weight (heaviest first), with the winner
 *       highlighted in gold and the rest in white.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   RoundEndOverlay overlay = new RoundEndOverlay(800, 600);
 *   sceneRoot.getChildren().add(overlay.getGroup());
 *
 *   // When the round ends (pass pigs sorted heaviest-first):
 *   overlay.show(rankedPigs);
 *
 *   // To start the next round:
 *   overlay.hide();
 * </pre>
 */
public class RoundEndOverlay {

    private static final Color WINNER_COLOR  = Color.rgb(255, 215, 0);   // gold
    private static final Color NORMAL_COLOR  = Color.WHITE;
    private static final Color TITLE_COLOR   = Color.rgb(255, 240, 100);
    private static final Color MEDAL_COLORS[] = {
        Color.rgb(255, 215,   0),  // 1st – gold
        Color.rgb(192, 192, 192),  // 2nd – silver
        Color.rgb(205, 127,  50),  // 3rd – bronze
    };

    private static final double TITLE_FONT_SIZE = 52.0;
    private static final double RANK_FONT_SIZE  = 22.0;
    private static final double LINE_SPACING    = 36.0;

    // -------------------------------------------------------------------------
    // Scene-graph nodes
    // -------------------------------------------------------------------------

    private final Group     group      = new Group();
    private final Rectangle backdrop;
    private final Text      titleText;

    /** Dynamically created per pig on each show() call. */
    private final List<Text> rankTexts = new ArrayList<>();

    /** "Play Again" button background. */
    private final Rectangle playAgainBg;

    /** "Play Again" button label. */
    private final Text playAgainText;

    /** Callback invoked when the player clicks "Play Again". */
    private Runnable onPlayAgain;

    private final int mapWidth;
    private final int mapHeight;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates the overlay for the given map dimensions.
     *
     * @param mapWidth  pixel width of the game map
     * @param mapHeight pixel height of the game map
     */
    public RoundEndOverlay(int mapWidth, int mapHeight) {
        this.mapWidth  = mapWidth;
        this.mapHeight = mapHeight;

        // Backdrop
        backdrop = new Rectangle(0, 0, mapWidth, mapHeight);
        backdrop.setFill(Color.rgb(0, 0, 0, 0.72));

        // "LEVEL COMPLETE!" title
        titleText = new Text("LEVEL COMPLETE!");
        titleText.setFont(Font.font("System", FontWeight.BOLD, TITLE_FONT_SIZE));
        titleText.setFill(TITLE_COLOR);
        titleText.setTextAlignment(TextAlignment.CENTER);

        // "Next Level" button
        double btnW = 180, btnH = 44;
        playAgainBg = new Rectangle(
            (mapWidth - btnW) / 2.0, mapHeight - 80, btnW, btnH);
        playAgainBg.setFill(Color.rgb(60, 140, 60));
        playAgainBg.setStroke(Color.rgb(30, 100, 30));
        playAgainBg.setStrokeWidth(2);
        playAgainBg.setArcWidth(12);
        playAgainBg.setArcHeight(12);

        playAgainText = new Text("Next Level");
        playAgainText.setFont(Font.font("System", FontWeight.BOLD, 20));
        playAgainText.setFill(Color.WHITE);
        double labelW = playAgainText.getText().length() * 20 * 0.55;
        playAgainText.setX((mapWidth - labelW) / 2.0);
        playAgainText.setY(mapHeight - 80 + 29);

        // Hover effect
        playAgainBg.setOnMouseEntered(e -> playAgainBg.setFill(Color.rgb(80, 170, 80)));
        playAgainBg.setOnMouseExited(e -> playAgainBg.setFill(Color.rgb(60, 140, 60)));
        playAgainBg.setOnMouseClicked(e -> { if (onPlayAgain != null) onPlayAgain.run(); });
        playAgainText.setOnMouseClicked(e -> { if (onPlayAgain != null) onPlayAgain.run(); });

        group.getChildren().addAll(backdrop, titleText, playAgainBg, playAgainText);
        group.setVisible(false);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the scene-graph group to be added to the scene root.
     *
     * @return the overlay group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Makes the overlay visible and populates the rankings.
     *
     * @param pigsRanked all pigs sorted heaviest-first (index 0 = winner)
     */
    public void show(List<Pig> pigsRanked) {
        // Remove old rank texts from scene graph
        for (Text t : rankTexts) {
            group.getChildren().remove(t);
        }
        rankTexts.clear();

        // Layout: vertically centred block
        // Total height = title + gap + (N lines * LINE_SPACING)
        int    n           = pigsRanked.size();
        double blockHeight = TITLE_FONT_SIZE + 20.0 + n * LINE_SPACING;
        double startY      = (mapHeight - blockHeight) / 2.0 + TITLE_FONT_SIZE;

        // Position title (centred horizontally)
        double titleWidth  = estimateTextWidth(titleText);
        titleText.setX((mapWidth - titleWidth) / 2.0);
        titleText.setY(startY);

        // Rank entries
        double entryY = startY + 20.0 + LINE_SPACING;
        for (int i = 0; i < n; i++) {
            Pig pig = pigsRanked.get(i);

            String medal = i < MEDAL_COLORS.length ? medalLabel(i) : (i + 1) + ".";
            String line  = medal + "  " + pig.getName()
                         + "  —  " + String.format("%.1f", pig.getWeight()) + " kg";

            Text t = new Text(line);
            t.setFont(Font.font("System", FontWeight.BOLD, RANK_FONT_SIZE));

            // Winner gets gold, others get their pig color with a minimum brightness
            if (i == 0) {
                t.setFill(WINNER_COLOR);
            } else if (i < MEDAL_COLORS.length) {
                t.setFill(MEDAL_COLORS[i]);
            } else {
                // Use the pig's colour but ensure it is readable on dark background
                t.setFill(brightenForDarkBg(pig.getColor()));
            }

            double textWidth = estimateTextWidth(t);
            t.setX((mapWidth - textWidth) / 2.0);
            t.setY(entryY);

            rankTexts.add(t);
            group.getChildren().add(t);
            entryY += LINE_SPACING;
        }

        group.setVisible(true);
    }

    /**
     * Registers a callback invoked when the player clicks "Play Again".
     *
     * @param action the restart callback
     */
    public void setOnPlayAgain(Runnable action) {
        this.onPlayAgain = action;
    }

    /**
     * Hides the overlay.
     */
    public void hide() {
        group.setVisible(false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns a medal label string for rank index 0, 1, 2. */
    private static String medalLabel(int index) {
        switch (index) {
            case 0: return "1st";
            case 1: return "2nd";
            case 2: return "3rd";
            default: return (index + 1) + ".";
        }
    }

    /**
     * Approximates the pixel width of a Text node.
     * JavaFX doesn't expose exact layout bounds until the node is in a live scene,
     * so we use a character-width heuristic that is good enough for centering.
     */
    private static double estimateTextWidth(Text t) {
        // Rough average: ~0.55 × font size per character
        double charWidth = t.getFont().getSize() * 0.55;
        return t.getText().length() * charWidth;
    }

    /**
     * Returns the colour if it is already bright enough, otherwise lightens it so
     * it remains legible on a dark background.
     */
    private static Color brightenForDarkBg(Color c) {
        double brightness = c.getBrightness();
        if (brightness >= 0.55) {
            return c;
        }
        return c.deriveColor(0, 1.0, 2.0 / brightness, 1.0);
    }
}
