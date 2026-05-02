package com.example.demo.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Persistent meta-progression state: banked truffles + permanent perk levels.
 * Stored as flat key=value lines in ~/.trufflerush/meta.txt.
 */
public class MetaProgression {

    private static final String DIR_NAME  = ".trufflerush";
    private static final String FILE_NAME = "meta.txt";

    private int truffleBank;
    private int truffleLifetime;
    private long bestDailyDate;
    private int bestDailyScore;
    private final Map<Perk, Integer> perkLevels = new EnumMap<>(Perk.class);
    private final Set<Achievement> achievements = EnumSet.noneOf(Achievement.class);

    public MetaProgression() {
        for (Perk p : Perk.values()) perkLevels.put(p, 0);
    }

    public int getTruffleBank() { return truffleBank; }
    public int getTruffleLifetime() { return truffleLifetime; }
    public int getPerkLevel(Perk p) { return perkLevels.getOrDefault(p, 0); }
    public long getBestDailyDate() { return bestDailyDate; }
    public int getBestDailyScore() { return bestDailyScore; }
    public Set<Achievement> getAchievements() { return achievements; }
    public boolean hasAchievement(Achievement a) { return achievements.contains(a); }

    /** Records a new achievement. Returns true if this was a fresh unlock. */
    public boolean unlockAchievement(Achievement a) {
        return achievements.add(a);
    }

    /** Adds banked truffles (called when a run ends). */
    public void addToBank(int amount) {
        if (amount > 0) {
            truffleBank += amount;
            truffleLifetime += amount;
        }
    }

    /** Records a daily-run score if it beats the prior one for the same date. */
    public void recordDailyScore(long epochDay, int score) {
        if (epochDay != bestDailyDate || score > bestDailyScore) {
            bestDailyDate = epochDay;
            bestDailyScore = score;
        }
    }

    /** Attempts to buy the next level of {@code perk}. Returns true on success. */
    public boolean tryBuy(Perk perk) {
        int current = getPerkLevel(perk);
        int cost = perk.costForLevel(current);
        if (cost < 0 || truffleBank < cost) return false;
        truffleBank -= cost;
        perkLevels.put(perk, current + 1);
        return true;
    }

    /** Resets all perks and refunds 70% of spent truffles. */
    public void respec() {
        int refund = 0;
        for (Perk p : Perk.values()) {
            int level = getPerkLevel(p);
            for (int l = 0; l < level; l++) refund += p.baseCost * (l + 1);
        }
        for (Perk p : Perk.values()) perkLevels.put(p, 0);
        truffleBank += (int) Math.round(refund * 0.7);
    }

    // --- Persistence ---------------------------------------------------------

    public static MetaProgression load() {
        MetaProgression m = new MetaProgression();
        Path file = filePath();
        if (!Files.exists(file)) return m;
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            Map<String, String> kv = new HashMap<>();
            while ((line = br.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                kv.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
            m.truffleBank     = parseInt(kv.get("truffleBank"), 0);
            m.truffleLifetime = parseInt(kv.get("truffleLifetime"), m.truffleBank);
            m.bestDailyDate   = parseLong(kv.get("bestDailyDate"), 0L);
            m.bestDailyScore  = parseInt(kv.get("bestDailyScore"), 0);
            for (Perk p : Perk.values()) {
                int lvl = parseInt(kv.get("perk." + p.name()), 0);
                m.perkLevels.put(p, Math.min(lvl, p.maxLevel));
            }
            String achStr = kv.get("achievements");
            if (achStr != null && !achStr.isEmpty()) {
                for (String name : achStr.split(",")) {
                    try { m.achievements.add(Achievement.valueOf(name.trim())); }
                    catch (IllegalArgumentException ignore) {}
                }
            }
        } catch (IOException ex) {
            // Corrupt file — ignore and start fresh.
        }
        return m;
    }

    public void save() {
        Path file = filePath();
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter bw = Files.newBufferedWriter(file)) {
                bw.write("truffleBank=" + truffleBank); bw.newLine();
                bw.write("truffleLifetime=" + truffleLifetime); bw.newLine();
                bw.write("bestDailyDate=" + bestDailyDate); bw.newLine();
                bw.write("bestDailyScore=" + bestDailyScore); bw.newLine();
                for (Perk p : Perk.values()) {
                    bw.write("perk." + p.name() + "=" + getPerkLevel(p));
                    bw.newLine();
                }
                StringBuilder achList = new StringBuilder();
                boolean first = true;
                for (Achievement a : achievements) {
                    if (!first) achList.append(',');
                    achList.append(a.name());
                    first = false;
                }
                bw.write("achievements=" + achList);
                bw.newLine();
            }
        } catch (IOException ex) {
            // Disk full / permission denied — nothing we can do; progress is lost.
        }
    }

    private static Path filePath() {
        String home = System.getProperty("user.home", ".");
        return Paths.get(home, DIR_NAME, FILE_NAME);
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException ex) { return def; }
    }

    private static long parseLong(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s); } catch (NumberFormatException ex) { return def; }
    }
}
