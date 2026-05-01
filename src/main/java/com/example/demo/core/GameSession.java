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

    /** Snapshot of perks bought before this run started. Read-only during the run. */
    private final java.util.Map<Perk, Integer> perks = new java.util.EnumMap<>(Perk.class);
    /** Boons picked between levels. May contain duplicates (one per level chosen). */
    private final java.util.List<Boon> activeBoons = new java.util.ArrayList<>();
    /** Daily-seed flag — true if this is a fixed-seed daily run. */
    private boolean dailyRun;
    /** Seed used to drive map and item randomness. 0 = use system time. */
    private long runSeed;

    public GameSession() {
        for (Perk p : Perk.values()) perks.put(p, 0);
    }

    /** Snapshots perk levels from meta-progression at run start. */
    public void applyPerks(MetaProgression meta) {
        for (Perk p : Perk.values()) perks.put(p, meta.getPerkLevel(p));
    }

    public int getPerkLevel(Perk p) { return perks.getOrDefault(p, 0); }

    public java.util.List<Boon> getActiveBoons() { return activeBoons; }
    public void addBoon(Boon b) { activeBoons.add(b); }
    public boolean hasBoon(Boon b) { return activeBoons.contains(b); }

    public boolean isDailyRun() { return dailyRun; }
    public void setDailyRun(boolean daily) { this.dailyRun = daily; }
    public long getRunSeed() { return runSeed; }
    public void setRunSeed(long seed) { this.runSeed = seed; }

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

    /** Weight decay per tick. Increases with level. Reduced by SLOWER_DECAY perk and PACIFIST boon. */
    public double getWeightDecayRate() {
        double base = 0.008 + (level - 1) * 0.001;
        base -= 0.001 * getPerkLevel(Perk.SLOWER_DECAY);
        if (hasBoon(Boon.GLUTTON)) base = 0;
        return Math.max(0, base);
    }

    /** Number of ticks to skip decay at the start of each level (DECAY_GRACE perk). */
    public int getDecayGraceTicks() {
        return getPerkLevel(Perk.DECAY_GRACE) > 0 ? 300 : 0;
    }

    /** Bonus starting weight from EXTRA_WEIGHT perk. */
    public double getStartWeightBonus() {
        return 5.0 * getPerkLevel(Perk.EXTRA_WEIGHT);
    }

    /** Magnet pull range in cells (default 3, +1 per BIG_SNOUT level). */
    public int getMagnetRange() {
        return 3 + getPerkLevel(Perk.EXTRA_MAGNET_RANGE);
    }

    /** Whether the player should start each level with a shield (LUCKY_ACORN perk). */
    public boolean startsWithShield() {
        return getPerkLevel(Perk.START_WITH_SHIELD) > 0;
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
