package com.example.demo.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Watches for achievement-unlock conditions during a run and reports newly
 * unlocked achievements to a single callback (one toast per unlock).
 *
 * <p>Stateless wrt the run — owns nothing but the consumer; defers all
 * persistence to {@link MetaProgression}.
 */
public class AchievementTracker {

    private final MetaProgression meta;
    private Consumer<Achievement> onUnlock = a -> {};

    public AchievementTracker(MetaProgression meta) {
        this.meta = meta;
    }

    public void setOnUnlock(Consumer<Achievement> handler) {
        this.onUnlock = handler == null ? a -> {} : handler;
    }

    /** Try to unlock {@code a} — fires the toast and persists if it's new. */
    public void unlock(Achievement a) {
        if (meta.unlockAchievement(a)) {
            meta.save();
            onUnlock.accept(a);
        }
    }

    /** Convenience: unlock multiple at once, return the ones that were new. */
    public List<Achievement> unlockAll(Achievement... candidates) {
        List<Achievement> fresh = new ArrayList<>();
        for (Achievement a : candidates) {
            if (meta.unlockAchievement(a)) fresh.add(a);
        }
        if (!fresh.isEmpty()) {
            meta.save();
            for (Achievement a : fresh) onUnlock.accept(a);
        }
        return fresh;
    }

    // Convenience condition checkers — call from the game loop.

    public void checkLifetimeTruffles() {
        if (meta.getTruffleLifetime() >= 1)    unlock(Achievement.FIRST_TRUFFLE);
        if (meta.getTruffleLifetime() >= 100)  unlock(Achievement.HUNDRED_TRUFFLES);
        if (meta.getTruffleLifetime() >= 1000) unlock(Achievement.THOUSAND_TRUFFLES);
    }

    public void checkLevelReached(int level) {
        if (level >= 5)  unlock(Achievement.REACH_LEVEL_5);
        if (level >= 10) unlock(Achievement.REACH_LEVEL_10);
        if (level >= 15) unlock(Achievement.REACH_LEVEL_15);
    }

    public void checkPerkMaxed() {
        for (Perk p : Perk.values()) {
            if (meta.getPerkLevel(p) >= p.maxLevel) {
                unlock(Achievement.MAX_PERK);
                return;
            }
        }
    }
}
