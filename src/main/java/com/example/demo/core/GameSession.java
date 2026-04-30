package com.example.demo.core;

/**
 * Tracks persistent state across levels: current level, score, high score, and
 * whether the player is still alive. Also provides difficulty-scaling formulas.
 */
public class GameSession {

    private static int highScore = 0;

    /** Maximum streak multiplier applied to item-pickup scores. */
    private static final int MAX_STREAK_STEPS = 10; // 10 → 1 + 0.2*10 = 3.0×

    private int level = 1;
    private int score = 0;
    private boolean alive = true;
    private String deathReason = "";

    /** Number of consecutive seconds the player has picked up at least one item. */
    private int streakSeconds;
    /** True if at least one item was collected during the current second window. */
    private boolean streakItemThisSecond;
    /** Set true when the player takes a wolf/farmer hit this level. Reset on level start. */
    private boolean hitTakenThisLevel;

    public int getLevel() { return level; }
    public int getScore() { return score; }
    public static int getHighScore() { return highScore; }
    public boolean isAlive() { return alive; }
    public String getDeathReason() { return deathReason; }

    /** Current consecutive-second streak (0 = no streak active). */
    public int getStreakSeconds() { return streakSeconds; }

    /** Multiplier applied to item-pickup scores at the current streak. */
    public double getStreakMultiplier() {
        int steps = Math.min(streakSeconds, MAX_STREAK_STEPS);
        return 1.0 + 0.2 * steps;
    }

    /** Returns whether the player has been hit this level (used for Clean Round bonus). */
    public boolean wasHitThisLevel() { return hitTakenThisLevel; }

    /** Marks that the player took a hit (wolf/farmer contact) this level. */
    public void markHitThisLevel() { hitTakenThisLevel = true; }

    /** Records that an item was collected during the current second window. */
    public void noteItemCollected() {
        streakItemThisSecond = true;
    }

    /** Called once per second to advance the streak window. */
    public void tickStreakSecond() {
        if (streakItemThisSecond) {
            streakSeconds = Math.min(streakSeconds + 1, MAX_STREAK_STEPS);
        } else {
            streakSeconds = 0;
        }
        streakItemThisSecond = false;
    }

    public void addScore(int points) {
        score += points;
    }

    /** Adds points scaled by the current streak multiplier. */
    public void addScoreWithStreak(int basePoints) {
        score += (int) Math.round(basePoints * getStreakMultiplier());
    }

    public void nextLevel() {
        level++;
        hitTakenThisLevel = false;
        streakSeconds = 0;
        streakItemThisSecond = false;
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

    /** Round duration in ticks. Starts at 1 min, decreases 15s per level, floor 30s. */
    public int getLevelTimeTicks() {
        return Math.max(1800, 3_600 - (level - 1) * 900);
    }

    /** Weight decay per tick. Increases with level. */
    public double getWeightDecayRate() {
        return 0.008 + (level - 1) * 0.001;
    }

    /** AI base move interval in ticks. Starts slow on Level 1, gets faster each level. */
    public int getAiMoveInterval() {
        return Math.max(8, 20 - level * 2);
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
