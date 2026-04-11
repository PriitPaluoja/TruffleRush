package com.example.demo.core;

/**
 * Tracks persistent state across levels: current level, score, high score, and
 * whether the player is still alive. Also provides difficulty-scaling formulas.
 */
public class GameSession {

    private static int highScore = 0;

    private int level = 1;
    private int score = 0;
    private boolean alive = true;
    private String deathReason = "";

    public int getLevel() { return level; }
    public int getScore() { return score; }
    public static int getHighScore() { return highScore; }
    public boolean isAlive() { return alive; }
    public String getDeathReason() { return deathReason; }

    public void addScore(int points) {
        score += points;
    }

    public void nextLevel() {
        level++;
    }

    public void endGame(String reason) {
        alive = false;
        deathReason = reason;
        if (score > highScore) {
            highScore = score;
        }
    }

    public void updateHighScore() {
        if (score > highScore) {
            highScore = score;
        }
    }

    /** Round duration in ticks. Decreases 15s per level, floor 1:30. */
    public int getLevelTimeTicks() {
        return Math.max(5400, 10_800 - (level - 1) * 900);
    }

    /** Weight decay per tick. Increases with level. */
    public double getWeightDecayRate() {
        return 0.008 + (level - 1) * 0.001;
    }

    /** AI base move interval in ticks. Gets faster each level. */
    public int getAiMoveInterval() {
        return Math.max(8, 15 - level);
    }

    /** Obstacle density multiplier. More obstacles at higher levels. */
    public double getObstacleDensityMultiplier() {
        return 1.0 + (level - 1) * 0.15;
    }

    /** Win-weight threshold. Same across levels. */
    public double getWinWeight() {
        return 150.0;
    }
}
